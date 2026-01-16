import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Home, FileCheck, Loader2, Inbox, CheckCircle2, Info, DollarSign, Building2 } from "lucide-react";
import { useState, useCallback, useMemo } from "react";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/shared/components/ui/popover";
import { useClientDashboardStats } from "@/features/dashboard/hooks/useDashboardStats";
import { TransactionOverviewCard } from "@/features/documents/components/TransactionOverviewCard";
import { UpcomingAppointmentsWidget } from "@/features/dashboard/components/UpcomingAppointmentsWidget";
import { useNotifications, useMarkNotificationAsRead } from "@/features/notifications/api/notificationsApi";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { useCurrentUser } from "@/features/auth/api/useCurrentUser";
import { useTransactionOffers, useTransactionProperties } from "@/features/transactions/api/queries";

export function ClientDashboardPage() {
  const { t } = useTranslation("dashboard");
  const { t: tNotifications } = useTranslation("notifications");
  const { t: tTransactions } = useTranslation("transactions");
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
  const { mutate: markAsRead } = useMarkNotificationAsRead();

  const safeNotifications = useMemo(() => notifications ?? [], [notifications]);

  const [openDocsNeeded, setOpenDocsNeeded] = useState(false);
  const [openDocsSubmitted, setOpenDocsSubmitted] = useState(false);
  const [openActiveTransactions, setOpenActiveTransactions] = useState(false);
  // Grid-only view

  // Translate notification message
  const getNotificationMessage = (notif: typeof safeNotifications[number]) => {
    if (notif.messageKey) {
      let params: Record<string, string> = {};
      if (notif.params) {
        try {
          params = JSON.parse(notif.params);
        } catch {
          // Ignore parse errors
        }
      }
      // Translate conditionType if present
      if (params.conditionType) {
        params.conditionType = tTransactions(`conditionTypes.${params.conditionType}`, { defaultValue: params.conditionType });
      }
      return tNotifications(notif.messageKey, params) || notif.message;
    }
    return notif.message;
  };

  const recentNotifications = useMemo(() => {
    if (!safeNotifications.length) return [];
    return [...safeNotifications]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(0, 5);
  }, [safeNotifications]);

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

  const getRequestedDocumentCountByTransaction = useCallback((transactionId: string) => {
    return clientDocuments.filter(
      d => d.transactionRef?.transactionId === transactionId && d.status === 'REQUESTED'
    ).length;
  }, [clientDocuments]);

  // Fallback offers count: fetch offers for the first SELL_SIDE transaction when backend KPI is missing
  const firstSellSideTx = useMemo(() => transactions.find(tx => tx.side === "SELL_SIDE"), [transactions]);
  const { data: offersForKpi = [] } = useTransactionOffers(
    firstSellSideTx?.transactionId ?? "",
    !!firstSellSideTx,
    firstSellSideTx?.clientId
  );

  const offersCount = (kpis?.global?.offersReceived ?? 0) || offersForKpi.length;

  // Fallback properties count: fetch properties for the first BUY_SIDE transaction
  const firstBuySideTx = useMemo(() => transactions.find(tx => tx.side === "BUY_SIDE"), [transactions]);
  const { data: propertiesForKpi = [] } = useTransactionProperties(
    firstBuySideTx?.transactionId ?? ""
  );

  const propertiesCount = propertiesForKpi.length;

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
        {firstSellSideTx && (
          <KpiCard
            title={t("client.offers", "Offers")}
            value={offersCount.toString()}
            icon={<DollarSign className="w-4 h-4 text-green-600" />}
            className="border border-green-200 dark:border-green-800 border-l-4 border-l-green-500 bg-white dark:bg-slate-900 shadow-sm"
            onClick={() => {
              // Find first sell-side transaction and navigate to offers tab
              const sellSideTransaction = transactions?.find(tx => tx.side === "SELL_SIDE");
              if (sellSideTransaction) {
                navigate(`/transactions/${sellSideTransaction.transactionId}?tab=offers`);
              }
            }}
            infoButton={
              <Popover>
                <PopoverTrigger asChild>
                  <button
                    className="p-1.5 hover:bg-green-100 dark:hover:bg-green-900/30 rounded-full transition-colors"
                    aria-label="More information about offers"
                    onClick={(e) => {
                      e.stopPropagation();
                    }}
                  >
                    <Info className="w-5 h-5 text-green-600 dark:text-green-400" />
                  </button>
                </PopoverTrigger>
                <PopoverContent className="w-64">
                  <div className="space-y-2">
                    <h4 className="font-semibold text-sm">{t("client.offers", "Offers")}</h4>
                    <p className="text-xs text-muted-foreground">
                      {t("client.offersDesc", "Total offers received from potential buyers for your property.")}
                    </p>
                  </div>
                </PopoverContent>
              </Popover>
            }
          />
        )}
        {firstBuySideTx && (
          <KpiCard
            title={t("client.properties", "Properties")}
            value={propertiesCount.toString()}
            icon={<Building2 className="w-4 h-4 text-blue-600" />}
            className="border border-blue-200 dark:border-blue-800 border-l-4 border-l-blue-500 bg-white dark:bg-slate-900 shadow-sm"
            onClick={() => {
              // Find first buy-side transaction and navigate to properties tab
              const buySideTransaction = transactions?.find(tx => tx.side === "BUY_SIDE");
              if (buySideTransaction) {
                navigate(`/transactions/${buySideTransaction.transactionId}?tab=properties`);
              }
            }}
            infoButton={
              <Popover>
                <PopoverTrigger asChild>
                  <button
                    className="p-1.5 hover:bg-blue-100 dark:hover:bg-blue-900/30 rounded-full transition-colors"
                    aria-label="More information about properties"
                    onClick={(e) => {
                      e.stopPropagation();
                    }}
                  >
                    <Info className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                  </button>
                </PopoverTrigger>
                <PopoverContent className="w-64">
                  <div className="space-y-2">
                    <h4 className="font-semibold text-sm">{t("client.properties", "Properties")}</h4>
                    <p className="text-xs text-muted-foreground">
                      {t("client.propertiesDesc", "Total properties you are considering for purchase.")}
                    </p>
                  </div>
                </PopoverContent>
              </Popover>
            }
          />
        )}
        <KpiCard
          title={t("client.activeTransactions")}
          value={kpis.global.activeTransactions.toString()}
          icon={<Home className="w-4 h-4" />}
          onClick={() => {
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
          onClick={() => {
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
          onClick={() => {
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
                requestedDocumentCount={getRequestedDocumentCountByTransaction(transaction.transactionId)}
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
                className={`space-y-4 ${recentNotifications.length > 3 ? "max-h-64 overflow-y-auto pr-2" : ""
                  }`}
              >
                {recentNotifications.map((notif) => (
                  <div
                    key={notif.publicId}
                    className="flex gap-3 text-sm cursor-pointer hover:opacity-80 transition-opacity"
                    role="button"
                    tabIndex={0}
                    onClick={() => {
                      if (!notif.read) {
                        markAsRead(notif.publicId);
                      }
                      if (notif.relatedTransactionId) {
                        navigate(`/transactions/${notif.relatedTransactionId}`);
                      }
                    }}
                    onKeyDown={(e) => {
                      if ((e.key === 'Enter' || e.key === ' ')) {
                        e.preventDefault();
                        if (!notif.read) {
                          markAsRead(notif.publicId);
                        }
                        if (notif.relatedTransactionId) {
                          navigate(`/transactions/${notif.relatedTransactionId}`);
                        }
                      }
                    }}
                  >
                    <div
                      className={`w-2 h-2 rounded-full mt-2 flex-shrink-0 ${notif.read ? 'bg-gray-300' : 'bg-orange-500'
                        }`}
                    />
                    <div className="flex-1">
                      <p className="font-medium">{getNotificationMessage(notif)}</p>
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
