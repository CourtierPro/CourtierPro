import { useTranslation } from 'react-i18next';
import { LoadingSpinner } from '@/shared/components/feedback/LoadingSpinner';
import { useLoginRedirect } from '@/features/auth/hooks/useLoginRedirect';

export function LoginPage() {
  const { t } = useTranslation('common');
  const { isLoading, isDeactivated } = useLoginRedirect();

  if (isDeactivated) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen p-4 text-center">
        <h1 className="text-2xl font-bold text-destructive mb-2">{t('auth.accountDeactivated')}</h1>
        <p className="text-muted-foreground">{t('auth.contactSupport')}</p>
        {/* Optional: Add a logout button to clear session */}
      </div>
    );
  }

  if (isLoading) {
    return <LoadingSpinner fullscreen message={t('auth.loggingIn')} />;
  }

  return <LoadingSpinner fullscreen message={t('auth.loggingIn')} />;
}
