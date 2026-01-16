import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Users, ShieldCheck, Activity, AlertTriangle, Database } from "lucide-react";
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


      {/* High-level system metrics */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <KpiCard
          title={t("admin.totalUsers", "Total Users")}
          value={stats?.totalUsers?.toString() || "0"}
          icon={<Users className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.activeBrokers", "Active Brokers")}
          value={stats?.activeBrokers?.toString() || "0"}
          icon={<ShieldCheck className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.systemHealth", "System Health")}
          value={stats?.systemHealth || "Unknown"}
          icon={<Activity className="w-4 h-4" />}
        />
      </div>

      {/* Quick access admin tools */}
      <div className="flex flex-wrap gap-4 mt-2">
        <button
          className="flex items-center gap-2 px-4 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded shadow"
          onClick={() => window.location.assign("/admin/resources")}
        >
          <Database className="w-4 h-4" />
          {t("admin.resourceRemoval", "Resource Removal")}
        </button>
        <button
          className="flex items-center gap-2 px-4 py-2 bg-orange-50 hover:bg-orange-100 text-orange-700 rounded shadow"
          onClick={() => window.location.assign("/admin/login-audit")}
        >
          <ShieldCheck className="w-4 h-4" />
          {t("admin.auditLogs", "Audit Logs")}
        </button>
      </div>

      {/* Recent admin actions */}
      <Section title={t("admin.recentActions", "Recent Admin Actions")} description={t("admin.recentActionsDesc", "Latest actions performed by admins.")}> 
        <div className="text-sm text-muted-foreground">{t("admin.recentActionsPlaceholder", "No recent actions to display.")}</div>
      </Section>

      {/* System alerts */}
      <Section title={t("admin.systemAlerts", "System Alerts")} description={t("admin.systemAlertsDesc", "Critical system alerts and warnings.")}> 
        <div className="flex items-center gap-2 text-red-600">
          <AlertTriangle className="w-5 h-5" />
          <span>{t("admin.systemAlertsPlaceholder", "No critical alerts.")}</span>
        </div>
      </Section>

      {/* System logs (read-only, monitoring) */}
      <Section title={t("admin.systemLogs")} description={t("admin.systemLogsDesc")}> 
        <div className="text-sm text-muted-foreground">{t("admin.logsPlaceholder")}</div>
      </Section>
    </div>
  );
}
