import { useTranslation } from 'react-i18next';
import { LoadingSpinner } from '@/shared/components/feedback/LoadingSpinner';
import { useLoginRedirect } from '@/features/auth/hooks/useLoginRedirect';

export function LoginPage() {
  const { t } = useTranslation('common');
  const { isLoading } = useLoginRedirect();

  if (isLoading) {
    return <LoadingSpinner fullscreen message={t('auth.loggingIn')} />;
  }

  // Show nothing while redirecting to Auth0
  return <LoadingSpinner fullscreen message={t('auth.loggingIn')} />;
}
