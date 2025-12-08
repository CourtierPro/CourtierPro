import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { registerAccessTokenProvider } from "@/shared/api/axiosInstance";

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const authDisabled = import.meta.env.VITE_AUTH_DISABLED === "true";

    const { getAccessTokenSilently, isAuthenticated } = useAuth0();

    useEffect(() => {
        if (authDisabled) {
            // When auth is disabled, register a no-op provider so axios won't attach any token.
            registerAccessTokenProvider(async () => undefined);
            return;
        }

        registerAccessTokenProvider(async () => {
            if (!isAuthenticated) return undefined;
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: import.meta.env.VITE_AUTH0_AUDIENCE,
                },
            });
            return token;
        });
    }, [authDisabled, getAccessTokenSilently, isAuthenticated]);

    return <>{children}</>;
}
