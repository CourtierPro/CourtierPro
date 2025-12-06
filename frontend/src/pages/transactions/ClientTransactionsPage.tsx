/**
 * ClientTransactionsPage
 * 
 * Displays a list of transactions for the current client.
 * Uses `useClientTransactions` query to fetch data.
 * Implements branded UI components for consistent look and feel.
 */
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { FileText } from "lucide-react";
import { TransactionSummary } from "@/features/transactions/components/TransactionSummary";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { useClientTransactions } from "@/features/transactions/api/queries";
import { ErrorState } from "@/shared/components/branded/ErrorState";

export function ClientTransactionsPage() {
  const { t } = useTranslation("transactions");
  const navigate = useNavigate();

  // NOTE: replace with real client ID from Auth0 later
  const mockClientId = "CLIENT123";

  const { data: transactions, isLoading, error } = useClientTransactions(mockClientId);

  if (isLoading) {
    return <LoadingState />;
  }

  if (error) {
    return <ErrorState title={t("errorLoadingTransactions")} message={t("couldNotLoadTransactions")} />;
  }

  if (!transactions || transactions.length === 0) {
    return (
      <div className="space-y-6">
        <PageHeader title={t("myTransactions")} />
        <EmptyState
          icon={<FileText className="h-12 w-12 text-muted-foreground" />}
          title={t("noTransactionsFound")}
          description={t("noActiveTransactions")}
          action={
            <button
              onClick={() => navigate("/")}
              className="px-4 py-2 rounded-lg bg-primary text-primary-foreground hover:bg-primary/90"
            >
              {t("goHome")}
            </button>
          }
        />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <PageHeader title={t("myTransactions")} />
      <div className="grid gap-4">
        {transactions.map((transaction) => (
          <TransactionSummary key={transaction.transactionId} transactionId={transaction.transactionId} />
        ))}
      </div>
    </div>
  );
}
