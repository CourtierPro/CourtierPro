import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { registerAccessTokenProvider } from "@/shared/api/axiosInstance";

// Helper to prefer Runtime Config (window.env) -> Build Config (import.meta.env) -> Fallback
const getEnv = (key: keyof Window['env'], fallback: string) => {
    if (window.env && window.env[key]) {
        return window.env[key];
    }
    return import.meta.env[key] || fallback;
};

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const { getAccessTokenSilently, isAuthenticated, loginWithRedirect } = useAuth0();
    const audience = getEnv("VITE_AUTH0_AUDIENCE", "https://api.courtierpro.dev");

    useEffect(() => {
        registerAccessTokenProvider(async () => {
            if (!isAuthenticated) return undefined;
            try {
                const token = await getAccessTokenSilently({
                    authorizationParams: {
                        audience: audience,
                    },
                });
                return token;
            } catch (error: unknown) {
                console.error("Error getting access token", error);
                const err = error as { error?: string; message?: string };
                // Auth0 errors can have different structures - check all possible formats
                const errorString = String(error);
                const isConsentOrLoginRequired =
                    err?.error === "consent_required" ||
                    err?.error === "login_required" ||
                    err?.message?.includes("Consent required") ||
                    err?.message?.includes("consent_required") ||
                    err?.message?.includes("login_required") ||
                    errorString.includes("Consent required") ||
                    errorString.includes("consent_required");

                if (isConsentOrLoginRequired) {


                    await loginWithRedirect({
                        appState: { returnTo: window.location.pathname },
                        authorizationParams: {
                            audience: audience,
                        },
                    });

                }
                throw error;
            }
        });
    }, [getAccessTokenSilently, isAuthenticated, loginWithRedirect, audience]);

    return <>{children}</>;
}