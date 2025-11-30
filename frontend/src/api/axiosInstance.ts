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
        handleLocally?: boolean;
    }
}

interface CustomAxiosRequestConfig extends InternalAxiosRequestConfig {
    handleLocally?: boolean;
}


//  Small “bridge” to connect Auth0 to axios
let accessTokenProvider:
    | (() => Promise<string | undefined>)
    | null = null;


export function registerAccessTokenProvider(
    provider: () => Promise<string | undefined>
) {
    accessTokenProvider = provider;
}


// If your backend routes already start with /api/...,
// we can leave the prefix empty to avoid /api/v1/api/...
const API_PREFIX = ""; // change to "/api/v1" if your gateway uses it

const createAxiosInstance = (): AxiosInstance => {
    // src/api/axiosInstance.ts
    const instance = axios.create({
        baseURL: import.meta.env.VITE_API_URL,
        headers: {
            "Content-Type": "application/json",
        },
    });


    //  REQUEST INTERCEPTOR  //
    instance.interceptors.request.use(
        async (config: CustomAxiosRequestConfig) => {

            if (config.url && !config.url.startsWith("http")) {
                config.url = API_PREFIX + config.url;
            }


            // ATTACH THE AUTH0 TOKEN IF YOU HAVE ONE
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

    // RESPONSE INTERCEPTOR //
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
