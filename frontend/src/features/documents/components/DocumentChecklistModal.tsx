import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { useDocuments } from "@/features/documents/api/queries";
import { X, FileText, CheckCircle, Clock, AlertCircle } from "lucide-react";
import type { DocumentStatusEnum } from "@/features/documents/types";

interface DocumentChecklistModalProps {
  open: boolean;
  onClose: () => void;
  transactionId?: string;
}

export function DocumentChecklistModal({
  open,
  onClose,
  transactionId,
}: DocumentChecklistModalProps) {
  const { t } = useTranslation("documents");
  const { data: documents = [], isLoading } = useDocuments(transactionId || "");

  if (!open) return null;

  const getStatusIcon = (status: DocumentStatusEnum) => {
    switch (status) {
      case "APPROVED":
        return <CheckCircle className="w-5 h-5 text-green-500 dark:text-green-400" />;
      case "SUBMITTED":
        return <Clock className="w-5 h-5 text-blue-500 dark:text-blue-400" />;
      case "NEEDS_REVISION":
        return <AlertCircle className="w-5 h-5 text-destructive" />;
      default:
        return <FileText className="w-5 h-5 text-muted-foreground" />;
    }
  };

  const completedCount = documents.filter(d => d.status === "APPROVED").length;
  const totalCount = documents.length;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div className="w-full max-w-lg rounded-lg bg-card shadow-lg max-h-[80vh] flex flex-col border border-border">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div>
            <h2 className="text-xl font-semibold text-foreground">{t("modals.documentChecklist")}</h2>
            {totalCount > 0 && (
              <p className="text-sm text-muted-foreground mt-1">
                {t("checklistProgress", { completed: completedCount, total: totalCount })}
              </p>
            )}
          </div>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="w-5 h-5" />
          </Button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {!transactionId ? (
            <EmptyState
              icon={<FileText className="w-12 h-12" />}
              title={t("noTransactionSelected")}
              description={t("selectTransactionToViewChecklist")}
            />
          ) : isLoading ? (
            <LoadingState />
          ) : documents.length === 0 ? (
            <EmptyState
              icon={<FileText className="w-12 h-12" />}
              title={t("noDocumentsRequired")}
              description={t("noDocumentsDescription")}
            />
          ) : (
            <ul className="space-y-3">
              {documents.map((doc) => {
                const title = doc.customTitle || t(`types.${doc.docType}`);
                return (
                  <li
                    key={doc.requestId}
                    className="flex items-center gap-3 p-3 rounded-lg border border-border bg-card hover:bg-muted/50 transition-colors"
                  >
                    {getStatusIcon(doc.status)}
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium text-foreground truncate">{title}</p>
                      <p className="text-xs text-muted-foreground">
                        {t(`parties.${doc.expectedFrom}`)}
                      </p>
                    </div>
                    <StatusBadge status={doc.status} />
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end p-6 border-t border-border">
          <Button variant="ghost" onClick={onClose}>
            {t("actions.close")}
          </Button>
        </div>
      </div>
    </div>
  );
}

