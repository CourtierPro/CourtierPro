import { useParams, useNavigate } from "react-router-dom";
import { useTranslation } from 'react-i18next';
import { TransactionSummary } from "@/features/transactions/components/TransactionSummary";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";

export function ClientTransactionDetailsPage() {
  const { transactionId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');

  if (!transactionId) {
    return (
      <ErrorState
        title={t('transactionNotFound')}
        message={t('noTransactionId')}
        action={
          <Button onClick={() => navigate('/dashboard/client')}>
            {t('goBack')}
          </Button>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <ErrorBoundary key={transactionId}>
        <TransactionSummary
          transactionId={transactionId}
        />
      </ErrorBoundary>
    </div>
  );
}
