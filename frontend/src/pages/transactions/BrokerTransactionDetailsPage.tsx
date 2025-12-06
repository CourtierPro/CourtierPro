import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { TransactionDetail } from '@/features/transactions/components/TransactionDetail';
import { ErrorBoundary } from '@/shared/components/error/ErrorBoundary';

export function BrokerTransactionDetailsPage() {
  const { transactionId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');

  if (!transactionId) {
    return (
      <div className="p-6">
        <h1 className="text-xl font-semibold">{t('transactionNotFound')}</h1>
        <p className="text-sm text-muted-foreground">{t('noTransactionId')}</p>
        <button
          onClick={() => navigate('/transactions')}
          className="mt-4 px-4 py-2 rounded-lg"
          style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
        >
          {t('goBack')}
        </button>
      </div>
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
