import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { TransactionDetail } from '@/features/transactions/components/TransactionDetail';
import { ErrorBoundary } from '@/shared/components/error/ErrorBoundary';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { Button } from '@/shared/components/ui/button';

export function BrokerTransactionDetailsPage() {
  const { transactionId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');

  if (!transactionId) {
    return (
      <ErrorState
        title={t('transactionNotFound')}
        message={t('noTransactionId')}
        action={
          <Button onClick={() => navigate('/transactions')}>
            {t('goBack')}
          </Button>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <ErrorBoundary key={transactionId}>
        <TransactionDetail
          transactionId={transactionId}
        />
      </ErrorBoundary>
    </div>
  );
}
