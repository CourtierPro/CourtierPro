import { useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { useNavigate } from 'react-router-dom';
import { getRoleFromUser } from '../roleUtils';
import { useCurrentUser } from '../api/useCurrentUser';

export function useLoginRedirect() {
    const { loginWithRedirect, isAuthenticated, isLoading: isAuth0Loading, user } = useAuth0();
    const navigate = useNavigate();
    const { data: currentUser, isLoading: isUserLoading, error: userError } = useCurrentUser();

    // Derive deactivation status instead of using useEffect/useState
    const isDeactivated = isAuthenticated && !isUserLoading && !!(userError || (currentUser && !currentUser.active));

    useEffect(() => {
        if (isAuthenticated && user && !isUserLoading) {
            // If deactivated, do not redirect
            if (isDeactivated) {
                return;
            }

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
    }, [isAuthenticated, user, navigate, currentUser, isUserLoading, userError, isDeactivated]);

    useEffect(() => {
        if (!isAuthenticated && !isAuth0Loading) {
            loginWithRedirect({
                appState: { returnTo: window.location.pathname },
            });
        }
    }, [isAuthenticated, isAuth0Loading, loginWithRedirect]);

    return { isLoading: isAuth0Loading || (isAuthenticated && isUserLoading), isDeactivated };
}
