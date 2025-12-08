import { type ReactNode } from "react";
import { BrowserRouter, useNavigate } from "react-router-dom";
import { Auth0Provider, type AppState } from "@auth0/auth0-react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { AuthProvider } from "@/app/providers/AuthProvider";
import { LanguageProvider } from "@/app/providers/LanguageProvider";
import { queryClient } from "@/shared/api/queryClient";

// TODO: Move these to environment variables or config file
const domain = "dev-y7mhv7ttykx4kz4f.us.auth0.com";
const clientId = "JJIaYMFFtbZqGwP1XKyOx1XcFTPO9Qlr";
const audience = "https://api.courtierpro.dev";

function Auth0ProviderWithNavigate({ children }: { children: ReactNode }) {
    if (import.meta.env.VITE_AUTH_DISABLED === "true") {
        return <>{children}</>;
    }
    const navigate = useNavigate();

    const onRedirectCallback = (appState?: AppState) => {
        navigate(appState?.returnTo || window.location.pathname);
    };

    return (
        <Auth0Provider
            domain={domain}
            clientId={clientId}
            authorizationParams={{
                audience,
                redirect_uri: window.location.origin,
            }}
            onRedirectCallback={onRedirectCallback}
        >
            {children}
        </Auth0Provider>
    );
}

export function AppProviders({ children }: { children: ReactNode }) {
    return (
        <QueryClientProvider client={queryClient}>
            <BrowserRouter>
                <Auth0ProviderWithNavigate>
                    <AuthProvider>
                        <LanguageProvider>
                            {children}
                        </LanguageProvider>
                    </AuthProvider>
                </Auth0ProviderWithNavigate>
            </BrowserRouter>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}
