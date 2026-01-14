import { ChevronRight, FileText, Home as HomeIcon, ShoppingCart } from "lucide-react";
import { useTranslation } from "react-i18next";
import { Card, CardContent, CardHeader, CardTitle } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import type { Transaction } from "@/features/transactions/api/queries";
import { formatDistanceToNow } from "date-fns";
import { enUS, fr } from "date-fns/locale";

interface TransactionDetailPanelProps {
  transaction: Transaction;
  documentCount?: number;
  approvedDocumentCount?: number;
  onViewFullDetails: (transactionId: string) => void;
}

export function TransactionDetailPanel({
  transaction,
  documentCount = 0,
  approvedDocumentCount = 0,
  onViewFullDetails,
}: TransactionDetailPanelProps) {
  const { t, i18n } = useTranslation("dashboard");
  const locale = i18n.language === "fr" ? fr : enUS;

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
        icon: <HomeIcon className="w-6 h-6" />,
        label: t("transaction.buying"),
        color: "text-blue-600 dark:text-blue-400",
        bgColor: "bg-blue-50 dark:bg-blue-950/30",
      };
    }
    return {
      icon: <ShoppingCart className="w-6 h-6" />,
      label: t("transaction.selling"),
      color: "text-amber-600 dark:text-amber-400",
      bgColor: "bg-amber-50 dark:bg-amber-950/30",
    };
  };

  const sideInfo = getSideInfo();

  const openedDate = transaction.openedDate
    ? new Date(transaction.openedDate)
    : null;
  const relativeTime = openedDate
    ? formatDistanceToNow(openedDate, { addSuffix: true, locale })
    : null;

  return (
    <div className="h-full flex flex-col overflow-hidden">
      <Card className="flex-1 overflow-auto border-0 shadow-none bg-transparent rounded-none">
        <CardHeader className="pb-4 border-b">
          <div className="flex items-start justify-between gap-3">
            <div className="flex flex-col items-start gap-3 flex-1">
              <div className="flex items-center gap-3">
                <div className={`p-3 rounded-lg ${sideInfo.bgColor}`}>
                  <div className={sideInfo.color}>{sideInfo.icon}</div>
                </div>
                <div>
                  <p className="text-sm font-medium text-muted-foreground">
                    {sideInfo.label}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    {t("transaction.opened")} {relativeTime || "N/A"}
                  </p>
                </div>
              </div>
              <CardTitle className="text-xl">
                {transaction.propertyAddress?.street ||
                  t("transaction.noAddress")}
              </CardTitle>
            </div>
          </div>
        </CardHeader>

        <CardContent className="pt-6 space-y-6">
          {/* Progress Section */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-sm font-semibold">
                {t("transaction.progress")}
              </h3>
              <Badge className={getStageColor(transaction.currentStage)}>
                {t(
                  `stages.${transaction.currentStage}`,
                  `Stage ${transaction.currentStage}`
                )}
              </Badge>
            </div>

            {/* Progress Dots */}
            <div className="flex gap-1.5">
              {Array.from({ length: transaction.totalStages }).map(
                (_, idx) => (
                  <div
                    key={idx}
                    className={`h-2 flex-1 rounded-full transition-all ${
                      idx < transaction.currentStage
                        ? "bg-gradient-to-r from-orange-500 to-orange-600"
                        : "bg-gray-300 dark:bg-gray-700"
                    }`}
                  />
                )
              )}
            </div>

            <p className="text-xs text-muted-foreground text-right">
              {transaction.currentStage}/{transaction.totalStages}
            </p>
          </div>

          {/* Documents Section */}
          {documentCount > 0 && (
            <div className="space-y-3 pt-4 border-t">
              <h3 className="text-sm font-semibold">
                {t("transaction.documents")}
              </h3>

              <div className="space-y-2">
                <div className="flex items-center justify-between p-2 bg-blue-50 dark:bg-blue-950/30 rounded">
                  <span className="text-sm text-muted-foreground flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    {t("transaction.documentsRequired")}
                  </span>
                  <span className="font-semibold text-blue-600">
                    {documentCount}
                  </span>
                </div>

                <div className="flex items-center justify-between p-2 bg-green-50 dark:bg-green-950/30 rounded">
                  <span className="text-sm text-muted-foreground">
                    ✓ {t("transaction.submissionStatus")}
                  </span>
                  <span className="font-semibold text-green-600">
                    {approvedDocumentCount}
                  </span>
                </div>

                {documentCount - approvedDocumentCount > 0 && (
                  <div className="flex items-center justify-between p-2 bg-orange-50 dark:bg-orange-950/30 rounded">
                    <span className="text-sm text-muted-foreground">
                      ⚠️ {t("transaction.needsRevision")}
                    </span>
                    <span className="font-semibold text-orange-600">
                      {documentCount - approvedDocumentCount}
                    </span>
                  </div>
                )}
              </div>
            </div>
          )}

          {/* CTA Button */}
          <Button
            onClick={() => onViewFullDetails(transaction.transactionId)}
            className="w-full bg-orange-500 hover:bg-orange-600 text-white mt-auto"
          >
            {t("transaction.viewDetails")}
            <ChevronRight className="ml-2 h-4 w-4" />
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
