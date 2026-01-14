import { ShoppingCart, Home as HomeIcon, ChevronRight, FileText } from "lucide-react";
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import type { Transaction } from "@/features/transactions/api/queries";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { getStageLabel, getStagesForSide, resolveStageIndex } from "@/shared/utils/stages";

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
  const DEBUG_TIMELINE = true;
  const { t, i18n } = useTranslation("dashboard");
  const { t: tTx } = useTranslation("transactions");
  const locale = i18n.language === "fr" ? fr : enUS;
  const [expanded, setExpanded] = useState(false);
  const [panelType, setPanelType] = useState<"documents" | "transaction">("documents");
  const [animateRings, setAnimateRings] = useState(false);

  // Debug logs for timeline visibility (development only)
  useEffect(() => {
    if (!DEBUG_TIMELINE) return;
    // eslint-disable-next-line no-console
    console.log("[Timeline] props", {
      totalStages: transaction.totalStages,
      currentStage: transaction.currentStage,
      side: transaction.side,
    });
  }, [DEBUG_TIMELINE, transaction.totalStages, transaction.currentStage, transaction.side]);

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
    transaction.currentStage as unknown as string | number | undefined,
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
    transaction.currentStage as unknown as string | number | undefined,
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
                    <div key={`dot-${idx}`} className="relative flex flex-col items-center group cursor-pointer z-10">
                      <div className="relative">
                        {/* Stage number badge on hover - better contrast */}
                        <div className="absolute -top-6 left-1/2 -translate-x-1/2 opacity-0 group-hover:opacity-100 transition-opacity duration-300 z-20">
                          <span className="text-xs font-bold text-white bg-orange-600 dark:bg-orange-500 px-2 py-1 rounded-full shadow-md">
                            {idx + 1}
                          </span>
                        </div>
                        {/* Dot */}
                        <div
                          className={`rounded-full transition-all duration-300 ${
                            isCurrent
                              ? "w-3 h-3 bg-gradient-to-br from-orange-500 to-orange-600 dark:from-orange-400 dark:to-orange-500 group-hover:w-6 group-hover:h-6 group-hover:shadow-lg group-hover:shadow-orange-300 dark:group-hover:shadow-orange-900 group-hover:scale-125"
                              : isCompleted
                              ? "w-2.5 h-2.5 bg-orange-500 group-hover:w-5 group-hover:h-5 group-hover:shadow-md group-hover:scale-125"
                              : "w-2.5 h-2.5 bg-gray-400 dark:bg-gray-600 group-hover:w-5 group-hover:h-5 group-hover:shadow-md group-hover:scale-125"
                          }`}
                        />
                      </div>
                      {/* Stage name on hover - high z-index to appear above labels */}
                      <span className="opacity-0 group-hover:opacity-100 transition-opacity duration-300 mt-3 text-xs leading-tight font-semibold text-slate-700 dark:text-slate-200 whitespace-nowrap absolute top-full pt-1 z-30 bg-white/90 dark:bg-slate-900/90 px-2 py-1 rounded shadow-sm">
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
              <Button variant="ghost" size="sm" onClick={() => onViewDetails?.(transaction.transactionId)}>
                {tTx("openFullDetails", "Open full details")}
              </Button>
            </div>
          )
        )}
      </CardContent>

      {expanded && (
        <div
          className="hidden lg:block absolute inset-y-0"
          style={{ left: "calc(100% + 20px)", right: "20px", minWidth: "500px" }}
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
            ) : (
              <div className="absolute inset-0 bg-white dark:bg-slate-900 border border-orange-200 dark:border-orange-900 shadow-lg rounded-xl p-6 space-y-6 flex flex-col overflow-y-auto">
                {/* Transaction Info Section */}
                <div className="border-l-4 border-l-orange-500 pl-4">
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
              </div>
            )}

          </div>
        </div>
      )}
    </Card>
  );
}
