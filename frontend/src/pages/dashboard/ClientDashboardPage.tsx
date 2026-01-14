import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Home, FileCheck, Loader2, Inbox, CheckCircle2, Info } from "lucide-react";
import { useState, useEffect, useCallback } from "react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/shared/components/ui/popover";
import { useClientDashboardStats } from "@/features/dashboard/hooks/useDashboardStats";
import { TransactionOverviewCard } from "@/features/documents/components/TransactionOverviewCard";
import { UpcomingAppointmentsWidget } from "@/features/dashboard/components/UpcomingAppointmentsWidget";
import { useNotifications } from "@/features/notifications/api/notificationsApi";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { useCurrentUser } from "@/features/auth/api/useCurrentUser";

export function ClientDashboardPage() {
  const { t } = useTranslation("dashboard");
  const navigate = useNavigate();
  const { data: currentUser } = useCurrentUser();
  const clientId = currentUser?.id ?? "";
  const {
    transactions,
    clientDocuments,
    kpis,
    isLoading,
  } = useClientDashboardStats(clientId);

  const { data: notifications, isLoading: isNotificationsLoading } =
    useNotifications();

  const [openDocsNeeded, setOpenDocsNeeded] = useState(false);
  const [openDocsSubmitted, setOpenDocsSubmitted] = useState(false);
  const [openActiveTransactions, setOpenActiveTransactions] = useState(false);
  // Grid-only view

  // KPI change indicators (persistent until acknowledged)
  const [changedActive, setChangedActive] = useState(false);
  const [changedNeeded, setChangedNeeded] = useState(false);
  const [changedSubmitted, setChangedSubmitted] = useState(false);

  const ackKey = (key: string) => `kpi_client_${key}_ack`;
  const ensureAckInit = (key: string, value: number) => {
    const k = ackKey(key);
    if (localStorage.getItem(k) === null) {
      localStorage.setItem(k, String(value));
    }
  };
  const isChanged = useCallback((key: string, value: number) => {
    const ack = localStorage.getItem(`kpi_client_${key}_ack`);
    if (ack === null) return false;
    return Number(ack) !== value;
  }, []);

  // Initialize ack on first mount with current values to avoid first-load dot
  // Only initialize after data is loaded to prevent false change indicators
  useEffect(() => {
    if (!isLoading) {
      ensureAckInit("active", kpis.global.activeTransactions);
      ensureAckInit("needed", kpis.global.documentsNeeded);
      ensureAckInit("submitted", kpis.global.documentsSubmitted);
    }
    // ensureAckInit is a stable function defined above, no need to include in deps
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoading, kpis.global.activeTransactions, kpis.global.documentsNeeded, kpis.global.documentsSubmitted]);

  // Recompute changed states when KPIs update
  // These setStates synchronize local UI state with external data - legitimate pattern
  useEffect(() => {
    setChangedActive(isChanged("active", kpis.global.activeTransactions));
  }, [kpis.global.activeTransactions, isChanged]);
  useEffect(() => {
    setChangedNeeded(isChanged("needed", kpis.global.documentsNeeded));
  }, [kpis.global.documentsNeeded, isChanged]);
  useEffect(() => {
    setChangedSubmitted(isChanged("submitted", kpis.global.documentsSubmitted));
  }, [kpis.global.documentsSubmitted, isChanged]);

  const recentNotifications = notifications?.slice(0, 5) || [];

  const getDocumentCountByTransaction = useCallback((transactionId: string) => {
    return clientDocuments.filter(
      d => d.transactionRef?.transactionId === transactionId
    ).length;
  }, [clientDocuments]);

  const getApprovedDocumentCountByTransaction = useCallback((transactionId: string) => {
    return clientDocuments.filter(
      d => d.transactionRef?.transactionId === transactionId && d.status === 'APPROVED'
    ).length;
  }, [clientDocuments]);

  const getNeedsRevisionCountByTransaction = useCallback((transactionId: string) => {
    return clientDocuments.filter(
      d => d.transactionRef?.transactionId === transactionId && d.status === 'NEEDS_REVISION'
    ).length;
  }, [clientDocuments]);

  const getSubmittedDocumentCountByTransaction = useCallback((transactionId: string) => {
    // Count docs currently considered "submitted": SUBMITTED or APPROVED (exclude NEEDS_REVISION)
    const submittedStatuses = ["SUBMITTED", "APPROVED"] as const;
    return clientDocuments.filter(
      d => d.transactionRef?.transactionId === transactionId && submittedStatuses.includes(d.status as typeof submittedStatuses[number])
    ).length;
  }, [clientDocuments]);

  if (isLoading) {
    return <LoadingState message={t("loading")} />;
  }

  const welcomeMessage = currentUser?.firstName
    ? t("client.welcome", { name: currentUser.firstName })
    : t("client.title");

  return (
    <div className="space-y-6">
      <PageHeader
        title={welcomeMessage}
        subtitle={t("client.subtitle")}
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <KpiCard
          title={t("client.activeTransactions")}
          value={kpis.global.activeTransactions.toString()}
          icon={<Home className="w-4 h-4" />}
          changed={changedActive}
          onClick={() => {
            localStorage.setItem("kpi_client_active_ack", String(kpis.global.activeTransactions));
            setChangedActive(false);
            navigate("/my-transaction");
          }}
          infoButton={
            <Popover open={openActiveTransactions} onOpenChange={setOpenActiveTransactions}>
              <PopoverTrigger asChild>
                <button 
                  className="p-1.5 hover:bg-orange-100 dark:hover:bg-orange-900/30 rounded-full transition-colors"
                  aria-label="More information about active transactions"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (changedActive) {
                      localStorage.setItem("kpi_client_active_ack", String(kpis.global.activeTransactions));
                      setChangedActive(false);
                    }
                  }}
                >
                  <Info className="w-5 h-5 text-orange-600 dark:text-orange-400" />
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-64">
                <div className="space-y-2">
                  <h4 className="font-semibold text-sm">{t("client.activeTransactions", "Active Transactions")}</h4>
                  <p className="text-xs text-muted-foreground">
                    {t("client.activeTransactionsDesc", "The number of real estate transactions currently in progress.")}
                  </p>
                </div>
              </PopoverContent>
            </Popover>
          }
          className="border-l-4 border-l-orange-500 bg-gradient-to-r from-orange-50/50 to-transparent dark:from-orange-950/20"
        />
        <KpiCard
          title={t("client.documentsNeeded")}
          value={kpis.global.documentsNeeded.toString()}
          icon={<FileCheck className="w-4 h-4" />}
          changed={changedNeeded}
          onClick={() => {
            localStorage.setItem("kpi_client_needed_ack", String(kpis.global.documentsNeeded));
            setChangedNeeded(false);
            navigate("/my-documents");
          }}
          infoButton={
            <Popover open={openDocsNeeded} onOpenChange={setOpenDocsNeeded}>
              <PopoverTrigger asChild>
                <button 
                  className="p-1.5 hover:bg-slate-100 dark:hover:bg-slate-800/30 rounded-full transition-colors"
                  aria-label="More information about documents needed"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (changedNeeded) {
                      localStorage.setItem("kpi_client_needed_ack", String(kpis.global.documentsNeeded));
                      setChangedNeeded(false);
                    }
                  }}
                >
                  <Info className="w-5 h-5 text-slate-600 dark:text-slate-400" />
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-64">
                <div className="space-y-2">
                  <h4 className="font-semibold text-sm">{t("transaction.documentsRequired", "Documents Required")}</h4>
                  <p className="text-xs text-muted-foreground">
                    {t("transaction.documentsReqDesc", "These are the documents your broker has requested across all your transactions.")}
                  </p>
                </div>
              </PopoverContent>
            </Popover>
          }
          className="border-l-4 border-l-slate-500 bg-gradient-to-r from-slate-50/50 to-transparent dark:from-slate-950/20"
        />
        <KpiCard
          title={t("client.documentsSubmitted")}
          value={kpis.global.documentsSubmitted.toString()}
          icon={<FileCheck className="w-4 h-4" />}
          changed={changedSubmitted}
          onClick={() => {
            localStorage.setItem("kpi_client_submitted_ack", String(kpis.global.documentsSubmitted));
            setChangedSubmitted(false);
            navigate("/my-documents");
          }}
          infoButton={
            <Popover open={openDocsSubmitted} onOpenChange={setOpenDocsSubmitted}>
              <PopoverTrigger asChild>
                <button 
                  className="p-1.5 hover:bg-blue-100 dark:hover:bg-blue-900/30 rounded-full transition-colors"
                  aria-label="More information about documents submitted"
                  onClick={(e) => {
                    e.stopPropagation();
                    if (changedSubmitted) {
                      localStorage.setItem("kpi_client_submitted_ack", String(kpis.global.documentsSubmitted));
                      setChangedSubmitted(false);
                    }
                  }}
                >
                  <Info className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-64">
                <div className="space-y-2">
                  <h4 className="font-semibold text-sm">{t("transaction.submissionStatus", "Submission Status")}</h4>
                  <p className="text-xs text-muted-foreground">
                    {t("transaction.submittedDesc", "Documents that have been approved and submitted to your broker.")}
                  </p>
                </div>
              </PopoverContent>
            </Popover>
          }
          className="border-l-4 border-l-blue-500 bg-gradient-to-r from-blue-50/50 to-transparent dark:from-blue-950/20"
        />
      </div>

      <Section 
        title={t("client.myTransactions")} 
        description={t("client.myTransactionsDesc")}
      >
        {transactions.length === 0 ? (
          <Card>
            <CardContent className="flex flex-col items-center justify-center py-12">
              <Inbox className="h-16 w-16 text-muted-foreground/50 mb-4" />
              <h3 className="text-lg font-semibold mb-2">{t("client.noTransactionsTitle", "No transactions yet")}</h3>
              <p className="text-sm text-muted-foreground text-center max-w-md">
                {t("client.noTransactionsDesc", "Your real estate transactions will appear here. Contact your broker to get started.")}
              </p>
            </CardContent>
          </Card>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
            {transactions.map((transaction) => (
              <TransactionOverviewCard
                key={transaction.transactionId}
                transaction={transaction}
                documentCount={getDocumentCountByTransaction(transaction.transactionId)}
                approvedDocumentCount={getApprovedDocumentCountByTransaction(transaction.transactionId)}
                needsRevisionCount={getNeedsRevisionCountByTransaction(transaction.transactionId)}
                submittedDocumentCount={getSubmittedDocumentCountByTransaction(transaction.transactionId)}
              />
            ))}
          </div>
        )}
      </Section>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2">
          <UpcomingAppointmentsWidget />
        </div>
        
        <Card>
          <CardHeader>
            <CardTitle className="text-lg">{t("client.recentUpdates", "Recent Updates")}</CardTitle>
            <CardDescription>{t("client.recentUpdatesDesc", "Latest notifications and updates for your transactions.")}</CardDescription>
          </CardHeader>
          <CardContent>
            {isNotificationsLoading ? (
              <div className="flex justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            ) : recentNotifications.length > 0 ? (
              <div
                className={`space-y-4 ${
                  recentNotifications.length > 3 ? "max-h-64 overflow-y-auto pr-2" : ""
                }`}
              >
                {recentNotifications.map((notif) => (
                  <div 
                    key={notif.publicId} 
                    className="flex gap-3 text-sm cursor-pointer hover:opacity-80 transition-opacity"
                    role="button"
                    tabIndex={0}
                    onClick={() => notif.relatedTransactionId && navigate(`/transactions/${notif.relatedTransactionId}`)}
                    onKeyDown={(e) => {
                      if ((e.key === 'Enter' || e.key === ' ') && notif.relatedTransactionId) {
                        e.preventDefault();
                        navigate(`/transactions/${notif.relatedTransactionId}`);
                      }
                    }}
                  >
                    <div className="w-2 h-2 bg-orange-500 rounded-full mt-2 flex-shrink-0" />
                    <div className="flex-1">
                      <p className="font-medium">{notif.message}</p>
                      <p className="text-xs text-muted-foreground mt-1">
                        {new Date(notif.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                ))}
                {notifications && notifications.length > 5 && (
                  <Button 
                    variant="ghost" 
                    size="sm" 
                    className="w-full mt-2"
                    onClick={() => navigate("/notifications")}
                  >
                    {t("client.viewAllNotifications", "View all notifications")} →
                  </Button>
                )}
              </div>
            ) : (
              <div className="text-center py-8">
                <CheckCircle2 className="h-12 w-12 text-muted-foreground/50 mx-auto mb-2" />
                <p className="text-sm text-muted-foreground">{t("client.noNotificationsTitle", "All caught up!")}</p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
