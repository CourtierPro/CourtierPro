import { ShoppingCart, Home as HomeIcon, ChevronRight, FileText, DollarSign, Building2 } from "lucide-react";
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

type PanelType = "documents" | "transaction" | "offers" | "properties";

interface TransactionOverviewCardProps {
  transaction: Transaction;
  documentCount?: number;
  approvedDocumentCount?: number;
  needsRevisionCount?: number;
  submittedDocumentCount?: number;
  requestedDocumentCount?: number;
  onViewDetails?: (transactionId: string) => void;
  // Carousel integration props (controlled panel state)
  expandedPanel?: PanelType | null;
  onPanelChange?: (panel: PanelType | null) => void;
}

export function TransactionOverviewCard({
  transaction,
  documentCount = 0,
  approvedDocumentCount = 0,
  needsRevisionCount = 0,
  submittedDocumentCount = 0,
  requestedDocumentCount = 0,
  onViewDetails,
  expandedPanel: controlledExpandedPanel,
  onPanelChange,
}: TransactionOverviewCardProps) {
  const { t, i18n } = useTranslation("dashboard");
  const { t: tTx } = useTranslation("transactions");
  const locale = i18n.language === "fr" ? fr : enUS;

  // Support both controlled and uncontrolled panel state
  const [internalExpanded, setInternalExpanded] = useState(false);
  const [internalPanelType, setInternalPanelType] = useState<PanelType>("documents");

  const isControlled = onPanelChange !== undefined;
  const expanded = isControlled ? !!controlledExpandedPanel : internalExpanded;
  const panelType: PanelType = isControlled
    ? (controlledExpandedPanel || internalPanelType)
    : internalPanelType;

  const setExpanded = (value: boolean, newPanelType?: PanelType) => {
    if (isControlled) {
      onPanelChange?.(value ? (newPanelType || panelType) : null);
    } else {
      setInternalExpanded(value);
    }
  };

  const setPanelType = (type: PanelType) => {
    setInternalPanelType(type); // Always store internally for fallback
    if (isControlled) {
      onPanelChange?.(type);
    } else {
      setInternalExpanded(true);
    }
  };

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
      const sideKey = side === "SELL_SIDE" ? "sell" : "buy";
      const stages = getStagesForSide(side);
      const idx = Math.max(0, Math.min(stages.length - 1, val - 1));
      const stageEnum = stages[idx];
      if (stageEnum) {
        return t(`stages.${sideKey}.${stageEnum.toLowerCase()}.name`, { defaultValue: `Stage ${val}` });
      }
      return `Stage ${val}`;
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
  const ringAwaiting = {
    key: "awaiting",
    label: tTx("awaitingReview", "Awaiting review"),
    value: `${pendingCount}/${submittedCount || 0}`,
    percent: submittedCount ? Math.min(100, Math.max(0, (pendingCount / submittedCount) * 100)) : 0,
    color: "#64748b",
    track: "#e2e8f0",
    legend: tTx("awaitingReviewLegend", "Pending review"),
  };
  const ringRequested = {
    key: "requested",
    label: tTx("documentStatus.requested", "Requested"),
    value: `${requestedDocumentCount}`,
    percent: 100,
    color: "#ea580c",
    track: "#ffedd5",
    legend: tTx("requestedLegend", "Total requested"),
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
  }, [expanded, panelType, submittedPercent, approvedPercent, revisionPercent]);

  const cardStyle = "bg-gradient-to-br from-orange-50/50 via-white to-white dark:from-orange-950/20 dark:via-background dark:to-background border-l-4 border-l-orange-500";

  // Desktop: side-by-side layout when expanded
  const cardElement = (
    <Card className={`relative overflow-visible hover:shadow-lg transition-all duration-700 ease-in-out ${cardStyle} ${expanded ? "lg:flex-shrink-0 lg:w-[400px]" : ""}`}>
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
            <div className="relative px-2 py-4 mt-2 mb-2">
              {/* Progress indicator */}
              <div className="flex justify-between items-center mb-4">
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
                      className={`h-0.5 flex-1 transition-colors duration-300 ${idx < currentStageIndex
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
                            className={`rounded-full transition-all duration-300 ${isCurrent
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

          <Button
            variant="default"
            size="sm"
            className="w-full bg-orange-600 hover:bg-orange-700 text-white flex items-center justify-center gap-2 overflow-hidden hover:scale-100 active:scale-100"
            onClick={() => {
              // Toggle documents panel
              if (expanded && panelType === "documents") {
                setExpanded(false);
              } else {
                setPanelType("documents");
              }
            }}
          >
            {expanded && panelType === "documents"
              ? tTx("hideDocumentDetails", "Hide document details")
              : tTx("viewDocumentDetails", "View document details")}
            <ChevronRight className={`h-4 w-4 transition-transform duration-300 ${expanded && panelType === "documents" ? "rotate-90" : ""}`} />
          </Button>

          <Button
            variant="outline"
            size="sm"
            className="w-full hover:bg-orange-500 hover:text-white transition-colors duration-200 flex items-center justify-center gap-2 overflow-hidden hover:scale-100 active:scale-100"
            onClick={() => {
              // Toggle transaction details panel
              if (expanded && panelType === "transaction") {
                setExpanded(false);
              } else {
                setPanelType("transaction");
              }
            }}
          >
            {expanded && panelType === "transaction"
              ? t("transaction.hideDetails", "Hide details")
              : tTx("viewTransactionDetails", "View transaction details")}
            <ChevronRight className={`h-4 w-4 transition-transform duration-300 ${expanded && panelType === "transaction" ? "rotate-90" : ""}`} />
          </Button>

          {transaction.side === "SELL_SIDE" && (
            <Button
              variant="outline"
              size="sm"
              className="w-full hover:bg-green-500 hover:text-white transition-colors duration-200 flex items-center justify-center gap-2 overflow-hidden hover:scale-100 active:scale-100"
              onClick={() => {
                // Toggle offers panel
                if (expanded && panelType === "offers") {
                  setExpanded(false);
                } else {
                  setPanelType("offers");
                }
              }}
            >
              {expanded && panelType === "offers"
                ? tTx("hideOffers", "Hide offers")
                : tTx("viewOffers", "View offers")}
              <ChevronRight className={`h-4 w-4 transition-transform duration-300 ${expanded && panelType === "offers" ? "rotate-90" : ""}`} />
            </Button>
          )}

          {transaction.side === "BUY_SIDE" && (
            <Button
              variant="outline"
              size="sm"
              className="w-full hover:bg-blue-500 hover:text-white transition-colors duration-200 flex items-center justify-center gap-2 overflow-hidden hover:scale-100 active:scale-100"
              onClick={() => {
                // Toggle properties panel
                if (expanded && panelType === "properties") {
                  setExpanded(false);
                } else {
                  setPanelType("properties");
                }
              }}
            >
              {expanded && panelType === "properties"
                ? tTx("hideProperties", "Hide properties")
                : tTx("viewProperties", "View properties")}
              <ChevronRight className={`h-4 w-4 transition-transform duration-300 ${expanded && panelType === "properties" ? "rotate-90" : ""}`} />
            </Button>
          )}
        </div>

        {expanded && (
          panelType === "documents" ? (
            <div className="mt-4 grid grid-cols-1 sm:grid-cols-3 gap-6 text-center hidden sm:grid lg:!hidden">
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
            <div className="mt-4 grid grid-cols-1 gap-4 text-sm hidden sm:grid lg:!hidden">
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
    </Card>
  );

  // Desktop side-by-side panel (always rendered for smooth transitions)
  const desktopPanel = (
    <div className="border border-orange-200 dark:border-orange-900 bg-white dark:bg-slate-900 rounded-xl shadow-lg overflow-hidden h-full min-w-[300px]">
      {panelType === "documents" ? (
        <div className="px-8 py-8 flex flex-col justify-center h-full">
          {/* Top row: 3 rings */}
          <div className="flex items-start justify-around gap-6 mb-8">
            {[ringRequested, ringSubmitted, ringApproved].map((ring) => (
              <div key={ring.key} className="flex flex-col items-center gap-3">
                <div className="relative w-24 h-24">
                  <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                    <circle cx="60" cy="60" r="50" fill="none" stroke={ring.track} strokeWidth="14" />
                    <circle cx="60" cy="60" r="50" fill="none" stroke={ring.color} strokeWidth="14" strokeDasharray={circumference} strokeDashoffset={animateRings ? ringOffset(ring.percent) : circumference} strokeLinecap="round" className="transition-[stroke-dashoffset] duration-700 ease-out" />
                  </svg>
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="text-base font-semibold">{ring.value}</span>
                  </div>
                </div>
                <div className="space-y-1 text-center">
                  <p className="text-sm font-semibold text-slate-800 dark:text-slate-100">{ring.label}</p>
                  <p className="text-xs text-muted-foreground">{ring.legend}</p>
                </div>
              </div>
            ))}
          </div>
          {/* Bottom row: 2 rings */}
          <div className="flex items-start justify-center gap-16">
            {[ringAwaiting, ringRevision].map((ring) => (
              <div key={ring.key} className="flex flex-col items-center gap-3">
                <div className="relative w-20 h-20">
                  <svg viewBox="0 0 120 120" className="w-full h-full -rotate-90">
                    <circle cx="60" cy="60" r="50" fill="none" stroke={ring.track} strokeWidth="14" />
                    <circle cx="60" cy="60" r="50" fill="none" stroke={ring.color} strokeWidth="14" strokeDasharray={circumference} strokeDashoffset={animateRings ? ringOffset(ring.percent) : circumference} strokeLinecap="round" className="transition-[stroke-dashoffset] duration-700 ease-out" />
                  </svg>
                  <div className="absolute inset-0 flex items-center justify-center">
                    <span className="text-sm font-semibold">{ring.value}</span>
                  </div>
                </div>
                <div className="space-y-1 text-center">
                  <p className="text-sm font-medium text-slate-700 dark:text-slate-200">{ring.label}</p>
                  <p className="text-xs text-muted-foreground">{ring.legend}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      ) : panelType === "offers" ? (
        <div className="px-4 py-4 flex flex-col max-h-96 overflow-hidden">
          <div className="flex-1 space-y-3 overflow-y-auto">
            <h4 className="font-semibold text-sm text-slate-800 dark:text-slate-100 flex items-center gap-2 border-b border-slate-200 dark:border-slate-700 pb-2 sticky top-0 bg-white dark:bg-slate-900">
              <DollarSign className="w-4 h-4 text-green-600" /> {tTx("offers", "Offers")} ({offers.length})
            </h4>
            {offers.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground bg-slate-50/50 dark:bg-slate-900/50 rounded-lg border border-dashed border-slate-300 dark:border-slate-700">
                <DollarSign className="h-10 w-10 mx-auto mb-2 opacity-30" />
                <p className="text-xs font-medium">{tTx("noOffers", "No offers received yet")}</p>
              </div>
            ) : (
              <div className="space-y-2">
                {offers.map((offer) => (
                  <div
                    key={offer.offerId}
                    className="bg-gradient-to-br from-white to-slate-50/50 dark:from-slate-800 dark:to-slate-800/50 p-3 rounded-lg border border-slate-200 dark:border-slate-700"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center">
                          <DollarSign className="w-3 h-3 text-green-600 dark:text-green-400" />
                        </div>
                        <div>
                          <p className="font-medium text-xs">{offer.buyerName}</p>
                          <p className="text-xs text-muted-foreground">{new Date(offer.createdAt).toLocaleDateString(locale.code)}</p>
                        </div>
                      </div>
                      <Badge variant={offerStatusVariantMap[offer.status]} className="text-xs">
                        {tTx(`receivedOfferStatuses.${offer.status}`, offer.status)}
                      </Badge>
                    </div>
                    {offer.offerAmount && (
                      <div className="bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800 rounded p-1.5 text-center">
                        <p className="text-base font-bold text-green-600 dark:text-green-400">${offer.offerAmount.toLocaleString()}</p>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      ) : panelType === "properties" ? (
        <div className="px-4 py-4 flex flex-col max-h-96 overflow-hidden">
          <div className="flex-1 space-y-3 overflow-y-auto">
            <h4 className="font-semibold text-sm text-slate-800 dark:text-slate-100 flex items-center gap-2 border-b border-slate-200 dark:border-slate-700 pb-2 sticky top-0 bg-white dark:bg-slate-900">
              <Building2 className="w-4 h-4 text-blue-600" /> {tTx("properties", "Properties")} ({properties.length})
            </h4>
            {properties.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground bg-slate-50/50 dark:bg-slate-900/50 rounded-lg border border-dashed border-slate-300 dark:border-slate-700">
                <Building2 className="h-10 w-10 mx-auto mb-2 opacity-30" />
                <p className="text-xs font-medium">{tTx("noProperties", "No properties added yet")}</p>
              </div>
            ) : (
              <div className="space-y-2">
                {properties.map((property) => (
                  <div
                    key={property.propertyId}
                    className="bg-gradient-to-br from-white to-slate-50/50 dark:from-slate-800 dark:to-slate-800/50 p-3 rounded-lg border border-slate-200 dark:border-slate-700"
                  >
                    <div className="flex items-start justify-between mb-1">
                      <p className="font-medium text-xs truncate flex-1">{property.address?.street || "Unknown"}</p>
                      <Badge
                        variant={propertyStatusConfig[property.offerStatus].variant}
                        className={`text-xs ${propertyStatusConfig[property.offerStatus].className}`}
                      >
                        {tTx(`propertyStatus.${property.offerStatus}`, property.offerStatus)}
                      </Badge>
                    </div>
                    <p className="text-xs text-muted-foreground mb-2">{property.address?.city}, {property.address?.province}</p>
                    <div className="flex gap-2">
                      {property.askingPrice && (
                        <div className="bg-blue-50 dark:bg-blue-950/30 rounded p-1.5 flex-1 text-center">
                          <p className="text-xs text-blue-600 font-bold">${property.askingPrice.toLocaleString()}</p>
                        </div>
                      )}
                      {property.offerAmount && (
                        <div className="bg-green-50 dark:bg-green-950/30 rounded p-1.5 flex-1 text-center">
                          <p className="text-xs text-green-600 font-bold">${property.offerAmount.toLocaleString()}</p>
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
        <div className="p-6 h-full flex flex-col justify-center">
          <div className="border-l-4 border-l-orange-500 pl-4 mb-6">
            <h4 className="font-semibold text-base mb-4 flex items-center gap-2">
              <HomeIcon className="w-5 h-5 text-orange-600" /> {t("transaction.summary", "Summary")}
            </h4>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <span className="text-muted-foreground text-sm">{t("transaction.type", "Type")}</span>
                <p className="font-semibold text-base">{sideInfo.label}</p>
              </div>
              <div>
                <span className="text-muted-foreground text-sm">{t("transaction.stage", "Stage")}</span>
                <p className="font-semibold text-base">{stageValueDisplay}</p>
              </div>
              <div>
                <span className="text-muted-foreground text-sm">{t("transaction.opened", "Opened")}</span>
                <p className="font-semibold text-base">{relativeTime ?? "--"}</p>
              </div>
              <div>
                <span className="text-muted-foreground text-sm">{t("transaction.status", "Status")}</span>
                <p className="font-semibold text-base">{tTx(transaction.status || "ACTIVE", transaction.status || "Active")}</p>
              </div>
            </div>
          </div>
          <div className="border-l-4 border-l-orange-500 pl-4">
            <h4 className="font-semibold text-base mb-4 flex items-center gap-2">
              <FileText className="w-5 h-5 text-orange-600" /> {tTx("conditions.title", "Conditions")}
            </h4>
            <div className="space-y-3">
              {conditions.length === 0 ? (
                <p className="text-sm text-muted-foreground">{tTx("conditions.noConditions", "No conditions added.")}</p>
              ) : (
                conditions.slice(0, 4).map((cond) => (
                  <div key={cond.conditionId} className="flex items-center justify-between gap-2 py-2 border-b border-slate-100 dark:border-slate-800">
                    <span className="text-sm font-medium truncate">{cond.customTitle || tTx(`conditionTypes.${cond.type}`, cond.type)}</span>
                    <Badge variant={cond.status === "SATISFIED" ? "default" : cond.status === "FAILED" ? "destructive" : "secondary"} className="text-xs">
                      {tTx(`conditionStatus.${cond.status}`, cond.status)}
                    </Badge>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );

  // Return flex layout - always use flex on desktop for smooth transitions
  return (
    <div className="lg:flex lg:gap-4 lg:items-start">
      <div className={`transition-all duration-700 ease-in-out lg:self-start ${expanded ? "lg:w-[400px] lg:flex-shrink-0" : "lg:w-full"}`}>
        {cardElement}
      </div>
      <div
        className={`hidden lg:block transition-all duration-700 ease-in-out overflow-hidden ${expanded
          ? "lg:flex-1 lg:opacity-100 lg:translate-x-0"
          : "lg:w-0 lg:opacity-0 lg:translate-x-full lg:pointer-events-none"
          }`}
        style={{ maxHeight: expanded ? '100%' : undefined }}
      >
        {desktopPanel}
      </div>
    </div>
  );
}
