import axios, { type AxiosInstance, type InternalAxiosRequestConfig } from "axios";
import axiosErrorResponseHandler from "@/api/axiosErrorResponseHandler";

declare module "axios" {
  export interface AxiosRequestConfig {
    /** When true the request error should be handled by the caller instead of global handler */
    handleLocally?: boolean;
  }
}

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  handleLocally?: boolean;
}

const API_PREFIX = "/api/v1";

// Use `VITE_BACKEND_URL` when provided (e.g. in .env.local). Fallback to dev backend.
const BASE_URL = (import.meta.env.VITE_BACKEND_URL ?? import.meta.env.VITE_DEV_BACKEND) || "";

const createAxiosInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: BASE_URL,
    withCredentials: true, // ensure cookies and credentials are sent (needed for Spring Boot allowCredentials)
    headers: {
      "Content-Type": "application/json",
    },
  });

  // Request interceptor: prefix non-absolute URLs and prepare Authorization header for future use
  instance.interceptors.request.use(
    async (config: CustomAxiosRequestConfig) => {
      // Prefix with API_PREFIX when URL is relative (not absolute).
      if (config.url && !/^(https?:)?\/\//i.test(config.url)) {
        const urlHasLeadingSlash = config.url.startsWith("/");
        const prefixed = API_PREFIX + (urlHasLeadingSlash ? config.url : `/${config.url}`);
        config.url = prefixed;
      }

      // Prepare Authorization header for future auth integration.
      // Current strategy:
      // 1. Try sessionStorage 'auth_token' (simple dev fallback)
      // 2. If an async token provider is registered on window as __getAuthToken, call it.
      // If neither provides a token, do nothing — header will not be sent.
      try {
        const existingAuth = (config.headers as any)?.Authorization;
        if (!existingAuth) {
          let token: string | null = null;

          token = sessionStorage.getItem("auth_token");

          if (!token && typeof window !== "undefined") {
            const provider = (window as any).__getAuthToken;
            if (typeof provider === "function") {
              // provider can be async
              // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
              token = await provider();
            }
          }

          if (token) {
            (config.headers as any) = {
              ...(config.headers as Record<string, unknown>),
              Authorization: `Bearer ${token}`,
            };
          }
        }
      } catch (e) {
        // Do not fail requests because token retrieval failed; just continue without Authorization
        // eslint-disable-next-line no-console
        console.warn("Failed to prepare Authorization header:", e);
      }

      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor: centralized handling for common status codes
  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      if (axios.isAxiosError(error)) {
        const status = error.response?.status ?? 0;
        const config = error.config as CustomAxiosRequestConfig;

        const globallyHandled = [401, 403, 500];

        if (globallyHandled.includes(status)) {
          // allow callers to opt-out of global handling
          if (!config?.handleLocally) {
            axiosErrorResponseHandler(error, status);
          }
        }
      }

      return Promise.reject(error);
    }
  );

  return instance;
};

const axiosInstance = createAxiosInstance();

export { axiosInstance };
export default axiosInstance;
