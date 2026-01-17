/**
 * Global type declarations for runtime environment configuration.
 * This is the single source of truth for the Window.env interface,
 * allowing runtime config (window.env) to override build-time config (import.meta.env).
 */

declare global {
    interface Window {
        env: {
            VITE_API_URL: string;
            VITE_AUTH0_DOMAIN?: string;
            VITE_AUTH0_CLIENT_ID?: string;
            VITE_AUTH0_AUDIENCE?: string;
            VITE_AUTH0_CALLBACK_URL?: string;
            [key: string]: string | undefined;
        };
    }
}

export { };
