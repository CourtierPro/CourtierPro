import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { registerAccessTokenProvider } from "@/shared/api/axiosInstance";

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const { getAccessTokenSilently, isAuthenticated } = useAuth0();

    useEffect(() => {
        registerAccessTokenProvider(async () => {
            if (!isAuthenticated) return undefined;
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: window.env?.VITE_AUTH0_AUDIENCE || import.meta.env.VITE_AUTH0_AUDIENCE || "https://api.courtierpro.dev",
                },
            });
            return token;
        });
    }, [getAccessTokenSilently, isAuthenticated]);

    return <>{children}</>;
}
