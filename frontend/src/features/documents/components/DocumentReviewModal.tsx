import type { DocumentRequest } from "@/features/documents/types";
import { useState } from "react";
import { toast } from "sonner";
import { useReviewDocument } from "@/features/documents/api/mutations";

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

  if (!open) return null;

  const handleApproveClick = () => {
    setAction('APPROVED');
    setShowCommentInput(true); // allow optional comment on approval
    setError(null);
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
        </div>
      </div>
    </div>
  );
}