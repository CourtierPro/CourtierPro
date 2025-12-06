import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { Home, FileCheck } from "lucide-react";

export function ClientDashboardPage() {
  const { t } = useTranslation("dashboard");

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("client.title")}
        subtitle={t("client.subtitle")}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <KpiCard
          title={t("client.activeTransactions")}
          value="1"
          icon={<Home className="w-4 h-4" />}
        />
        <KpiCard
          title={t("client.documentsNeeded")}
          value="2"
          icon={<FileCheck className="w-4 h-4" />}
          trend={{ value: 1, label: t("client.overdue"), direction: "down" }}
        />
      </div>

      <Section title={t("client.myTransactions")} description={t("client.myTransactionsDesc")}>
        <div className="text-sm text-muted-foreground">{t("client.transactionListPlaceholder")}</div>
      </Section>
    </div>
  );
}
