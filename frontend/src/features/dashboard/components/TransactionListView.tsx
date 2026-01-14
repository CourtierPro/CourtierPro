import { ShoppingCart, Home as HomeIcon } from "lucide-react";
import { useTranslation } from "react-i18next";
import type { Transaction } from "@/features/transactions/api/queries";
import { formatDistanceToNow } from "date-fns";
import { enUS, fr } from "date-fns/locale";

interface TransactionListViewProps {
  transactions: Transaction[];
  selectedTransactionId?: string;
  onSelectTransaction: (id: string) => void;
  documentCounts: Record<string, number>;
  approvedDocumentCounts: Record<string, number>;
}

export function TransactionListView({
  transactions,
  selectedTransactionId,
  onSelectTransaction,
  documentCounts,
  approvedDocumentCounts,
}: TransactionListViewProps) {
  const { t, i18n } = useTranslation("dashboard");
  const locale = i18n.language === "fr" ? fr : enUS;

  const getSideInfo = (side: string) => {
    if (side === "BUY_SIDE") {
      return {
        icon: <HomeIcon className="w-4 h-4" />,
        label: t("transaction.buying"),
        color: "text-blue-600 dark:text-blue-400",
      };
    }
    return {
      icon: <ShoppingCart className="w-4 h-4" />,
      label: t("transaction.selling"),
      color: "text-amber-600 dark:text-amber-400",
    };
  };

  return (
    <div className="space-y-1 overflow-y-auto">
      {transactions.map((transaction) => {
        const sideInfo = getSideInfo(transaction.side);
        const isSelected = selectedTransactionId === transaction.transactionId;
        const openedDate = transaction.openedDate
          ? new Date(transaction.openedDate)
          : null;
        const relativeTime = openedDate
          ? formatDistanceToNow(openedDate, { addSuffix: true, locale })
          : null;
        const docCount = documentCounts[transaction.transactionId] || 0;
        const approvedCount = approvedDocumentCounts[transaction.transactionId] || 0;

        return (
          <button
            key={transaction.transactionId}
            onClick={() => onSelectTransaction(transaction.transactionId)}
            className={`w-full text-left p-3 rounded-lg transition-all duration-200 border-l-4 ${
              isSelected
                ? "bg-orange-50 dark:bg-orange-950/20 border-l-orange-500 shadow-md"
                : "bg-white dark:bg-slate-900 border-l-gray-300 dark:border-l-slate-700 hover:bg-gray-50 dark:hover:bg-slate-800"
            }`}
          >
            <div className="flex items-start justify-between gap-2 mb-2">
              <div className="flex items-center gap-2">
                <div className={sideInfo.color}>{sideInfo.icon}</div>
                <div className="text-xs font-medium text-muted-foreground">
                  {sideInfo.label}
                </div>
              </div>
              <div className="text-xs font-semibold text-blue-600 dark:text-blue-400">
                {t(`stages.${transaction.currentStage}`, `Stage ${transaction.currentStage}`)} / {transaction.totalStages}
              </div>
            </div>

            <h3 className="font-semibold text-sm mb-1 line-clamp-1">
              {transaction.propertyAddress?.street ||
                t("transaction.noAddress")}
            </h3>

            {relativeTime && (
              <p className="text-xs text-muted-foreground mb-2">
                {t("transaction.opened")} {relativeTime}
              </p>
            )}

            {docCount > 0 && (
              <div className="flex items-center gap-2 text-xs">
                <span className="text-muted-foreground">
                  📄 {docCount} {t("transaction.documents")}
                </span>
                <span className="text-green-600 dark:text-green-400">
                  ✓ {approvedCount}
                </span>
              </div>
            )}
          </button>
        );
      })}
    </div>
  );
}
