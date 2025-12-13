import { useNavigate } from "react-router-dom";
import { ClientTransactionList } from "@/features/transactions/components/ClientTransactionList";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";

export function ClientTransactionsPage() {
  const navigate = useNavigate();

  return (
    <div className="space-y-6">
      <ErrorBoundary>
        <ClientTransactionList onNavigate={(route) => navigate(route)} />
      </ErrorBoundary>
    </div>
  );
}
