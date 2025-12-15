
import { ClientTransactionTimeline } from '@/features/transactions/components/ClientTransactionTimeline';
import { TransactionDetail } from '@/features/transactions/components/TransactionDetail';

export function ClientTransactionDetailsPage() {
  return (
    <TransactionDetail isReadOnly={true} TimelineComponent={ClientTransactionTimeline} />
  );
}