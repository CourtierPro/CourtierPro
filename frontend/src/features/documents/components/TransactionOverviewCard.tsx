import { ShoppingCart, Home as HomeIcon, ChevronRight, FileText, DollarSign, AlertTriangle, Building2 } from "lucide-react";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import type { Transaction } from "@/features/transactions/api/queries";
import { useTransactionOffers, useTransactionProperties } from "@/features/transactions/api/queries";
import { useTransactionConditions } from "@/features/transactions/api/queries";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { getStageLabel, getStagesForSide, resolveStageIndex } from "@/shared/utils/stages";
import type { PropertyOfferStatus, ReceivedOfferStatus } from "@/shared/api/types";

/**
 * Calculate deadline status based on days remaining.
 * Returns: 'overdue' | 'urgent' (<=3 days) | 'warning' (<=7 days) | 'normal'
 */
function getDeadlineStatus(deadlineDate: string): 'overdue' | 'urgent' | 'warning' | 'normal' {
  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const deadline = new Date(deadlineDate);
  deadline.setHours(0, 0, 0, 0);

  const diffTime = deadline.getTime() - today.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

  if (diffDays < 0) return 'overdue';
  if (diffDays <= 3) return 'urgent';
  if (diffDays <= 7) return 'warning';
  return 'normal';
}

// Badge variant mapping for offer statuses (SELL_SIDE)
const offerStatusVariantMap: Record<ReceivedOfferStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
  PENDING: 'secondary',
  UNDER_REVIEW: 'secondary',
  COUNTERED: 'outline',
  ACCEPTED: 'default',
  DECLINED: 'destructive',
};

