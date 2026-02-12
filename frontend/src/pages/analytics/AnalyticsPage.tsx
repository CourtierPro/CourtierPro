import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { KpiCard } from "@/shared/components/branded/KpiCard";
import { Section } from "@/shared/components/branded/Section";
import { StatLine } from "@/shared/components/branded/StatLine";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { useAnalytics } from "@/features/analytics/api/queries";
import {
  BarChart3,
  Home,
  FileText,
  Calendar,
  TrendingUp,
  Building2,
  HandCoins,
  CheckCircle,
  XCircle,
  Activity,
  Clock,
  Users,
  AlertTriangle,
  ShieldCheck,
  Target,
  DollarSign,
  ArrowUpDown,
  Gavel,
  Eye,
  UserCheck,
  CalendarClock,
  Flame,
  Pause,
} from "lucide-react";
import { Card, CardContent } from "@/shared/components/ui/card";

function formatCurrency(value: number): string {
  if (value === 0) return "$0";
  return "$" + value.toLocaleString("en-US", { maximumFractionDigits: 0 });
}

function formatPercent(value: number): string {
  return value + "%";
}

export function AnalyticsPage() {
  const { t } = useTranslation("analytics");
  const { data, isLoading, isError, refetch } = useAnalytics();

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title={t("title")} subtitle={t("subtitle")} />
        <LoadingState message={t("loading")} />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="space-y-6">
        <PageHeader title={t("title")} subtitle={t("subtitle")} />
        <ErrorState message={t("empty")} onRetry={() => refetch()} />
      </div>
    );
  }

  if (data.totalTransactions === 0) {
    return (
      <div className="space-y-6">
        <PageHeader title={t("title")} subtitle={t("subtitle")} />
        <EmptyState
          icon={<BarChart3 />}
          title={t("title")}
          description={t("empty")}
        />
      </div>
    );
  }

  const buyerStages = Object.entries(data.buyerStageDistribution);
  const sellerStages = Object.entries(data.sellerStageDistribution);
  const maxBuyerCount = Math.max(...buyerStages.map(([, v]) => v), 1);
  const maxSellerCount = Math.max(...sellerStages.map(([, v]) => v), 1);

  const allMonths = [
    ...new Set([
      ...Object.keys(data.transactionsOpenedPerMonth),
      ...Object.keys(data.transactionsClosedPerMonth),
    ]),
  ].sort();
  const maxMonthly = Math.max(
    ...allMonths.map(
      (m) =>
        (data.transactionsOpenedPerMonth[m] || 0) +
        (data.transactionsClosedPerMonth[m] || 0)
    ),
    1
  );

  return (
    <div className="space-y-6">
      <PageHeader title={t("title")} subtitle={t("subtitle")} />

      {/* ─── TRANSACTION OVERVIEW ─── */}
      <Section title={t("sections.overview")}>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <KpiCard title={t("stats.totalTransactions")} value={data.totalTransactions} icon={<BarChart3 className="h-5 w-5" />} />
          <KpiCard title={t("stats.activeTransactions")} value={data.activeTransactions} icon={<Activity className="h-5 w-5" />} />
          <KpiCard title={t("stats.closedTransactions")} value={data.closedTransactions} icon={<CheckCircle className="h-5 w-5" />} />
          <KpiCard title={t("stats.terminatedTransactions")} value={data.terminatedTransactions} icon={<XCircle className="h-5 w-5" />} />
          <KpiCard title={t("stats.buyTransactions")} value={data.buyTransactions} icon={<Home className="h-5 w-5" />} />
          <KpiCard title={t("stats.sellTransactions")} value={data.sellTransactions} icon={<HandCoins className="h-5 w-5" />} />
          <KpiCard title={t("stats.successRate")} value={formatPercent(data.successRate)} icon={<Target className="h-5 w-5" />} />
          <KpiCard title={t("stats.avgDuration")} value={data.avgTransactionDurationDays} icon={<Clock className="h-5 w-5" />} />
        </div>
        <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 gap-3">
          <StatLine label={t("stats.longestDuration")} value={data.longestDurationDays} icon={<Clock className="h-4 w-4" />} />
          <StatLine label={t("stats.shortestDuration")} value={data.shortestDurationDays} icon={<Clock className="h-4 w-4" />} />
        </div>
      </Section>

      {/* ─── MONTHLY ACTIVITY ─── */}
      {allMonths.length > 0 && (
        <Section title={t("sections.monthlyActivity")}>
          <div className="space-y-3">
            {allMonths.map((month) => {
              const opened = data.transactionsOpenedPerMonth[month] || 0;
              const closed = data.transactionsClosedPerMonth[month] || 0;
              return (
                <div key={month} className="flex items-center gap-3">
                  <span className="text-sm text-muted-foreground w-24 shrink-0 font-mono">
                    {month}
                  </span>
                  <div className="flex-1 flex gap-1 h-7">
                    {opened > 0 && (
                      <div
                        className="bg-blue-500 rounded-l-md transition-all duration-500 ease-out flex items-center justify-center text-[11px] text-white font-medium min-w-[20px]"
                        style={{ width: `${(opened / maxMonthly) * 100}%` }}
                        title={`${t("stats.opened")}: ${opened}`}
                      >
                        {opened}
                      </div>
                    )}
                    {closed > 0 && (
                      <div
                        className="bg-emerald-500 rounded-r-md transition-all duration-500 ease-out flex items-center justify-center text-[11px] text-white font-medium min-w-[20px]"
                        style={{ width: `${(closed / maxMonthly) * 100}%` }}
                        title={`${t("stats.closed")}: ${closed}`}
                      >
                        {closed}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
            <div className="flex items-center gap-4 mt-2 pl-24">
              <div className="flex items-center gap-1.5">
                <div className="w-3 h-3 rounded-sm bg-blue-500" />
                <span className="text-xs text-muted-foreground">{t("stats.opened")}</span>
              </div>
              <div className="flex items-center gap-1.5">
                <div className="w-3 h-3 rounded-sm bg-emerald-500" />
                <span className="text-xs text-muted-foreground">{t("stats.closed")}</span>
              </div>
            </div>
          </div>
        </Section>
      )}

      {/* ─── STAGE DISTRIBUTION ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {buyerStages.length > 0 && (
          <Section title={t("stageLabels.buyerStages")}>
            <div className="space-y-3">
              {buyerStages.map(([stage, count]) => (
                <StageBar key={stage} label={t(`stages.${stage}`, stage)} count={count} max={maxBuyerCount} color="bg-blue-500" />
              ))}
            </div>
          </Section>
        )}
        {sellerStages.length > 0 && (
          <Section title={t("stageLabels.sellerStages")}>
            <div className="space-y-3">
              {sellerStages.map(([stage, count]) => (
                <StageBar key={stage} label={t(`stages.${stage}`, stage)} count={count} max={maxSellerCount} color="bg-emerald-500" />
              ))}
            </div>
          </Section>
        )}
      </div>

      {/* ─── HOUSE VISITS ─── */}
      <Section title={t("sections.houseVisits")}>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <KpiCard title={t("stats.totalHouseVisits")} value={data.totalHouseVisits} icon={<Building2 className="h-5 w-5" />} />
          <KpiCard title={t("stats.avgHouseVisits")} value={data.avgHouseVisitsPerClosedTransaction} icon={<TrendingUp className="h-5 w-5" />} />
        </div>
      </Section>

      {/* ─── PROPERTIES + BUYER OFFERS (side by side) ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Section title={t("sections.properties")}>
          <div className="grid grid-cols-2 gap-3 mb-3">
            <KpiCard title={t("stats.totalProperties")} value={data.totalProperties} icon={<Building2 className="h-5 w-5" />} />
            <KpiCard title={t("stats.avgPropertiesPerBuy")} value={data.avgPropertiesPerBuyTransaction} icon={<TrendingUp className="h-5 w-5" />} />
          </div>
          <div className="space-y-2">
            <StatLine label={t("stats.propertyInterestRate")} value={formatPercent(data.propertyInterestRate)} icon={<Eye className="h-4 w-4" />} variant="success" />
            <StatLine label={t("stats.propertiesNeedingInfo")} value={data.propertiesNeedingInfo} icon={<AlertTriangle className="h-4 w-4" />} variant={data.propertiesNeedingInfo > 0 ? "warning" : "default"} />
            <StatLine label={t("stats.propertiesWithOffers")} value={data.propertiesWithOffers} icon={<Gavel className="h-4 w-4" />} variant="success" />
            <StatLine label={t("stats.propertiesWithoutOffers")} value={data.propertiesWithoutOffers} icon={<Gavel className="h-4 w-4" />} />
          </div>
        </Section>

        <Section title={t("sections.buyerOffers")}>
          <div className="grid grid-cols-2 gap-3 mb-3">
            <KpiCard title={t("stats.totalBuyerOffers")} value={data.totalBuyerOffers} icon={<Gavel className="h-5 w-5" />} />
            <KpiCard title={t("stats.buyerOfferAcceptanceRate")} value={formatPercent(data.buyerOfferAcceptanceRate)} icon={<CheckCircle className="h-5 w-5" />} />
          </div>
          <div className="space-y-2">
            <StatLine label={t("stats.avgOfferRounds")} value={data.avgOfferRounds} icon={<ArrowUpDown className="h-4 w-4" />} />
            <StatLine label={t("stats.avgBuyerOfferAmount")} value={formatCurrency(data.avgBuyerOfferAmount)} icon={<DollarSign className="h-4 w-4" />} />
            <StatLine label={t("stats.expiredOrWithdrawnOffers")} value={data.expiredOrWithdrawnOffers} icon={<XCircle className="h-4 w-4" />} variant={data.expiredOrWithdrawnOffers > 0 ? "warning" : "default"} />
            <StatLine label={t("stats.buyerCounterOfferRate")} value={formatPercent(data.buyerCounterOfferRate)} icon={<ArrowUpDown className="h-4 w-4" />} />
          </div>
        </Section>
      </div>

      {/* ─── RECEIVED OFFERS (SELL-SIDE) ─── */}
      <Section title={t("sections.receivedOffers")}>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-3">
          <KpiCard title={t("stats.totalOffers")} value={data.totalOffers} icon={<HandCoins className="h-5 w-5" />} />
          <KpiCard title={t("stats.receivedOfferAcceptanceRate")} value={formatPercent(data.receivedOfferAcceptanceRate)} icon={<CheckCircle className="h-5 w-5" />} />
          <KpiCard title={t("stats.highestOfferAmount")} value={formatCurrency(data.highestOfferAmount)} icon={<TrendingUp className="h-5 w-5" />} />
          <KpiCard title={t("stats.pendingOrReviewOffers")} value={data.pendingOrReviewOffers} icon={<Clock className="h-5 w-5" />} />
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-2">
          <StatLine label={t("stats.avgReceivedOfferAmount")} value={formatCurrency(data.avgReceivedOfferAmount)} icon={<DollarSign className="h-4 w-4" />} />
          <StatLine label={t("stats.lowestOfferAmount")} value={formatCurrency(data.lowestOfferAmount)} icon={<DollarSign className="h-4 w-4" />} />
          <StatLine label={t("stats.avgOffersPerSell")} value={data.avgOffersPerSellTransaction} icon={<TrendingUp className="h-4 w-4" />} />
          <StatLine label={t("stats.receivedCounterOfferRate")} value={formatPercent(data.receivedCounterOfferRate)} icon={<ArrowUpDown className="h-4 w-4" />} />
        </div>
      </Section>

      {/* ─── DOCUMENTS + APPOINTMENTS (side by side) ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Section title={t("sections.documents")}>
          <div className="grid grid-cols-2 gap-3 mb-3">
            <KpiCard title={t("stats.totalDocuments")} value={data.totalDocuments} icon={<FileText className="h-5 w-5" />} />
            <KpiCard title={t("stats.documentCompletionRate")} value={formatPercent(data.documentCompletionRate)} icon={<CheckCircle className="h-5 w-5" />} />
          </div>
          <div className="space-y-2">
            <StatLine label={t("stats.pendingDocuments")} value={data.pendingDocuments} icon={<Clock className="h-4 w-4" />} variant={data.pendingDocuments > 0 ? "warning" : "default"} />
            <StatLine label={t("stats.documentsNeedingRevision")} value={data.documentsNeedingRevision} icon={<AlertTriangle className="h-4 w-4" />} variant={data.documentsNeedingRevision > 0 ? "danger" : "default"} />
            <StatLine label={t("stats.avgDocumentsPerTransaction")} value={data.avgDocumentsPerTransaction} icon={<FileText className="h-4 w-4" />} />
          </div>
        </Section>

        <Section title={t("sections.appointments")}>
          <div className="grid grid-cols-2 gap-3 mb-3">
            <KpiCard title={t("stats.totalAppointments")} value={data.totalAppointments} icon={<Calendar className="h-5 w-5" />} />
            <KpiCard title={t("stats.appointmentConfirmationRate")} value={formatPercent(data.appointmentConfirmationRate)} icon={<CheckCircle className="h-5 w-5" />} />
          </div>
          <div className="space-y-2">
            <StatLine label={t("stats.upcomingAppointments")} value={data.upcomingAppointments} icon={<CalendarClock className="h-4 w-4" />} variant="success" />
            <StatLine label={t("stats.declinedAppointmentRate")} value={formatPercent(data.declinedAppointmentRate)} icon={<XCircle className="h-4 w-4" />} variant={data.declinedAppointmentRate > 0 ? "warning" : "default"} />
            <StatLine label={t("stats.cancelledAppointmentRate")} value={formatPercent(data.cancelledAppointmentRate)} icon={<XCircle className="h-4 w-4" />} variant={data.cancelledAppointmentRate > 0 ? "warning" : "default"} />
            <StatLine label={t("stats.avgAppointmentsPerTransaction")} value={data.avgAppointmentsPerTransaction} icon={<Calendar className="h-4 w-4" />} />
          </div>
        </Section>
      </div>

      {/* ─── CONDITIONS ─── */}
      <Section title={t("sections.conditions")}>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-3">
          <KpiCard title={t("stats.totalConditions")} value={data.totalConditions} icon={<ShieldCheck className="h-5 w-5" />} />
          <KpiCard title={t("stats.conditionSatisfiedRate")} value={formatPercent(data.conditionSatisfiedRate)} icon={<CheckCircle className="h-5 w-5" />} />
          <KpiCard title={t("stats.conditionsApproachingDeadline")} value={data.conditionsApproachingDeadline} icon={<AlertTriangle className="h-5 w-5" />} />
          <KpiCard title={t("stats.overdueConditions")} value={data.overdueConditions} icon={<XCircle className="h-5 w-5" />} />
        </div>
        <StatLine label={t("stats.avgConditionsPerTransaction")} value={data.avgConditionsPerTransaction} icon={<ShieldCheck className="h-4 w-4" />} />
      </Section>

      {/* ─── CLIENT ENGAGEMENT + TRENDS (side by side) ─── */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Section title={t("sections.clientEngagement")}>
          <div className="grid grid-cols-2 gap-3 mb-3">
            <KpiCard title={t("stats.totalActiveClients")} value={data.totalActiveClients} icon={<Users className="h-5 w-5" />} />
            <KpiCard title={t("stats.clientsWithMultipleTransactions")} value={data.clientsWithMultipleTransactions} icon={<UserCheck className="h-5 w-5" />} />
          </div>
          <div className="space-y-2">
            <StatLine label={t("stats.appointmentsByBroker")} value={data.appointmentsByBroker} icon={<UserCheck className="h-4 w-4" />} />
            <StatLine label={t("stats.appointmentsByClient")} value={data.appointmentsByClient} icon={<Users className="h-4 w-4" />} />
          </div>
        </Section>

        <Section title={t("sections.trends")}>
          <div className="space-y-3">
            <Card>
              <CardContent className="p-4 flex items-center gap-3">
                <div className="p-2 bg-orange-100 dark:bg-orange-950/40 rounded-lg">
                  <Flame className="h-5 w-5 text-orange-600 dark:text-orange-400" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">{t("stats.busiestMonth")}</p>
                  <p className="text-lg font-bold">{data.busiestMonth}</p>
                </div>
              </CardContent>
            </Card>
            <StatLine
              label={t("stats.idleTransactions")}
              value={data.idleTransactions}
              icon={<Pause className="h-4 w-4" />}
              variant={data.idleTransactions > 0 ? "danger" : "default"}
            />
          </div>
        </Section>
      </div>
    </div>
  );
}

interface StageBarProps {
  label: string;
  count: number;
  max: number;
  color: string;
}

function StageBar({ label, count, max, color }: StageBarProps) {
  const widthPercent = max > 0 ? (count / max) * 100 : 0;

  return (
    <div className="flex items-center gap-3">
      <span className="text-sm text-muted-foreground w-40 shrink-0 truncate">
        {label}
      </span>
      <div className="flex-1 h-6 bg-muted rounded-full overflow-hidden">
        <div
          className={`h-full ${color} rounded-full transition-all duration-500 ease-out`}
          style={{ width: `${widthPercent}%` }}
        />
      </div>
      <span className="text-sm font-semibold w-8 text-right">{count}</span>
    </div>
  );
}
