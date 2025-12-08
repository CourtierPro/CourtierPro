import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";
import { Section } from "@/shared/components/branded/Section";
import { AttributeRow } from "@/shared/components/branded/AttributeRow";
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { X, Download, CheckCircle, XCircle, FileText } from "lucide-react";
import type { DocumentRequest, SubmittedDocument } from "@/features/documents/types";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";

interface DocumentReviewModalProps {
  open: boolean;
  onClose: () => void;
  document?: DocumentRequest;
  onApprove?: (requestId: string) => void;
  onReject?: (requestId: string) => void;
}

export function DocumentReviewModal({
  open,
  onClose,
  document,
  onApprove,
  onReject,
}: DocumentReviewModalProps) {
  const { t, i18n } = useTranslation("documents");
  const locale = i18n.language === "fr" ? fr : enUS;

  if (!open) return null;

  const title = document?.customTitle || (document?.docType ? t(`types.${document.docType}`) : t("modals.reviewDocument"));
  const latestSubmission: SubmittedDocument | undefined = document?.submittedDocuments?.[document.submittedDocuments.length - 1];
  const submittedDate = latestSubmission?.uploadedAt
    ? format(new Date(latestSubmission.uploadedAt), "PPP", { locale })
    : null;

  const handleApprove = () => {
    if (document?.requestId && onApprove) {
      onApprove(document.requestId);
    }
    onClose();
  };

  const handleReject = () => {
    if (document?.requestId && onReject) {
      onReject(document.requestId);
    }
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-lg rounded-lg bg-white shadow-lg">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <FileText className="w-6 h-6 text-primary" />
            <h2 className="text-xl font-semibold text-foreground">{title}</h2>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="w-5 h-5" />
          </Button>
        </div>

        {/* Content */}
        <div className="p-6 space-y-6">
          {document ? (
            <>
              {/* Status */}
              <div className="flex items-center gap-3">
                <span className="text-sm text-muted-foreground">{t("status.label")}:</span>
                <StatusBadge status={document.status} />
              </div>

              {/* Document Metadata */}
              <Section className="p-4">
                <div className="space-y-0">
                  <AttributeRow label={t("documentType")} value={t(`types.${document.docType}`)} />
                  <AttributeRow label={t("expectedFrom")} value={t(`parties.${document.expectedFrom}`)} />
                  {document.brokerNotes && (
                    <AttributeRow label={t("brokerNotes")} value={document.brokerNotes} />
                  )}
                  {submittedDate && (
                    <AttributeRow label={t("submittedAt")} value={submittedDate} />
                  )}
                  {latestSubmission && (
                    <AttributeRow
                      label={t("fileName")}
                      value={latestSubmission.storageObject?.fileName || "-"}
                    />
                  )}
                </div>
              </Section>

              {/* Download Link */}
              {latestSubmission && (
                <div className="flex justify-center">
                  <Button variant="outline" className="gap-2">
                    <Download className="w-4 h-4" />
                    {t("downloadDocument")}
                  </Button>
                </div>
              )}

              {/* Actions */}
              {document.status === "SUBMITTED" && (onApprove || onReject) && (
                <div className="flex justify-end gap-3 pt-4 border-t border-gray-200">
                  {onReject && (
                    <Button variant="outline" onClick={handleReject} className="gap-2 text-destructive">
                      <XCircle className="w-4 h-4" />
                      {t("actions.reject")}
                    </Button>
                  )}
                  {onApprove && (
                    <Button onClick={handleApprove} className="gap-2">
                      <CheckCircle className="w-4 h-4" />
                      {t("actions.approve")}
                    </Button>
                  )}
                </div>
              )}
            </>
          ) : (
            <p className="text-muted-foreground text-center py-8">
              {t("noDocumentSelected")}
            </p>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end p-6 border-t border-gray-200">
          <Button variant="ghost" onClick={onClose}>
            {t("actions.close")}
          </Button>
        </div>
      </div>
    </div>
  );
}

