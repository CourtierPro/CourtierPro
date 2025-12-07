import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { Users, ShieldCheck, Server } from "lucide-react";

export function AdminDashboardPage() {
  const { t } = useTranslation("dashboard");

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("admin.title")}
        subtitle={t("admin.subtitle")}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <KpiCard
          title={t("admin.totalUsers")}
          value="1,234"
          icon={<Users className="w-4 h-4" />}
          trend={{ value: 5, label: t("admin.thisWeek"), direction: "up" }}
        />
        <KpiCard
          title={t("admin.activeBrokers")}
          value="45"
          icon={<ShieldCheck className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.systemHealth")}
          value="99.9%"
          icon={<Server className="w-4 h-4" />}
          trend={{ value: 0, label: t("admin.stable"), direction: "neutral" }}
        />
      </div>

      <Section title={t("admin.systemLogs")} description={t("admin.systemLogsDesc")}>
        <div className="text-sm text-muted-foreground">{t("admin.logsPlaceholder")}</div>
      </Section>
    </div>
  );
}
