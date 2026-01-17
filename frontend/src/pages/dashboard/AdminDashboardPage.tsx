import { useTranslation } from "react-i18next";
import { useState } from "react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Users, ShieldCheck, Activity, AlertTriangle, Database } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { useNavigate } from "react-router-dom";
import { useAdminDashboardStats, useAdminRecentActions } from "@/features/dashboard/hooks/useDashboardStats";
import { BroadcastMessageModal } from "@/features/notifications/components/BroadcastMessageModal";
import { InviteUserModal } from "@/features/admin/components/InviteUserModal";
import { useSystemLogs } from "@/features/dashboard/hooks/useSystemLogs";
import { useSystemAlerts } from "@/features/dashboard/hooks/useSystemAlerts";
import axiosInstance from "@/shared/api/axiosInstance";

export function AdminDashboardPage() {
  const { t } = useTranslation("dashboard");
  const { data: stats, isLoading } = useAdminDashboardStats();
  const { data: recentActions, isLoading: isRecentLoading } = useAdminRecentActions();
  const navigate = useNavigate();
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [showBroadcastModal, setShowBroadcastModal] = useState(false);
  // Show the 5 most recent logs on the dashboard
  const { data: systemLogs, isLoading: isLogsLoading, error: logsError } = useSystemLogs(5);
  const { data: systemAlerts, isLoading: isAlertsLoading, error: alertsError, refetch: refetchAlerts } = useSystemAlerts();
  const [isSendingAlert, setIsSendingAlert] = useState(false);
  const sendTestAlert = async () => {
    setIsSendingAlert(true);
    try {
      await axiosInstance.post("/api/v1/dashboard/admin/alerts", {
        message: "Test alert from dashboard button!",
        severity: "CRITICAL",
      });
      refetchAlerts();
    } catch {
      // Optionally show error
    } finally {
      setIsSendingAlert(false);
    }
  };

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
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
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
          title={t("admin.clientCount", "Total Clients")}
          value={stats?.clientCount?.toString() || "0"}
          icon={<Users className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.activeTransactions", "Active Transactions")}
          value={stats?.activeTransactions?.toString() || "0"}
          icon={<Activity className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.newUsers", "New Users (24h)")}
          value={stats?.newUsers?.toString() || "0"}
          icon={<Users className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.failedLogins", "Failed Logins (24h)")}
          value={stats?.failedLogins?.toString() || "0"}
          icon={<AlertTriangle className="w-4 h-4" />}
        />
        <KpiCard
          title={t("admin.systemHealth", "System Health")}
          value={stats?.systemHealth || "Unknown"}
          icon={<Activity className="w-4 h-4" />}
        />
      </div>
      {/* Quick access admin tools */}
      <div className="flex flex-wrap gap-4 mt-2">
        <Button
          type="button"
          aria-label="Navigate to Resource Removal page"
          className="flex items-center gap-2 px-4 py-2 bg-blue-50 hover:bg-blue-100 text-blue-700 rounded shadow"
          onClick={() => navigate("/admin/resources")}
        >
          <Database className="w-4 h-4" />
          {t("admin.resourceRemoval", "Resource Removal")}
        </Button>
        <Button
          type="button"
          aria-label="Navigate to Audit Logs page"
          className="flex items-center gap-2 px-4 py-2 bg-orange-50 hover:bg-orange-100 text-orange-700 rounded shadow"
          onClick={() => navigate("/admin/login-audit")}
        >
          <ShieldCheck className="w-4 h-4" />
          {t("admin.auditLogs", "Audit Logs")}
        </Button>
        <Button
          type="button"
          aria-label="Invite User"
          className="flex items-center gap-2 px-4 py-2 bg-green-50 hover:bg-green-100 text-green-700 rounded shadow"
          onClick={() => setShowInviteModal(true)}
        >
          {t("admin.inviteUser", "Invite User")}
        </Button>
          {/* Invite User Modal */}
          <InviteUserModal open={showInviteModal} onClose={() => setShowInviteModal(false)} />
        <Button
          type="button"
          aria-label="Create Broadcast"
          className="flex items-center gap-2 px-4 py-2 bg-purple-50 hover:bg-purple-100 text-purple-700 rounded shadow"
          onClick={() => setShowBroadcastModal(true)}
        >
          {t("admin.createBroadcast", "Create Broadcast")}
        </Button>
        {/* Broadcast Message Modal */}
        <BroadcastMessageModal open={showBroadcastModal} onOpenChange={setShowBroadcastModal} />
      </div>
      {/* Recent admin actions */}
      <Section title={t("admin.recentActions", "Recent Admin Actions")} description={t("admin.recentActionsDesc", "Latest actions performed by admins.")}>
        {isRecentLoading ? (
          <LoadingState message={t("loading")} />
        ) : (
          <div className="space-y-2">
            <div>
              <h4 className="font-semibold">Login Audits</h4>
              {recentActions?.recentLogins?.length ? (
                <ul className="text-xs text-muted-foreground">
                  {recentActions.recentLogins.map((log, idx) => {
                    if (
                      typeof log === 'object' && log !== null &&
                      'email' in log && 'role' in log && 'timestamp' in log
                    ) {
                      const l = log as { id?: string | number; email: string; role: string; timestamp: string | number };
                      return (
                        <li key={l.id || idx}>
                          {l.email} ({l.role}) at {new Date(l.timestamp).toLocaleString()}
                        </li>
                      );
                    }
                    return null;
                  })}
                </ul>
              ) : (
                <div className="text-xs text-muted-foreground">No recent login audits.</div>
              )}
            </div>
            <div>
              <h4 className="font-semibold">Deletion Audits</h4>
              {recentActions?.recentDeletions?.length ? (
                <ul className="text-xs text-muted-foreground">
                  {recentActions.recentDeletions.map((del, idx) => {
                    if (
                      typeof del === 'object' && del !== null &&
                      'adminEmail' in del && 'resourceType' in del && 'resourceId' in del && 'timestamp' in del
                    ) {
                      const d = del as { id?: string | number; adminEmail: string; resourceType: string; resourceId: string | number; timestamp: string | number };
                      return (
                        <li key={d.id || idx}>
                          {d.adminEmail} deleted {d.resourceType} ({d.resourceId}) at {new Date(d.timestamp).toLocaleString()}
                        </li>
                      );
                    }
                    return null;
                  })}
                </ul>
              ) : (
                <div className="text-xs text-muted-foreground">No recent deletion audits.</div>
              )}
            </div>
          </div>
        )}
      </Section>
      {/* System alerts */}
      <Section title={t("admin.systemAlerts", "System Alerts")} description={t("admin.systemAlertsDesc", "Critical system alerts and warnings.")}>
        <Button
          type="button"
          className="mb-4 px-4 py-2 bg-red-100 text-red-700 rounded shadow"
          onClick={sendTestAlert}
          disabled={isSendingAlert}
        >
          {isSendingAlert ? "Sending..." : "Trigger Test Alert"}
        </Button>
        {isAlertsLoading ? (
          <div className="text-sm text-muted-foreground">Loading alerts...</div>
        ) : alertsError ? (
          <div className="text-sm text-destructive">Failed to load alerts.</div>
        ) : systemAlerts && systemAlerts.length > 0 ? (
          <ul className="space-y-2">
            {systemAlerts.map(alert => (
              <li key={alert.id} className="flex items-center gap-2 text-red-600">
                <AlertTriangle className="w-5 h-5" />
                <span>{alert.message}</span>
                <span className="text-xs text-muted-foreground">({alert.severity})</span>
              </li>
            ))}
          </ul>
        ) : (
          <div className="flex items-center gap-2 text-red-600">
            <AlertTriangle className="w-5 h-5" />
            <span>No critical alerts.</span>
          </div>
        )}
      </Section>
      {/* System logs (read-only, monitoring) */}
      <Section title={t("admin.systemLogs")} description={t("admin.systemLogsDesc")}
        >
        {isLogsLoading ? (
          <div className="text-sm text-muted-foreground">{t("loading")}</div>
        ) : logsError ? (
          <div className="text-sm text-destructive">{t("settings.errors.loadFailed")}</div>
        ) : systemLogs && systemLogs.length > 0 ? (
          <ul className="text-sm text-muted-foreground space-y-1">
            {systemLogs.slice(0, 5).map((log) => {
              // Friendly summary for template changes
              let templateChangeMsg = "No invitation template changes";
              if (log.inviteTemplateEnChanged || log.inviteTemplateFrChanged) {
                templateChangeMsg = "Invitation template changed";
              }
              // Friendly summary for language changes
              let langChangeMsg = "Default language did not change";
              if (log.previousDefaultLanguage && log.newDefaultLanguage && log.previousDefaultLanguage !== log.newDefaultLanguage) {
                langChangeMsg = `Default language changed from ${log.previousDefaultLanguage.toUpperCase()} to ${log.newDefaultLanguage.toUpperCase()}`;
              }
              return (
                <li key={log.id} className="flex flex-col md:flex-row md:items-center md:gap-2">
                  <span className="font-mono text-xs text-muted-foreground">{log.timestamp ? new Date(log.timestamp).toLocaleString() : "—"}</span>
                  <span className="flex-1">{templateChangeMsg}</span>
                  <span className="hidden md:inline-block">{langChangeMsg}</span>
                </li>
              );
            })}
          </ul>
        ) : (
          <div className="text-sm text-muted-foreground">No system logs found. Recent system changes will appear here.</div>
        )}
        <div
          className="text-xs text-blue-700 cursor-pointer hover:underline mt-2"
          role="button"
          tabIndex={0}
          aria-label="View all system logs"
          onClick={() => navigate("/admin/system-logs")}
          onKeyPress={e => { if (e.key === "Enter" || e.key === " ") navigate("/admin/system-logs"); }}
        >
          {t("admin.viewAllSystemLogs", "View all system logs")}
        </div>
      </Section>
    </div>
  );
}
