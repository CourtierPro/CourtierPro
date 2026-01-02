import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Home, FileCheck } from "lucide-react";
import { useClientDashboardStats } from "@/features/dashboard/hooks/useDashboardStats";
import { useAuth0 } from "@auth0/auth0-react";

export function ClientDashboardPage() {
  const { t } = useTranslation("dashboard");
  const { user } = useAuth0();
  const clientId = user?.sub || "";
  const {
    transactions,
    setSelectedTransactionId,
    selectedTransactionId,
    kpis,
    isLoading,
  } = useClientDashboardStats(clientId);

  if (isLoading) {
    return <LoadingState message={t("loading")} />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("client.title")}
        subtitle={t("client.subtitle")}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <KpiCard
          title={t("client.activeTransactions")}
          value={kpis.global.activeTransactions.toString()}
          icon={<Home className="w-4 h-4" />}
        />
        <KpiCard
          title={t("client.documentsNeeded")}
          value={kpis.selected.documentsNeeded.toString()}
          icon={<FileCheck className="w-4 h-4" />}
        />
        <KpiCard
          title={t("client.documentsSubmitted")}
          value={kpis.selected.documentsSubmitted.toString()}
          icon={<FileCheck className="w-4 h-4" />}
        />
      </div>

      <Section title={t("client.myTransactions")} description={t("client.myTransactionsDesc")}>
        {transactions.length === 0 && (
          <div className="text-sm text-muted-foreground">{t("client.transactionListPlaceholder")}</div>
        )}
        <div className="flex flex-wrap gap-2 mt-2">
          {transactions.map((transaction) => (
            <button
              key={transaction.transactionId}
              className={`px-4 py-2 rounded border ${selectedTransactionId === transaction.transactionId ? 'bg-primary text-white' : 'bg-background text-foreground'}`}
              onClick={() => setSelectedTransactionId(transaction.transactionId)}
            >
              {transaction.propertyAddress?.street || t('client.unknownAddress')}
            </button>
          ))}
        </div>
      </Section>
    </div>
  );
}
