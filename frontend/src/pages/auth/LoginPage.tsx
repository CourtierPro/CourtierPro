import { useEffect } from 'react';
import { useAuth0 } from '@auth0/auth0-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { LoadingSpinner } from '@/components/feedback/LoadingSpinner';
import { getRoleFromUser } from '@/auth/roleUtils';

export function LoginPage() {
  const { loginWithRedirect, isAuthenticated, isLoading, user } = useAuth0();
  const { t } = useTranslation('common');
  const navigate = useNavigate();

  // Redirect authenticated users to their dashboard
  useEffect(() => {
    if (isAuthenticated && user) {
      const role = getRoleFromUser(user);
      if (role) {
        const dashboards = {
          broker: '/dashboard/broker',
          client: '/dashboard/client',
          admin: '/dashboard/admin',
        };
        navigate(dashboards[role], { replace: true });
      }
    }
  }, [isAuthenticated, user, navigate]);

  // Automatically redirect to Auth0 on page load
  useEffect(() => {
    if (!isAuthenticated && !isLoading) {
      loginWithRedirect({
        appState: { returnTo: window.location.pathname },
      });
    }
  }, [isAuthenticated, isLoading, loginWithRedirect]);

  if (isLoading) {
    return <LoadingSpinner fullscreen message={t('auth.loggingIn')} />;
  }

  // Show nothing while redirecting to Auth0
  return <LoadingSpinner fullscreen message={t('auth.loggingIn')} />;
}
