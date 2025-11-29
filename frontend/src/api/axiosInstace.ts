import axios, {
  AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import axiosErrorResponseHandler from "@/api/axiosErrorResponseHandler";

axios.defaults.withCredentials = false;

declare module "axios" {
  export interface AxiosRequestConfig {
    handleLocally?: boolean;
  }
}

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
  handleLocally?: boolean;
}

const API_PREFIX = "/api/v1";

// Use `VITE_BACKEND_URL` when provided (e.g. in .env.local). Fallback to dev backend.
const BASE_URL = import.meta.env.VITE_BACKEND_URL ?? import.meta.env.VITE_DEV_BACKEND;

const createAxiosInstance = (): AxiosInstance => {
  const instance = axios.create({
    baseURL: BASE_URL,
    headers: {
      "Content-Type": "application/json",
    },
  });

  instance.interceptors.request.use(
    async (config: CustomAxiosRequestConfig) => {
      if (config.url && !config.url.startsWith("http")) {
        config.url = API_PREFIX + config.url;
      }

      // 🔐 TODO (after FE Auth0 setup):
      // const token = await auth0Client.getTokenSilently();
      // if (token) config.headers.Authorization = `Bearer ${token}`;

      return config;
    },
    (error) => Promise.reject(error)
  );

  instance.interceptors.response.use(
    (response) => response,
    (error) => {
      const shouldStopPropagation = handleAxiosError(error);

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
      if (config?.handleLocally) return false;

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
