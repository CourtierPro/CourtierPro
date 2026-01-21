import { type ReactNode } from "react";
import { BrowserRouter, useNavigate } from "react-router-dom";
import { Auth0Provider, type AppState } from "@auth0/auth0-react";
import { QueryClientProvider } from "@tanstack/react-query";
import { ReactQueryDevtools } from "@tanstack/react-query-devtools";
import { AuthProvider } from "@/app/providers/AuthProvider";
import { LanguageProvider } from "@/app/providers/LanguageProvider";
import { ThemeProvider } from "@/app/providers/ThemeProvider";
import { queryClient } from "@/shared/api/queryClient";
import { Toaster } from "sonner";
import { TooltipProvider } from "@/shared/components/ui/tooltip";
import { SearchProvider } from "@/features/search/context/SearchProvider";
// Window.env types are declared globally in @/shared/env.d.ts

// Helper to prefer Runtime Config (window.env) -> Build Config (import.meta.env) -> Fallback
const getEnv = (key: keyof Window['env'], fallback: string) => {
    if (window.env && window.env[key]) {
        return window.env[key];
    }
    return import.meta.env[key] || fallback;
};

// 3. Use the helper (Note: I removed the trailing slash from the fallback just in case!)
const domain = getEnv("VITE_AUTH0_DOMAIN", "dev-y7mhv7ttykx4kz4f.us.auth0.com");
const clientId = getEnv("VITE_AUTH0_CLIENT_ID", "JJIaYMFFtbZqGwP1XKyOx1XcFTPO9Qlr");
const audience = getEnv("VITE_AUTH0_AUDIENCE", "https://api.courtierpro.dev");

function Auth0ProviderWithNavigate({ children }: { children: ReactNode }) {
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
            cacheLocation="localstorage"
            useRefreshTokens={true}
            useRefreshTokensFallback={true}
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
                            <ThemeProvider defaultTheme="light" storageKey="courtier-pro-theme">
                                <TooltipProvider>
                                    <SearchProvider>
                                        {children}
                                    </SearchProvider>
                                    <Toaster />
                                </TooltipProvider>
                            </ThemeProvider>
                        </LanguageProvider>
                    </AuthProvider>
                </Auth0ProviderWithNavigate>
            </BrowserRouter>
            <ReactQueryDevtools initialIsOpen={false} />
        </QueryClientProvider>
    );
}
