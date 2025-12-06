import { useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { useNavigate } from 'react-router-dom';
import { getRoleFromUser } from '../roleUtils';

export function useLoginRedirect() {
    const { loginWithRedirect, isAuthenticated, isLoading, user } = useAuth0();
    const navigate = useNavigate();

    useEffect(() => {
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
    }, [isAuthenticated, user, navigate]);

    useEffect(() => {
        if (!isAuthenticated && !isLoading) {
            loginWithRedirect({
                appState: { returnTo: window.location.pathname },
            });
        }
    }, [isAuthenticated, isLoading, loginWithRedirect]);

    return { isLoading };
}
