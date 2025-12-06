import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { DollarSign, FileText, Users, Activity } from "lucide-react";

export function BrokerDashboardPage() {
  const { t } = useTranslation("dashboard");

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("broker.title")}
        subtitle={t("broker.subtitle")}
      />

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <KpiCard
          title={t("broker.activeTransactions")}
          value="12"
          icon={<FileText className="w-4 h-4" />}
          trend={{ value: 10, label: t("broker.vsLastMonth"), direction: "up" }}
        />
        <KpiCard
          title={t("broker.totalCommission")}
          value="$45,231"
          icon={<DollarSign className="w-4 h-4" />}
          trend={{ value: 12, label: t("broker.vsLastMonth"), direction: "up" }}
        />
        <KpiCard
          title={t("broker.activeClients")}
          value="8"
          icon={<Users className="w-4 h-4" />}
        />
        <KpiCard
          title={t("broker.pendingActions")}
          value="5"
          icon={<Activity className="w-4 h-4" />}
          trend={{ value: 2, label: t("broker.newToday"), direction: "neutral" }}
        />
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Section title={t("broker.recentActivity")} description={t("broker.recentActivityDesc")}>
          <div className="text-sm text-muted-foreground">{t("broker.activityPlaceholder")}</div>
        </Section>
        <Section title={t("broker.upcomingAppointments")} description={t("broker.upcomingAppointmentsDesc")}>
          <div className="text-sm text-muted-foreground">{t("broker.calendarPlaceholder")}</div>
        </Section>
      </div>
    </div>
  );
}
