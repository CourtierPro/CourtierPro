import { useNavigate } from "react-router-dom";
<<<<<<< HEAD
import { ClientTransactionList } from "@/features/transactions/components/ClientTransactionList";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";
=======
import { useTranslation } from "react-i18next";
import { FileText } from "lucide-react";
import { TransactionSummary } from "@/features/transactions/components/TransactionSummary";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { useClientTransactionsPageLogic } from "@/features/transactions/hooks/useClientTransactionsPageLogic";
import { Button } from "@/shared/components/ui/button";
import { ClientTransactionTimeline } from '@/features/transactions/components/ClientTransactionTimeline';
>>>>>>> 768abe9 (refactor: remove duplicate TimelineEntryDTO, clean imports, unify timeline audit logic)

export function ClientTransactionsPage() {
  const navigate = useNavigate();

  if (transactions.length === 1) {
    const transaction = transactions[0];
    return (
      <div className="space-y-6">
        <PageHeader title={t("myTransactions")} />
        <TransactionSummary transactionId={transaction.transactionId} />
        <ClientTransactionTimeline transactionId={transaction.transactionId} />
      </div>
    );
  }

  // Sinon, fallback multi-transactions (comportement précédent)
  return (
    <div className="space-y-6">
      <ErrorBoundary>
        <ClientTransactionList onNavigate={(route) => navigate(route)} />
      </ErrorBoundary>
    </div>
  );
}
