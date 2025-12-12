<<<<<<< HEAD
import type { DocumentRequest } from "@/features/documents/types";
import { useState } from "react";
import { toast } from "sonner";
import { useReviewDocument } from "@/features/documents/api/mutations";
=======
import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";
import { Section } from "@/shared/components/branded/Section";
import { AttributeRow } from "@/shared/components/branded/AttributeRow";
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { X, Download, CheckCircle, XCircle, FileText } from "lucide-react";
import type { DocumentRequest, SubmittedDocument } from "@/features/documents/types";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { Dialog, DialogContent, DialogTitle } from "@/shared/components/ui/dialog";
>>>>>>> origin/main

interface DocumentReviewModalProps {
  open: boolean;
  onClose: () => void;
  document?: DocumentRequest;
  transactionId: string;
  onSuccess?: () => void;
}

export function DocumentReviewModal({
  open,
  onClose,
  document,
  transactionId,
  onSuccess,
}: DocumentReviewModalProps) {
  const [comment, setComment] = useState<string>('');
  const [action, setAction] = useState<'APPROVED' | 'NEEDS_REVISION' | null>(null);
  const [showCommentInput, setShowCommentInput] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const reviewMutation = useReviewDocument();

<<<<<<< HEAD
  if (!open) return null;

  const handleApproveClick = () => {
    setAction('APPROVED');
    setShowCommentInput(true); // allow optional comment on approval
    setError(null);
=======
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
>>>>>>> origin/main
  };

  const handleNeedsRevisionClick = () => {
    setAction('NEEDS_REVISION');
    setShowCommentInput(true);
    setError(null);
  };

  const handleSubmit = async () => {
    if (action != null && document) {
      try {
        await reviewMutation.mutateAsync({
          transactionId,
          requestId: document.requestId,
          decision: action,
          comments: comment || undefined,
        });
        const successMessage =
          action === "APPROVED"
            ? "Document approved"
            : "Revision requested";
        toast.success(successMessage);
        onSuccess?.();
        onClose();
      } catch {
        setError('Error submitting review. Please try again.');
        toast.error('Error submitting review. Please try again.');
      }
    } else {
      setError('Please select an action before submitting.');
    }
  };

  return (
<<<<<<< HEAD
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 animate-fade-in">
      <div 
        className="absolute inset-0 bg-black/50 backdrop-blur-sm transition-opacity" 
        onClick={onClose} 
        aria-hidden="true"
      />
      <div 
        role="dialog" 
        aria-modal="true" 
        aria-labelledby="review-modal-title"
        className="relative w-full max-w-md bg-card dark:bg-card rounded-lg shadow-xl border border-border animate-slide-in-right"
      >
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <h2 id="review-modal-title" className="text-lg font-semibold text-foreground">
            Review Document
          </h2>
          <button 
            onClick={onClose} 
            aria-label="Close modal"
            className="inline-flex h-8 w-8 items-center justify-center rounded-md text-muted-foreground hover:bg-accent hover:text-accent-foreground transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
          </button>
=======
    <Dialog open={open} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="sm:max-w-lg p-0 gap-0 [&>button]:hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div className="flex items-center gap-3">
            <FileText className="w-6 h-6 text-primary" />
            <DialogTitle className="text-xl font-semibold text-foreground">{title}</DialogTitle>
          </div>
          <Button variant="ghost" size="icon" onClick={onClose}>
            <X className="w-5 h-5" />
          </Button>
>>>>>>> origin/main
        </div>

        {/* Body */}
        <div className="px-6 py-5 space-y-5">
          {/* Document Info */}
          {document && (
            <div className="rounded-md bg-muted/50 p-4 space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground font-medium">Document Type</span>
                <span className="font-semibold text-foreground">{document.docType}</span>
              </div>
              {document.customTitle && (
                <div className="flex items-center justify-between text-sm">
                  <span className="text-muted-foreground font-medium">Title</span>
                  <span className="font-medium text-foreground">{document.customTitle}</span>
                </div>
              )}
            </div>
          )}

<<<<<<< HEAD
          {/* Error Alert */}
          {error && (
            <div className="flex items-start gap-3 rounded-md border border-destructive/50 bg-destructive/10 p-3 text-sm text-destructive">
              <svg className="h-5 w-5 flex-shrink-0 mt-0.5" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <circle cx="12" cy="12" r="10"></circle>
                <line x1="12" y1="8" x2="12" y2="12"></line>
                <line x1="12" y1="16" x2="12.01" y2="16"></line>
              </svg>
              <span>{error}</span>
            </div>
          )}

          {/* Action Buttons */}
          {action === null && (
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground mb-3">Choose an action:</p>
              <div className="grid gap-3">
                <button 
                  onClick={handleApproveClick} 
                  className="flex items-center justify-center gap-2 rounded-md bg-green-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors hover:bg-green-700 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-green-600 focus-visible:ring-offset-2"
                >
                  <svg className="h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                  Approve
                </button>
                <button 
                  onClick={handleNeedsRevisionClick} 
                  className="flex items-center justify-center gap-2 rounded-md bg-amber-500 px-4 py-2.5 text-sm font-medium text-white shadow-sm transition-colors hover:bg-amber-600 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-amber-500 focus-visible:ring-offset-2"
                >
                  <svg className="h-4 w-4" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                    <path d="M12 22h6a2 2 0 0 0 2-2V7.5L14.5 2H6a2 2 0 0 0-2 2v3"></path>
                    <path d="M18 14h-8"></path>
                    <path d="M15 18H6.5a2.5 2.5 0 0 1 0-5H15"></path>
                  </svg>
                  Needs Revision
                </button>
              </div>
            </div>
          )}

          {/* Comment Input */}
          {action !== null && showCommentInput && (
            <div className="space-y-2">
              <label htmlFor="review-notes" className="block text-sm font-medium text-foreground">
                {action === 'APPROVED' ? 'Approval Notes (Optional)' : 'Revision Notes'}
                {action === 'NEEDS_REVISION' && <span className="text-destructive ml-1">*</span>}
              </label>
              <textarea
                id="review-notes"
                value={comment}
                onChange={(e) => setComment(e.target.value)}
                placeholder={action === 'APPROVED' ? 'Add any additional comments...' : 'Explain what needs to be revised...'}
                className="w-full min-h-[100px] rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground placeholder:text-muted-foreground transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-50"
                rows={4}
                required={action === 'NEEDS_REVISION'}
              />
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-border bg-muted/30">
          <button 
            onClick={onClose} 
            className="inline-flex items-center justify-center rounded-md px-4 py-2 text-sm font-medium text-foreground bg-secondary hover:bg-secondary/80 transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
          >
            Cancel
          </button>
          {action !== null && (
            <button 
              onClick={handleSubmit} 
              disabled={reviewMutation.isPending || (action === 'NEEDS_REVISION' && !comment.trim())}
              className="inline-flex items-center justify-center gap-2 rounded-md bg-primary px-4 py-2 text-sm font-medium text-primary-foreground shadow-sm transition-colors hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50"
            >
              {reviewMutation.isPending && (
                <svg className="h-4 w-4 animate-spin" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
              )}
              {reviewMutation.isPending ? 'Submitting...' : 'Submit Review'}
            </button>
          )}
=======
              {/* Actions */}
              {document.status === "SUBMITTED" && (onApprove || onReject) && (
                <div className="flex justify-end gap-3 pt-4 border-t border-border">
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

        {/* Footer - already handled above in Actions, or general footer? 
            Original had a separate footer for "Close" if not approved/rejected, OR standard footer.
            Original Code:
            {document.status === "SUBMITTED" ... Actions ...}
            <div className="flex justify-end p-6 border-t border-border"> ... Close ... </div>
            This means TWO footers?
            Let me check original code.
            Step 574:
            It has `Actions` div inside `p-6 space-y-6`.
            AND a separate Footer div at the bottom.
            If I preserve this, I should keep both.
        */}
        <div className="flex justify-end p-6 border-t border-border transition-colors">
          <Button variant="ghost" onClick={onClose}>
            {t("actions.close")}
          </Button>
>>>>>>> origin/main
        </div>
      </DialogContent>
    </Dialog>
  );
}