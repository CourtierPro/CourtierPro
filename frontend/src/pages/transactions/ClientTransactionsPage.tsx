import { useNavigate } from "react-router-dom";
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

export function ClientTransactionsPage() {
  const { t } = useTranslation("transactions");
  const navigate = useNavigate();
  const { transactions, isLoading, error } = useClientTransactionsPageLogic();

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
            <Button
              onClick={() => navigate("/")}
            >
              {t("goHome")}
            </Button>
          }
        />
      </div>
    );
  }

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
