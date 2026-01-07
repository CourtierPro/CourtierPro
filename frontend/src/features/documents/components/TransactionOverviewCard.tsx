import { ShoppingCart, Home as HomeIcon, ChevronRight, FileText, CheckCircle2, AlertCircle } from "lucide-react";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/shared/components/ui/popover";
import type { Transaction } from "@/features/transactions/api/queries";
import { formatDistanceToNow } from "date-fns";
import { enUS, fr } from "date-fns/locale";

interface TransactionOverviewCardProps {
  transaction: Transaction;
  documentCount?: number;
  approvedDocumentCount?: number;
  onViewDetails: (transactionId: string) => void;
}

export function TransactionOverviewCard({
  transaction,
  documentCount = 0,
  approvedDocumentCount = 0,
  onViewDetails,
}: TransactionOverviewCardProps) {
  const { t, i18n } = useTranslation("dashboard");
  const locale = i18n.language === "fr" ? fr : enUS;
  const [openRequired, setOpenRequired] = useState(false);
  const [openSubmitted, setOpenSubmitted] = useState(false);

  const needsRevisionCount = documentCount - approvedDocumentCount;

  const getStageColor = (stage: number) => {
    const stageMap: Record<number, string> = {
      1: "bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200",
      2: "bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200",
      3: "bg-purple-100 text-purple-800 dark:bg-purple-900 dark:text-purple-200",
      4: "bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-200",
      5: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200",
    };
    return stageMap[stage] || "bg-gray-100 text-gray-800 dark:bg-gray-900 dark:text-gray-200";
  };

  const getSideInfo = () => {
    if (transaction.side === "BUY_SIDE") {
      return {
        icon: <HomeIcon className="w-5 h-5" />,
        label: t("transaction.buying", "Buying"),
        color: "text-blue-600 dark:text-blue-400",
        bgColor: "bg-blue-50 dark:bg-blue-950/30",
      };
    }
    return {
      icon: <ShoppingCart className="w-5 h-5" />,
      label: t("transaction.selling", "Selling"),
      color: "text-amber-600 dark:text-amber-400",
      bgColor: "bg-amber-50 dark:bg-amber-950/30",
    };
  };

  const sideInfo = getSideInfo();

  const openedDate = transaction.openedDate ? new Date(transaction.openedDate) : null;
  const relativeTime = openedDate
    ? formatDistanceToNow(openedDate, { addSuffix: true, locale })
    : null;

  const cardStyle = "bg-gradient-to-br from-orange-50/50 via-white to-white dark:from-orange-950/20 dark:via-background dark:to-background border-l-4 border-l-orange-500";

  return (
    <Card className={`relative overflow-hidden hover:shadow-lg transition-all duration-300 ${cardStyle}`}>
      <CardHeader className="pb-3 pt-4 px-4 relative z-10">
        <div className="flex items-start justify-between gap-3">
          {/* Icône Buy/Sell avec label visible */}
          <div className="flex flex-col items-center gap-1">
            <div className={`p-2 rounded-lg ${sideInfo.bgColor}`}>
              <div className={sideInfo.color}>{sideInfo.icon}</div>
            </div>
            <span className="text-xs font-medium text-muted-foreground whitespace-nowrap">{sideInfo.label}</span>
          </div>
          
          <div className="flex-1">
            <CardTitle className="text-base font-semibold">
              {transaction.propertyAddress?.street || t("transaction.noAddress", "No address")}
            </CardTitle>
            {relativeTime && (
              <p className="text-xs text-muted-foreground mt-1">
                {t("transaction.opened", "Opened")} {relativeTime}
              </p>
            )}
          </div>
        </div>

        {/* Barre de progression avec étapes visuelles */}
        <div className="mt-4 space-y-2">
          <div className="flex justify-between items-center gap-2">
            <span className="text-xs font-medium text-muted-foreground">{t("transaction.progress", "Progress")}</span>
            <Badge className={getStageColor(transaction.currentStage)} variant="secondary">
              {t(`stages.${transaction.currentStage}`, `Stage ${transaction.currentStage}`)}
            </Badge>
          </div>
          
          {/* Dots pour les étapes - plus visible */}
          <div className="flex gap-1.5">
            {Array.from({ length: transaction.totalStages }).map((_, idx) => (
              <div
                key={idx}
                className={`h-3 flex-1 rounded-full transition-all duration-300 ${
                  idx < transaction.currentStage
                    ? "bg-gradient-to-r from-orange-500 to-orange-600"
                    : "bg-gray-300 dark:bg-gray-700"
                }`}
              />
            ))}
          </div>
          <p className="text-xs text-muted-foreground text-right">
            {transaction.currentStage}/{transaction.totalStages}
          </p>
        </div>
      </CardHeader>

      <CardContent className="space-y-3 px-4 pb-4 relative z-10">
        {/* Documents Popovers */}
        <div className="flex gap-2">
          {/* Documents Requis */}
          <Popover open={openRequired} onOpenChange={setOpenRequired}>
            <PopoverTrigger asChild>
              <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 hover:bg-blue-200 dark:hover:bg-blue-900/50 transition-colors cursor-pointer">
                <FileText className="w-3.5 h-3.5" />
                {documentCount} {t("transaction.documents", "docs")}
              </button>
            </PopoverTrigger>
            <PopoverContent className="w-64" align="start">
              <div className="space-y-3">
                <h4 className="font-semibold text-sm">{t("transaction.documentsRequired", "Documents Required")}</h4>
                <div className="space-y-2">
                  <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">Total documents needed:</span>
                    <span className="font-semibold text-blue-600">{documentCount}</span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {documentCount > 0 
                      ? t("transaction.documentsReqDesc", "These are the documents your broker has requested for this transaction.")
                      : t("transaction.noDocumentsReq", "No documents required for this transaction.")
                    }
                  </p>
                </div>
              </div>
            </PopoverContent>
          </Popover>

          {/* Documents Submitted */}
          {documentCount > 0 && (
            <Popover open={openSubmitted} onOpenChange={setOpenSubmitted}>
              <PopoverTrigger asChild>
                <button className="flex items-center gap-1.5 px-3 py-1.5 rounded-full text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300 hover:bg-green-200 dark:hover:bg-green-900/50 transition-colors cursor-pointer">
                  ✓ {approvedDocumentCount}/{documentCount}
                </button>
              </PopoverTrigger>
              <PopoverContent className="w-64" align="start">
                <div className="space-y-3">
                  <h4 className="font-semibold text-sm">{t("transaction.submissionStatus", "Submission Status")}</h4>
                  <div className="space-y-2">
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2 text-muted-foreground">
                        <CheckCircle2 className="w-4 h-4 text-green-600" />
                        <span>Approved:</span>
                      </div>
                      <span className="font-semibold text-green-600">{approvedDocumentCount}</span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2 text-muted-foreground">
                        <AlertCircle className="w-4 h-4 text-orange-600" />
                        <span>Needs Revision:</span>
                      </div>
                      <span className="font-semibold text-orange-600">{needsRevisionCount}</span>
                    </div>
                  </div>
                </div>
              </PopoverContent>
            </Popover>
          )}
        </div>
        
        <Button
          variant="outline"
          size="sm"
          onClick={() => onViewDetails(transaction.transactionId)}
          className="w-full hover:bg-orange-500 hover:text-white transition-all duration-200"
        >
          {t("transaction.viewDetails", "View Details")}
          <ChevronRight className="ml-2 h-4 w-4" />
        </Button>
      </CardContent>
    </Card>
  );
}
