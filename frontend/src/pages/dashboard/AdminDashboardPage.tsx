import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Users, ShieldCheck, Server } from "lucide-react";
import { useAdminDashboardStats } from "@/features/dashboard/hooks/useDashboardStats";

export function AdminDashboardPage() {
  const { t } = useTranslation("dashboard");
  const { data: stats, isLoading } = useAdminDashboardStats();

  if (isLoading) {
    return <LoadingState message={t("loading")} />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("admin.title")}
        subtitle={t("admin.subtitle")}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <KpiCard
          title={t("admin.totalUsers")}
          value={stats?.totalUsers.toString() || "0"}
          icon={<Users className="w-4 h-4" />}
          trend={{ value: 5, label: t("admin.thisWeek"), direction: "up" }}
        />
        <KpiCard
          title={t("admin.activeBrokers")}
          value={stats?.activeBrokers.toString() || "0"}
          icon={<ShieldCheck className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.systemHealth")}
          value={stats?.systemHealth || "Unknown"}
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
