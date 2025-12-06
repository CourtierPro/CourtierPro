import { useNavigate } from 'react-router-dom';
import { TransactionCreateForm } from '@/features/transactions/components/TransactionCreateForm';
import { ErrorBoundary } from '@/shared/components/error/ErrorBoundary';

export default function CreateTransactionPage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <ErrorBoundary>
        <TransactionCreateForm
          onNavigate={(route) => navigate(route)}
        />
      </ErrorBoundary>
    </div>
  );
}
