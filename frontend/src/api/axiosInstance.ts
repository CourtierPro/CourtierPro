// src/api/axiosInstance.ts

import axios, {
    type AxiosInstance,
    type InternalAxiosRequestConfig,
    AxiosError,
} from "axios";
import axiosErrorResponseHandler from "@/api/axiosErrorResponseHandler";

axios.defaults.withCredentials = false;

declare module "axios" {
    export interface AxiosRequestConfig {
        /** When true the request error should be handled by the caller instead of global handler */
        handleLocally?: boolean;
    }
}

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
    handleLocally?: boolean;
}

// Small “bridge” to connect Auth0 to axios
let accessTokenProvider:
    | (() => Promise<string | undefined>)
    | null = null;

export function registerAccessTokenProvider(
    provider: () => Promise<string | undefined>
) {
    accessTokenProvider = provider;
}

// If your backend routes already start with /api/v1/...,
// set VITE_API_URL to "http://localhost:8080/api/v1" and keep prefix empty.
const API_PREFIX = ""; // change to "/api/v1" if VITE_API_URL is just "http://localhost:8080"

const createAxiosInstance = (): AxiosInstance => {
    const instance = axios.create({
        baseURL: import.meta.env.VITE_API_URL,
        headers: {
            "Content-Type": "application/json",
        },
    });

    // REQUEST INTERCEPTOR
    instance.interceptors.request.use(
        async (config: CustomAxiosRequestConfig) => {
            // prefix relative URLs
            if (config.url && !config.url.startsWith("http")) {
                config.url = API_PREFIX + config.url;
            }

            // Attach the Auth0 token if we have one
            if (accessTokenProvider) {
                try {
                    const token = await accessTokenProvider();

                    if (token) {
                        config.headers = config.headers ?? {};
                        (config.headers as any).Authorization = `Bearer ${token}`;
                    }
                } catch (e) {
                    console.error("Error while getting Auth0 access token:", e);
                }
            }

            return config;
        },
        (error) => Promise.reject(error)
    );

    // RESPONSE INTERCEPTOR
    instance.interceptors.response.use(
        (response) => response,
        (error) => {
            handleAxiosError(error);
            return Promise.reject(error);
        }
    );

    return instance;
};

const handleAxiosError = (error: unknown): void => {
    if (axios.isAxiosError(error)) {
        const axiosErr = error as AxiosError;
        const statusCode = axiosErr.response?.status ?? 0;
        const config = axiosErr.config as CustomAxiosRequestConfig;

        const globallyHandledCodes = [401, 403, 500, 503];

        if (globallyHandledCodes.includes(statusCode) && !config?.handleLocally) {
            axiosErrorResponseHandler(axiosErr, statusCode);
        }
    } else {
        console.error("Unexpected non-Axios error:", error);
    }
};

const axiosInstance = createAxiosInstance();

export default axiosInstance;
export { axiosInstance };