// Badge styling for property statuses (BUY_SIDE) - matching PropertyCard.tsx
const propertyStatusConfig: Record<PropertyOfferStatus, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; className: string }> = {
  OFFER_TO_BE_MADE: { variant: 'outline', className: 'border-muted-foreground text-muted-foreground' },
  OFFER_MADE: { variant: 'secondary', className: 'bg-blue-500/20 text-blue-600 dark:text-blue-400 border-blue-500/30' },
  COUNTERED: { variant: 'secondary', className: 'bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30' },
  ACCEPTED: { variant: 'secondary', className: 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border-emerald-500/30' },
  DECLINED: { variant: 'destructive', className: 'bg-red-500/20 text-red-600 dark:text-red-400 border-red-500/30' },
};

interface TransactionOverviewCardProps {
  transaction: Transaction;
  documentCount?: number;
  approvedDocumentCount?: number;
  needsRevisionCount?: number;
  submittedDocumentCount?: number;
  onViewDetails?: (transactionId: string) => void;
}

export function TransactionOverviewCard({
  transaction,
  documentCount = 0,
  approvedDocumentCount = 0,
  needsRevisionCount = 0,
  submittedDocumentCount = 0,
  onViewDetails,
}: TransactionOverviewCardProps) {
  const { t, i18n } = useTranslation("dashboard");
  const { t: tTx } = useTranslation("transactions");
  const locale = i18n.language === "fr" ? fr : enUS;
  const [expanded, setExpanded] = useState(false);
  const [panelType, setPanelType] = useState<"documents" | "transaction" | "offers" | "properties">("documents");
  const [animateRings, setAnimateRings] = useState(false);

  // Fetch offers for SELL_SIDE transactions
  const shouldFetchOffers = transaction.side === "SELL_SIDE";
  const { data: offersData = [] } = useTransactionOffers(
    transaction.transactionId,
    shouldFetchOffers,
    transaction.clientId
  );
  const offers = offersData || [];

  // Fetch properties for BUY_SIDE transactions
  const shouldFetchProperties = transaction.side === "BUY_SIDE";
  const { data: propertiesData = [] } = useTransactionProperties(
    shouldFetchProperties ? transaction.transactionId : ""
  );
  const properties = propertiesData || [];

  const { data: conditions = [] } = useTransactionConditions(transaction.transactionId);


  const getSideInfo = () => {
    if (transaction.side === "BUY_SIDE") {
      return {
        icon: <HomeIcon className="w-5 h-5 text-orange-600 dark:text-orange-400" />,
        label: t("transaction.buying", "Buying"),
        color: "text-orange-600 dark:text-orange-400",
        bgColor: "bg-orange-50 dark:bg-orange-950/30",
      };
    }
    return {
      icon: <ShoppingCart className="w-5 h-5 text-orange-600 dark:text-orange-400" />,
      label: t("transaction.selling", "Selling"),
      color: "text-orange-600 dark:text-orange-400",
      bgColor: "bg-orange-50 dark:bg-orange-950/30",
    };
  };

  const sideInfo = getSideInfo();

  // Resolve stages for side and current index (handles string stages when totalStages undefined)
  const stagesForSide = getStagesForSide(transaction.side);
  const totalStagesResolved = stagesForSide.length;
  const currentStageIndex = resolveStageIndex(
    transaction.currentStage as string | number | undefined,
    stagesForSide
  );

  const openedDate = transaction.openedDate ? new Date(transaction.openedDate) : null;
  const relativeTime = openedDate
    ? format(openedDate, "yyyy-MM-dd", { locale })
    : null;

  const humanizeStageText = (val: string | number | undefined, side: string | undefined) => {
    if (val === undefined || val === null) return t("transaction.stageUnknown", "Stage");
    if (typeof val === "number") {
      return t(`stages.${val}`, `Stage ${val}`);
    }
    return getStageLabel(String(val), tTx, side as "BUY_SIDE" | "SELL_SIDE" | undefined);
  };

  const stageLabel = humanizeStageText(
    transaction.currentStage as string | number | undefined,
    transaction.side
  );
  const stageValueDisplay =
    typeof transaction.currentStage === "number" && typeof transaction.totalStages === "number" && transaction.totalStages > 0
      ? `${transaction.currentStage}/${transaction.totalStages}`
      : stageLabel;

  const stageSummaryLine = openedDate
    ? `${t("transaction.opened", "Opened")} ${relativeTime} • ${t("transaction.stage", "Stage")} ${stageValueDisplay}`
    : `${t("transaction.stage", "Stage")} ${stageValueDisplay}`;

  const circumference = 2 * Math.PI * 50;
  const ringOffset = (pct: number) => circumference * (1 - pct / 100);

  // Submitted/Approved/Revision rings
  const submittedCount = submittedDocumentCount;
  const approvedCount = approvedDocumentCount;
  const revisionCount = needsRevisionCount;

  // Submitted ring should represent submitted over requested (total documents)
  const submittedPercent = documentCount
    ? Math.min(100, Math.max(0, (submittedCount / documentCount) * 100))
    : 0;
  const approvedPercent = submittedCount
    ? Math.min(100, Math.max(0, (approvedCount / submittedCount) * 100))
    : 0;
  const revisionPercent = submittedCount
    ? Math.min(100, Math.max(0, (revisionCount / submittedCount) * 100))
    : 0;
  const pendingCount = Math.max(0, submittedCount - approvedCount - revisionCount);

  const ringSubmitted = {
    key: "submitted",
    label: t("transaction.submitted", "Submitted"),
    value: `${submittedCount}/${documentCount || 0}`,
    percent: submittedPercent,
    color: "#0ea5e9",
    track: "#dbeafe",
    legend: t("transaction.submittedLegend", "Documents submitted"),
  };
  const ringApproved = {
    key: "approved",
    label: t("transaction.approved", "Approved"),
    value: `${approvedCount}/${submittedCount || 0}`,
    percent: approvedPercent,
    color: "#22c55e",
    track: "#bbf7d0",
    legend: t("transaction.approvedLegend", "Documents approved"),
  };
  const ringRevision = {
    key: "revision",
    label: t("transaction.needsRevision", "Needs revision"),
    value: `${revisionCount}/${submittedCount || 0}`,
    percent: revisionPercent,
    color: "#f97316",
    track: "#fed7aa",
    legend: t("transaction.revisionLegend", "Pending fixes"),
  };

  // Trigger ring fill animation whenever documents panel opens or data changes
  useEffect(() => {
    if (expanded && panelType === "documents") {
      const timeout = setTimeout(() => setAnimateRings(true), 50);
      return () => {
        clearTimeout(timeout);
        setAnimateRings(false);
      };
    }
  }, [expanded, panelType, ringSubmitted.percent, ringApproved.percent, ringRevision.percent]);

  const cardStyle = "bg-gradient-to-br from-orange-50/50 via-white to-white dark:from-orange-950/20 dark:via-background dark:to-background border-l-4 border-l-orange-500";

  return (
    <Card className={`relative overflow-visible hover:shadow-lg transition-all duration-300 ${cardStyle}`}>
      <CardHeader className="pb-2 pt-4 px-4 relative z-10">
        <div className="flex items-start justify-between gap-3 mb-3">
          {/* Icône Buy/Sell avec label visible */}
          <div className="flex flex-col items-center gap-1">
            <div className={`p-2 rounded-lg ${sideInfo.bgColor}`}>
              <div className={sideInfo.color}>{sideInfo.icon}</div>
            </div>
            <span className="text-xs font-medium text-muted-foreground whitespace-nowrap hidden"></span>
          </div>
          
          <div className="flex-1">
            <CardTitle className="text-base font-semibold">
              {transaction.propertyAddress?.street || t("transaction.noAddress", "No address")}
            </CardTitle>
            <p className="text-xs text-muted-foreground mt-1">
              {stageSummaryLine}
            </p>
          </div>
        </div>

        {/* Transaction type and stage badges */}
        <div className="flex flex-wrap gap-2 items-center mt-2">
          <Badge className="bg-orange-600 text-white hover:bg-orange-700">
            {sideInfo.label}
          </Badge>
          <Badge variant="outline" className="border-orange-300 text-orange-700 dark:text-orange-400 dark:border-orange-700">
            Stage: {stageValueDisplay}
          </Badge>
        </div>
      </CardHeader>

      <CardContent
        className={`space-y-3 px-4 pb-4 relative z-10 ${expanded ? "lg:flex lg:items-start lg:justify-between lg:gap-6" : ""}`}
      >
        <div className={expanded ? "flex-1 space-y-3" : "space-y-3"}>
          {/* Mini timeline dans la card (stages avec point, stage actuel plus gros + label) */}
          {totalStagesResolved > 1 && (
            <div className="relative px-2 py-8 mt-8 mb-4">
              {/* Progress indicator */}
              <div className="flex justify-between items-center mb-8">
                <span className="text-xs font-medium text-muted-foreground">
                  {t("transaction.stage", "Stage")} {currentStageIndex + 1} / {totalStagesResolved}
                </span>
              </div>
              {/* Dots container with relative positioning */}
              <div className="relative">
                {/* Connector line segments - centered with dots */}
                <div className="absolute left-0 right-0 top-1/2 -translate-y-1/2 flex items-center px-4">
                  {Array.from({ length: totalStagesResolved - 1 }).map((_, idx) => (
                    <div
                      key={`line-${idx}`}
                      className={`h-0.5 flex-1 transition-colors duration-300 ${
                        idx < currentStageIndex
                          ? "bg-orange-500"
                          : "bg-slate-300 dark:bg-slate-700"
                      }`}
                    />
                  ))}
                </div>
                {/* Dots */}
                <div className="relative flex items-center justify-between">
                {Array.from({ length: totalStagesResolved }).map((_, idx) => {
                  const stageName = getStageLabel(stagesForSide[idx], tTx, transaction.side as "BUY_SIDE" | "SELL_SIDE" | undefined);
                  const isCurrent = idx === currentStageIndex;
                  const isCompleted = idx < currentStageIndex;
                  
                  return (
                    <div 
                      key={`dot-${idx}`} 
                      className="relative flex flex-col items-center group cursor-pointer z-10"
                      role="button"
                      tabIndex={0}
                      aria-label={`Stage ${idx + 1} of ${totalStagesResolved}: ${stageName}${isCurrent ? ' (current)' : isCompleted ? ' (completed)' : ''}`}
                      onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                          e.preventDefault();
                          // Could potentially trigger stage details or navigation here
                        }
                      }}
                    >
                      <div className="relative">
                        {/* Stage number badge on hover - better contrast */}
                        <div className="absolute -top-6 left-1/2 -translate-x-1/2 opacity-0 group-hover:opacity-100 group-focus:opacity-100 transition-opacity duration-300 z-20">
                          <span className="text-xs font-bold text-white bg-orange-600 dark:bg-orange-500 px-2 py-1 rounded-full shadow-md">
                            {idx + 1}
                          </span>
                        </div>
                        {/* Dot */}
                        <div
                          className={`rounded-full transition-all duration-300 ${
                            isCurrent
                              ? "w-3 h-3 bg-gradient-to-br from-orange-500 to-orange-600 dark:from-orange-400 dark:to-orange-500 group-hover:w-6 group-hover:h-6 group-focus:w-6 group-focus:h-6 group-hover:shadow-lg group-hover:shadow-orange-300 dark:group-hover:shadow-orange-900 group-hover:scale-125 group-focus:scale-125"
                              : isCompleted
                              ? "w-2.5 h-2.5 bg-orange-500 group-hover:w-5 group-hover:h-5 group-focus:w-5 group-focus:h-5 group-hover:shadow-md group-hover:scale-125 group-focus:scale-125"
                              : "w-2.5 h-2.5 bg-gray-400 dark:bg-gray-600 group-hover:w-5 group-hover:h-5 group-focus:w-5 group-focus:h-5 group-hover:shadow-md group-hover:scale-125 group-focus:scale-125"
                          }`}
                        />
                      </div>
                      {/* Stage name on hover/focus - high z-index to appear above labels */}
                      <span className="opacity-0 group-hover:opacity-100 group-focus:opacity-100 transition-opacity duration-300 mt-3 text-xs leading-tight font-semibold text-slate-700 dark:text-slate-200 whitespace-nowrap absolute top-full pt-1 z-30 bg-white/90 dark:bg-slate-900/90 px-2 py-1 rounded shadow-sm">
                        {stageName}
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
            </div>
          )}

          {/* Résumé des statuts documents */}
          <div className="flex flex-wrap gap-2 text-xs">
            <Badge variant="secondary" className="bg-blue-50 text-blue-700 border-blue-100">
              {t("transaction.submitted", "Submitted")}: {submittedCount}
            </Badge>
            <Badge variant="secondary" className="bg-emerald-50 text-emerald-700 border-emerald-100">
              {t("transaction.approved", "Approved")}: {approvedCount}
            </Badge>
            <Badge variant="secondary" className="bg-amber-50 text-amber-700 border-amber-100">
              {t("transaction.needsRevision", "Needs revision")}: {revisionCount}
            </Badge>
            <Badge variant="secondary" className="bg-slate-50 text-slate-700 border-slate-100">
              {tTx("awaitingReview", "Awaiting review")}: {pendingCount}
            </Badge>
          </div>
        
          <Button
            variant="default"
            size="sm"
            className="w-full bg-orange-600 hover:bg-orange-700 text-white"
            onClick={() => {
              // Show rings panel (documents)
              if (expanded && panelType === "documents") {
                setExpanded(false);
              } else {
                setPanelType("documents");
                setExpanded(true);
              }
            }}
          >
            {tTx("viewDocumentDetails", "View document details")}
          </Button>

          <Button
            variant="outline"
            size="sm"
            className="w-full hover:bg-orange-500 hover:text-white transition-all duration-200 flex items-center justify-center gap-2"
            onClick={() => {
              // Show transaction details panel
              if (expanded && panelType === "transaction") {
                setExpanded(false);
              } else {
                setPanelType("transaction");
                setExpanded(true);
              }
            }}
          >
            {expanded && panelType === "transaction"
              ? t("transaction.hideDetails", "Hide details")
              : tTx("viewTransactionDetails", "View transaction details")}
            <ChevronRight className={`h-4 w-4 transition-transform ${expanded && panelType === "transaction" ? "translate-x-1" : ""}`} />
          </Button>

          {transaction.side === "SELL_SIDE" && (
            <Button
              variant="outline"
              size="sm"
              className="w-full hover:bg-green-500 hover:text-white transition-all duration-200 flex items-center justify-center gap-2"
              onClick={() => {
                // Show offers panel
                if (expanded && panelType === "offers") {
                  setExpanded(false);
                } else {
                  setPanelType("offers");
                  setExpanded(true);
                }
              }}
            >
              {expanded && panelType === "offers"
                ? tTx("hideOffers", "Hide offers")
                : tTx("viewOffers", "View offers")}
              <DollarSign className={`h-4 w-4 transition-transform ${expanded && panelType === "offers" ? "translate-x-1" : ""}`} />
            </Button>
          )}

          {transaction.side === "BUY_SIDE" && (
            <Button
              variant="outline"
              size="sm"
              className="w-full hover:bg-blue-500 hover:text-white transition-all duration-200 flex items-center justify-center gap-2"
              onClick={() => {
                // Show properties panel
                if (expanded && panelType === "properties") {
                  setExpanded(false);
                } else {
                  setPanelType("properties");
                  setExpanded(true);
                }
              }}
            >
              {expanded && panelType === "properties"
                ? tTx("hideProperties", "Hide properties")
                : tTx("viewProperties", "View properties")}
              <Building2 className={`h-4 w-4 transition-transform ${expanded && panelType === "properties" ? "translate-x-1" : ""}`} />
            </Button>
          )}
        </div>

        {expanded && (
          panelType === "documents" ? (
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-6 text-center lg:hidden">
              {[ringSubmitted, ringApproved, ringRevision].map((item) => (
                <div key={item.key} className="flex flex-col items-center gap-2">
                  <div className="relative w-24 h-24">
                    <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                      <circle cx="60" cy="60" r="50" fill="none" stroke={item.track} strokeWidth="18" />
                      <circle cx="60" cy="60" r="50" fill="none" stroke={item.color} strokeWidth="18" strokeDasharray={circumference} strokeDashoffset={ringOffset(item.percent)} strokeLinecap="round" className="transition-[stroke-dashoffset] duration-700 ease-out" />
                    </svg>
                    <div className="absolute inset-3 rounded-full bg-white dark:bg-slate-900 flex flex-col items-center justify-center text-xs font-semibold shadow-sm">
                      <span className="text-sm">{item.value}</span>
                    </div>
                  </div>
                  <div className="space-y-1">
                    <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">{item.label}</p>
                    <p className="text-xs text-muted-foreground">{item.legend}</p>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="mt-4 grid grid-cols-1 gap-4 text-sm lg:hidden">
              <div className="rounded-lg border p-3">
                <p className="font-semibold mb-2">{t("transaction.summary", "Transaction summary")}</p>
                <div className="grid grid-cols-2 gap-x-4 gap-y-1">
                  <span className="text-muted-foreground">{t("transaction.type", "Type")}</span>
                  <span>{sideInfo.label}</span>
                  <span className="text-muted-foreground">{t("transaction.opened", "Opened")}</span>
                  <span>{relativeTime ?? "--"}</span>
                  <span className="text-muted-foreground">{t("transaction.stage", "Stage")}</span>
                  <span>{stageValueDisplay}</span>
                  <span className="text-muted-foreground">{t("transaction.documents", "Documents")}</span>
                  <span>{documentCount}</span>
                </div>
              </div>
              <div className="rounded-lg border p-3 space-y-2">
                <p className="font-semibold text-sm flex items-center gap-2">{tTx("conditions.title", "Conditions")}</p>
                {conditions.length === 0 ? (
                  <p className="text-xs text-muted-foreground">{tTx("conditions.noConditions", "No conditions added.")}</p>
                ) : (
                  <div className="space-y-2">
                    {conditions.map((cond) => (
                      <div key={cond.conditionId} className="text-xs border border-slate-200 dark:border-slate-700 rounded p-2">
                        <div className="flex items-center justify-between mb-1">
                          <span className="font-semibold">{cond.customTitle || tTx(`conditionTypes.${cond.type}`, cond.type)}</span>
                          <Badge variant={cond.status === "SATISFIED" ? "default" : cond.status === "FAILED" ? "destructive" : "secondary"} className="text-[10px]">
                            {tTx(`conditionStatus.${cond.status}`, cond.status)}
                          </Badge>
                        </div>
                        <div className="text-muted-foreground">{cond.description}</div>
                        <div className="text-muted-foreground mt-1">{tTx("conditions.deadline", "Deadline")}: {cond.deadlineDate ? format(new Date(cond.deadlineDate), "PPP", { locale }) : "--"}</div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
              <Button variant="ghost" size="sm" onClick={() => onViewDetails?.(transaction.transactionId)}>
                {tTx("openFullDetails", "Open full details")}
              </Button>
            </div>
          )
        )}
      </CardContent>

      {expanded && (
        <div
          className="hidden lg:block absolute inset-y-0 left-[calc(100%+20px)] right-5 min-w-[500px]"
        >
          <div className="relative h-full flex flex-col">
            <div className="absolute -left-3 top-1/2 -translate-y-1/2 w-5 h-5 rotate-45 bg-white dark:bg-slate-900 border border-orange-200 dark:border-orange-900 shadow-sm" />
            
            {panelType === "documents" ? (
              <div className="absolute inset-0 bg-white dark:bg-slate-900 border border-orange-200 dark:border-orange-900 shadow-lg rounded-xl px-8 py-6 flex items-center justify-between gap-12">
                {/* Left: Needs revision */}
                <div className="flex flex-col items-center gap-3 translate-y-2">
                  <div className="relative w-24 h-24">
                    <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                      <circle cx="60" cy="60" r="50" fill="none" stroke={ringRevision.track} strokeWidth="18" />
                      <circle cx="60" cy="60" r="50" fill="none" stroke={ringRevision.color} strokeWidth="18" strokeDasharray={circumference} strokeDashoffset={animateRings ? ringOffset(ringRevision.percent) : circumference} strokeLinecap="round" className="transition-[stroke-dashoffset] duration-700 ease-out" />
                    </svg>
                    <div className="absolute inset-3 rounded-full bg-white dark:bg-slate-900 flex flex-col items-center justify-center text-xs font-semibold shadow-sm">
                      <span className="text-xs">{ringRevision.value}</span>
                    </div>
                  </div>
                  <div className="space-y-1 text-center">
                    <p className="text-base font-semibold text-slate-800 dark:text-slate-100">{ringRevision.label}</p>
                    <p className="text-sm text-muted-foreground">{ringRevision.legend}</p>
                  </div>
                </div>

                {/* Center: Submitted */}
                <div className="flex flex-col items-center gap-3 -translate-y-8">
                  <div className="relative w-32 h-32">
                    <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                      <circle cx="60" cy="60" r="50" fill="none" stroke={ringSubmitted.track} strokeWidth="18" />
                      <circle cx="60" cy="60" r="50" fill="none" stroke={ringSubmitted.color} strokeWidth="18" strokeDasharray={circumference} strokeDashoffset={animateRings ? ringOffset(ringSubmitted.percent) : circumference} strokeLinecap="round" className="transition-[stroke-dashoffset] duration-700 ease-out" />
                    </svg>
                    <div className="absolute inset-3 rounded-full bg-white dark:bg-slate-900 flex flex-col items-center justify-center text-xs font-semibold shadow-sm">
                      <span className="text-base">{ringSubmitted.value}</span>
                    </div>
                  </div>
                  <div className="space-y-1 text-center">
                    <p className="text-base font-semibold text-slate-800 dark:text-slate-100">{ringSubmitted.label}</p>
                    <p className="text-sm text-muted-foreground">{ringSubmitted.legend}</p>
                  </div>
                </div>

                {/* Right: Approved */}
                <div className="flex flex-col items-center gap-3 translate-y-2">
                  <div className="relative w-24 h-24">
                    <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                      <circle cx="60" cy="60" r="50" fill="none" stroke={ringApproved.track} strokeWidth="18" />
                      <circle cx="60" cy="60" r="50" fill="none" stroke={ringApproved.color} strokeWidth="18" strokeDasharray={circumference} strokeDashoffset={animateRings ? ringOffset(ringApproved.percent) : circumference} strokeLinecap="round" className="transition-[stroke-dashoffset] duration-700 ease-out" />
                    </svg>
                    <div className="absolute inset-3 rounded-full bg-white dark:bg-slate-900 flex flex-col items-center justify-center text-xs font-semibold shadow-sm">
                      <span className="text-xs">{ringApproved.value}</span>
                    </div>
                  </div>
                  <div className="space-y-1 text-center">
                    <p className="text-base font-semibold text-slate-800 dark:text-slate-100">{ringApproved.label}</p>
                    <p className="text-sm text-muted-foreground">{ringApproved.legend}</p>
                  </div>
                </div>
              </div>
            ) : panelType === "offers" ? (
              /* Offers Panel - Desktop */
              <div className="absolute inset-0 bg-white dark:bg-slate-900 border border-orange-200 dark:border-orange-900 shadow-lg rounded-xl px-8 py-6 overflow-y-auto">
                <div className="space-y-4">
                  <h4 className="font-semibold text-base text-slate-800 dark:text-slate-100 flex items-center gap-2 border-b border-slate-200 dark:border-slate-700 pb-2">
                    <DollarSign className="w-5 h-5 text-green-600" /> {tTx("offers", "Offers")} ({offers.length})
                  </h4>
                  {offers.length === 0 ? (
                    <div className="text-center py-12 text-muted-foreground bg-slate-50/50 dark:bg-slate-900/50 rounded-lg border border-dashed border-slate-300 dark:border-slate-700">
                      <DollarSign className="h-16 w-16 mx-auto mb-3 opacity-30" />
                      <p className="text-sm font-medium">{tTx("noOffers", "No offers received yet")}</p>
                      <p className="text-xs mt-1 text-muted-foreground/70">{tTx("noOffersDesc", "Offers from potential buyers will appear here")}</p>
                    </div>
                  ) : (
                    <div className={`space-y-3 ${offers.length > 2 ? "max-h-96 overflow-y-auto pr-2" : ""}`}>
                      {offers.map((offer) => (
                        <div 
                          key={offer.offerId} 
                          className="bg-gradient-to-br from-white to-slate-50/50 dark:from-slate-800 dark:to-slate-800/50 p-5 rounded-xl border-2 border-slate-200 dark:border-slate-700 hover:border-orange-300 dark:hover:border-orange-700 transition-all shadow-sm hover:shadow-md"
                        >
                          <div className="flex items-start justify-between mb-3">
                            <div className="flex items-center gap-2">
                              <div className="w-10 h-10 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                                <DollarSign className="w-5 h-5 text-green-600 dark:text-green-400" />
                              </div>
                              <div>
                                <p className="font-semibold text-base text-slate-900 dark:text-slate-100">{offer.buyerName}</p>
                                <p className="text-xs text-muted-foreground">
                                  {tTx("receivedOn", "Received on")} {new Date(offer.createdAt).toLocaleDateString(locale.code)}
                                </p>
                              </div>
                            </div>
                            <Badge variant={offerStatusVariantMap[offer.status]} className="text-xs">
                              {tTx(`offerStatus.${offer.status}`, offer.status)}
                            </Badge>
                          </div>
                          {offer.offerAmount && (
                            <div className="bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800 rounded-lg p-3 mb-3">
                              <p className="text-xs text-green-700 dark:text-green-300 mb-1">{tTx("offerAmount", "Offer Amount")}</p>
                              <p className="text-2xl font-bold text-green-600 dark:text-green-400">
                                ${offer.offerAmount.toLocaleString()}
                              </p>
                            </div>
                          )}
                          {offer.notes && (
                            <div className="bg-slate-100 dark:bg-slate-900/50 rounded-lg p-3 border border-slate-200 dark:border-slate-700">
                              <p className="text-xs text-muted-foreground mb-1">{tTx("notes", "Notes")}</p>
                              <p className="text-sm text-slate-700 dark:text-slate-300">{offer.notes}</p>
                            </div>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ) : panelType === "properties" ? (
              /* Properties Panel - Desktop */
              <div className="absolute inset-0 bg-white dark:bg-slate-900 border border-orange-200 dark:border-orange-900 shadow-lg rounded-xl px-8 py-6 overflow-y-auto">
                <div className="space-y-4">
                  <h4 className="font-semibold text-base text-slate-800 dark:text-slate-100 flex items-center gap-2 border-b border-slate-200 dark:border-slate-700 pb-2">
                    <Building2 className="w-5 h-5 text-blue-600" /> {tTx("properties", "Properties")} ({properties.length})
                  </h4>
                  {properties.length === 0 ? (
                    <div className="text-center py-12 text-muted-foreground bg-slate-50/50 dark:bg-slate-900/50 rounded-lg border border-dashed border-slate-300 dark:border-slate-700">
                      <Building2 className="h-16 w-16 mx-auto mb-3 opacity-30" />
                      <p className="text-sm font-medium">{tTx("noProperties", "No properties added yet")}</p>
                      <p className="text-xs mt-1 text-muted-foreground/70">{tTx("noPropertiesDesc", "Properties you are considering will appear here")}</p>
                    </div>
                  ) : (
                    <div className={`space-y-3 ${properties.length > 2 ? "max-h-96 overflow-y-auto pr-2" : ""}`}>
                      {properties.map((property) => (
                        <div 
                          key={property.propertyId} 
                          className="bg-gradient-to-br from-white to-slate-50/50 dark:from-slate-800 dark:to-slate-800/50 p-5 rounded-xl border-2 border-slate-200 dark:border-slate-700 hover:border-blue-300 dark:hover:border-blue-700 transition-all shadow-sm hover:shadow-md"
                        >
                          <div className="flex items-start justify-between mb-3">
                            <div className="flex items-center gap-2 flex-1 min-w-0">
                              <div className="w-10 h-10 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center flex-shrink-0">
                                <Building2 className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                              </div>
                              <div className="min-w-0 flex-1">
                                <p className="font-semibold text-base text-slate-900 dark:text-slate-100 truncate">{property.address?.street || "Unknown"}</p>
                                <p className="text-xs text-muted-foreground">
                                  {property.address?.city}, {property.address?.province} {property.address?.postalCode}
                                </p>
                              </div>
                            </div>
                            <Badge 
                              variant={propertyStatusConfig[property.offerStatus].variant} 
                              className={`text-xs flex-shrink-0 ${propertyStatusConfig[property.offerStatus].className}`}
                            >
                              {tTx(`propertyStatus.${property.offerStatus}`, property.offerStatus)}
                            </Badge>
                          </div>
                          {property.centrisNumber && (
                            <div className="text-xs text-muted-foreground mb-3">
                              <span className="font-medium">Centris: </span>{property.centrisNumber}
                            </div>
                          )}
                          <div className="grid grid-cols-2 gap-3">
                            {property.askingPrice && (
                              <div className="bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 rounded-lg p-3">
                                <p className="text-xs text-blue-700 dark:text-blue-300 mb-1">{tTx("askingPrice", "Asking Price")}</p>
                                <p className="font-bold text-blue-600 dark:text-blue-400">
                                  ${property.askingPrice.toLocaleString()}
                                </p>
                              </div>
                            )}
                            {property.offerAmount && (
                              <div className="bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800 rounded-lg p-3">
                                <p className="text-xs text-green-700 dark:text-green-300 mb-1">{tTx("offerAmount", "Offer Amount")}</p>
                                <p className="font-bold text-green-600 dark:text-green-400">
                                  ${property.offerAmount.toLocaleString()}
                                </p>
                              </div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            ) : (
              <div className="absolute inset-0 bg-white dark:bg-slate-900 border border-orange-200 dark:border-orange-900 shadow-lg rounded-xl p-6 space-y-6 flex flex-col overflow-y-auto">
                {/* Transaction Info Section */}
                <div className="border-l-4 border-l-orange-500 pl-4 w-full min-w-0">
                  <h4 className="font-semibold text-sm text-slate-800 dark:text-slate-100 mb-4 flex items-center gap-2">
                    <HomeIcon className="w-4 h-4 text-orange-600" /> {t("transaction.type", "Type")}
                  </h4>
                  <div className="grid grid-cols-2 gap-x-6 gap-y-3 text-sm">
                    <div>
                      <span className="text-muted-foreground text-xs">{t("transaction.type", "Type")}</span>
                      <p className="font-medium">{sideInfo.label}</p>
                    </div>
                    <div className="text-right">
                      <span className="text-muted-foreground text-xs block">{t("transaction.opened", "Opened")}</span>
                      <p className="font-medium">{relativeTime ?? "--"}</p>
                    </div>
                    <div>
                      <span className="text-muted-foreground text-xs">{t("transaction.stage", "Stage")}</span>
                      <p className="font-medium">{stageValueDisplay}</p>
                    </div>
                  </div>
                </div>

                {/* Transaction Details Section */}
                <div className="border-l-4 border-l-orange-500 pl-4">
                  <h4 className="font-semibold text-sm text-slate-800 dark:text-slate-100 mb-4 flex items-center gap-2">
                    <FileText className="w-4 h-4 text-orange-600" /> {t("transaction.details", "Details")}
                  </h4>
                  <div className="space-y-4 text-sm flex-grow">
                    <div className="bg-slate-50 dark:bg-slate-800 p-3 rounded border border-slate-200 dark:border-slate-700">
                      <span className="text-muted-foreground text-xs block mb-1">Transaction ID</span>
                      <p className="font-mono text-sm font-bold text-orange-700 dark:text-orange-400 break-all">{transaction.transactionId}</p>
                    </div>
                    <div className="grid grid-cols-2 gap-x-6 gap-y-3">
                      <div>
                        <span className="text-muted-foreground text-xs">{t("transaction.status", "Status")}</span>
                        <p className="font-medium">{transaction.status || "ACTIVE"}</p>
                      </div>
                      <div className="text-right">
                        <span className="text-muted-foreground text-xs block">{t("transaction.address", "Address")}</span>
                        <p className="font-medium text-sm leading-tight">{transaction.propertyAddress?.street || "--"}</p>
                      </div>
                    </div>
                  </div>
                </div>

                {/* Conditions Section */}
                <div className="border-l-4 border-l-orange-500 pl-4 w-full min-w-0">
                  <h4 className="font-semibold text-sm text-slate-800 dark:text-slate-100 mb-4 flex items-center gap-2">
                    <FileText className="w-4 h-4 text-orange-600" /> {tTx("conditions.title", "Conditions")}
                  </h4>
                  <div className="w-full min-w-0 max-h-80 overflow-y-auto pr-1 space-y-3 bg-slate-50 dark:bg-slate-900/50 border border-slate-200 dark:border-slate-800 rounded-lg p-3">
                    {conditions.length === 0 ? (
                      <p className="text-xs text-muted-foreground">{tTx("conditions.noConditions", "No conditions added.")}</p>
                    ) : (
                      conditions.map((cond) => {
                        const deadlineStatus = cond.status === 'PENDING'
                          ? getDeadlineStatus(cond.deadlineDate)
                          : 'normal';
                        const showOverdateBadge = cond.status === 'PENDING' && deadlineStatus === 'overdue';
                        
                        return (
                          <div key={cond.conditionId} className="text-sm space-y-1">
                            <div className="flex items-center justify-between gap-2">
                              <p className="font-semibold text-slate-800 dark:text-slate-100 flex-1 min-w-0">
                                {cond.customTitle || tTx(`conditionTypes.${cond.type}`, cond.type)}
                              </p>
                              <div className="flex items-center gap-1 flex-shrink-0">
                                {showOverdateBadge && (
                                  <Badge variant="destructive" className="text-xs flex items-center gap-1">
                                    <AlertTriangle className="w-3 h-3" />
                                    {tTx("overdue", "Overdue")}
                                  </Badge>
                                )}
                                <Badge variant={cond.status === "SATISFIED" ? "default" : cond.status === "FAILED" ? "destructive" : "secondary"} className="text-xs">
                                  {tTx(`conditionStatus.${cond.status}`, cond.status)}
                                </Badge>
                              </div>
                            </div>
                            <p className="text-xs text-muted-foreground">{cond.description}</p>
                            <div className="flex justify-between text-[11px] text-muted-foreground">
                              <span>{tTx("conditions.deadline", "Deadline")}: {cond.deadlineDate ? format(new Date(cond.deadlineDate), "PPP", { locale }) : "--"}</span>
                              {cond.satisfiedAt && (
                                <span>{tTx("conditionSatisfied", "Satisfied")}: {format(new Date(cond.satisfiedAt), "PPP", { locale })}</span>
                              )}
                            </div>
                          </div>
                        );
                      })
                    )}
                  </div>
                </div>
              </div>
            )}

          </div>
        </div>
      )}
    </Card>
  );
}
