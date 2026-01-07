import { useTranslation } from "react-i18next";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { Home, FileCheck } from "lucide-react";
import type { ClientDashboardStats } from "@/features/dashboard/hooks/useDashboardStats";

interface DashboardStatsWidgetProps {
  stats: ClientDashboardStats;
}

export function DashboardStatsWidget({
  stats,
}: DashboardStatsWidgetProps) {
  const { t } = useTranslation("dashboard");

  return (
    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
      <KpiCard
        title={t("kpi.activeTransactions", "Active Transactions")}
        value={stats.activeTransactions}
        icon={<Home className="w-4 h-4 text-blue-600" />}
        className="border-l-4 border-blue-500"
      />
      <KpiCard
        title={t("kpi.documentsNeeded", "Documents Needed")}
        value={stats.documentsNeeded}
        icon={<FileCheck className="w-4 h-4 text-orange-600" />}
        className="border-l-4 border-orange-500"
      />
      <KpiCard
        title={t("kpi.documentsSubmitted", "Documents Submitted")}
        value={stats.documentsSubmitted}
        icon={<FileCheck className="w-4 h-4 text-green-600" />}
        className="border-l-4 border-green-500"
      />
    </div>
  );
}
