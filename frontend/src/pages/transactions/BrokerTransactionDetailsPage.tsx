import { useParams, useNavigate } from 'react-router-dom';
import { TransactionDetail } from '@/components/TransactionDetail';

export function BrokerTransactionDetailsPage() {
  const { transactionId } = useParams();
  const navigate = useNavigate();

  if (!transactionId) {
    return (
      <div className="p-6">
        <h1 className="text-xl font-semibold">Transaction Not Found</h1>
        <p className="text-sm text-muted-foreground">No transaction ID provided.</p>
        <button
          onClick={() => navigate('/transactions')}
          className="mt-4 px-4 py-2 rounded-lg"
          style={{ backgroundColor: '#FF6B01', color: '#FFFFFF' }}
        >
          Go Back
        </button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <TransactionDetail
        transactionId={transactionId}
        language="en"
        onNavigate={(route) => navigate(route)}
      />
    </div>
  );
}
