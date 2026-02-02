import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import {
  FileText,
  Users,
  Plus,
  List,
  FileCheck,
  Bell,
} from "lucide-react";
import { useBrokerDashboardStats } from "@/features/dashboard/hooks/useDashboardStats";
import { QuickLinksGrid, type QuickLink } from "@/features/dashboard/components/QuickLinksGrid";
import { AppointmentWidget } from "@/features/dashboard/components/AppointmentWidget";
import { PriorityCardsSection } from "@/features/dashboard/components/PriorityCardsSection";
import { RecentActivityFeed } from "@/features/dashboard/components/RecentActivityFeed";
import { CreateTransactionModal } from "@/features/transactions/components/CreateTransactionModal";
import { CreateAppointmentModal } from "@/features/appointments/components/CreateAppointmentModal";

export function BrokerDashboardPage() {
  const { t } = useTranslation("dashboard");
  const navigate = useNavigate();
  const { data: stats, isLoading } = useBrokerDashboardStats();
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [isRequestAppointmentOpen, setIsRequestAppointmentOpen] = useState(false);

  const quickLinks: QuickLink[] = [
    {
      id: "new-transaction",
      label: t("broker.quickLinks.newTransaction"),
      icon: <Plus className="w-7 h-7" />,
      onClick: () => setIsCreateModalOpen(true),
      variant: "default",
    },
    {
      id: "all-transactions",
      label: t("broker.quickLinks.allTransactions"),
      icon: <List className="w-7 h-7" />,
      href: "/transactions",
    },
    {
      id: "clients",
      label: t("broker.quickLinks.clients"),
      icon: <Users className="w-7 h-7" />,
      href: "/clients",
    },
    {
      id: "pending-documents",
      label: t("broker.quickLinks.pendingDocuments"),
      icon: <FileCheck className="w-7 h-7" />,
      href: "/documents?status=SUBMITTED",
    },
    {
      id: "request-appointment",
      label: t("broker.quickLinks.requestAppointment", "Request Appointment"),
      icon: <Plus className="w-7 h-7" />,
      onClick: () => setIsRequestAppointmentOpen(true),
      variant: "default",
    },
    {
      id: "notifications",
      label: t("broker.quickLinks.notifications"),
      icon: <Bell className="w-7 h-7" />,
      href: "/notifications",
    },
  ];

  if (isLoading) {
    return <LoadingState message={t("loading")} />;
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("broker.title")}
        subtitle={t("broker.subtitle")}
      />

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <KpiCard
          title={t("broker.activeTransactions")}
          value={stats?.activeTransactions?.toString() || "0"}
          icon={<FileText className="w-4 h-4" />}
          onClick={() => navigate("/transactions")}
        />
        <KpiCard
          title={t("broker.activeClients")}
          value={stats?.activeClients?.toString() || "0"}
          icon={<Users className="w-4 h-4" />}
          onClick={() => navigate("/clients?filter=active")}
        />
      </div>


      {/* Appointment Widget */}
      <div className="my-6">
        <AppointmentWidget />
      </div>

      {/* Quick Links */}
      <Section
        title={t("broker.quickLinks.title")}
        description={t("broker.quickLinks.description")}
      >
        <QuickLinksGrid links={quickLinks} />
      </Section>

      {/* Priority Cards - Expiring Offers, Pending Documents, Upcoming Appointments */}
      <PriorityCardsSection />

      {/* Recent Activity Feed */}
      <RecentActivityFeed />

      {/* Create Transaction Modal */}
      <CreateTransactionModal
        isOpen={isCreateModalOpen}
        onClose={() => setIsCreateModalOpen(false)}
        onSuccess={() => setIsCreateModalOpen(false)}
      />
      {/* Request Appointment Modal */}
      <CreateAppointmentModal
        isOpen={isRequestAppointmentOpen}
        onClose={() => setIsRequestAppointmentOpen(false)}
        onSubmit={() => setIsRequestAppointmentOpen(false)}
      />
    </div>
  );
}

