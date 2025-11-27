import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import axiosErrorResponseHandler from "@/api/axiosErrorResponseHandler";

// We’ll use bearer tokens later, so no cookies by default
axios.defaults.withCredentials = false;

declare module "axios" {
  export interface AxiosRequestConfig {
    handleLocally?: boolean; // if true, skip global redirects/handling
  }
}

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  handleLocally?: boolean;
}

const API_PREFIX = "/api/v1";

const createAxiosInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_URL,
    headers: {
      "Content-Type": "application/json",
    },
  });

  // Request interceptor
  instance.interceptors.request.use(
    async (config: CustomAxiosRequestConfig) => {
      // Prefix non-absolute URLs
      if (config.url && !config.url.startsWith("http")) {
        config.url = API_PREFIX + config.url;
      }

      // 🔐 TODO: attach Auth0 access token here
      // const token = await auth0Client.getTokenSilently();
      // if (token) {
      //   config.headers = config.headers ?? {};
      //   config.headers.Authorization = `Bearer ${token}`;
      // }

      return config;
    },
    (error) => Promise.reject(error)
  );

  // Response interceptor
  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      const shouldStopPropagation = handleAxiosError(error);

      // We still reject so callers can catch, but global handling
      // (redirect/log) may already have happened.
      if (shouldStopPropagation) {
        return Promise.reject(error);
      }

      return Promise.reject(error);
    }
  );

  return instance;
};

const handleAxiosError = (error: unknown): boolean => {
  if (axios.isAxiosError(error)) {
    const statusCode = error.response?.status ?? 0;
    const config = error.config as CustomAxiosRequestConfig;

    const globallyHandledCodes = [401, 403, 500, 503];

    if (globallyHandledCodes.includes(statusCode)) {
      // Let specific request opt out
      if (config?.handleLocally) {
        return false;
      }

      axiosErrorResponseHandler(error as AxiosError, statusCode);
      return true;
    }

    return false;
  }

  console.error("Unexpected non-Axios error occurred:", error);
  return false;
};

const axiosInstance = createAxiosInstance();

export default axiosInstance;
