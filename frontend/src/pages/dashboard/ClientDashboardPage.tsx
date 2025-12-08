import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Home, FileCheck } from "lucide-react";
import { useClientDashboardStats } from "@/features/dashboard/hooks/useDashboardStats";

export function ClientDashboardPage() {
  const { t } = useTranslation("dashboard");
  const { data: stats, isLoading } = useClientDashboardStats();

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
          value={stats?.activeTransactions.toString() || "0"}
          icon={<Home className="w-4 h-4" />}
        />
        <KpiCard
          title={t("client.documentsNeeded")}
          value={stats?.documentsNeeded.toString() || "0"}
          icon={<FileCheck className="w-4 h-4" />}
          trend={stats?.documentsNeeded ? { value: stats.documentsNeeded, label: t("client.overdue"), direction: "down" } : undefined}
        />
      </div>

      <Section title={t("client.myTransactions")} description={t("client.myTransactionsDesc")}>
        <div className="text-sm text-muted-foreground">{t("client.transactionListPlaceholder")}</div>
      </Section>
    </div>
  );
}
