import { useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { useNavigate } from 'react-router-dom';
import { getRoleFromUser } from '../roleUtils';

export function useLoginRedirect() {
    const authDisabled = import.meta.env.VITE_AUTH_DISABLED === "true";

    const { loginWithRedirect, isAuthenticated: rawIsAuthenticated, isLoading: rawIsLoading, user } = useAuth0();
    const navigate = useNavigate();

    const isLoading = authDisabled ? false : rawIsLoading;
    const isAuthenticated = authDisabled ? true : rawIsAuthenticated;

    useEffect(() => {
        if (authDisabled) return;

        if (isAuthenticated && user) {
            const role = getRoleFromUser(user);
            if (role) {
                const dashboards = {
                    broker: '/dashboard/broker',
                    client: '/dashboard/client',
                    admin: '/dashboard/admin',
                };
                navigate(dashboards[role as keyof typeof dashboards], { replace: true });
            }
        }
    }, [authDisabled, isAuthenticated, user, navigate]);

    useEffect(() => {
        if (authDisabled) return;

        if (!isAuthenticated && !isLoading) {
            void loginWithRedirect({
                appState: { returnTo: window.location.pathname },
            });
        }
    }, [authDisabled, isAuthenticated, isLoading, loginWithRedirect]);

    return { isLoading };
}
