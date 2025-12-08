import { useEffect } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { registerAccessTokenProvider } from "@/shared/api/axiosInstance";

export function AuthProvider({ children }: { children: React.ReactNode }) {
    if (import.meta.env.VITE_AUTH_DISABLED === "true") {
        return <>{children}</>;
    }

    const { getAccessTokenSilently, isAuthenticated } = useAuth0();

    useEffect(() => {
        registerAccessTokenProvider(async () => {
            if (!isAuthenticated) return undefined;
            const token = await getAccessTokenSilently({
                authorizationParams: {
                    audience: import.meta.env.VITE_AUTH0_AUDIENCE,
                },
            });
            return token;
        });
    }, [getAccessTokenSilently, isAuthenticated]);

    return <>{children}</>;
}
