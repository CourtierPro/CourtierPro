import type { DocumentRequest } from "@/features/documents/types";
import { useState } from "react";
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

  const handleApproveClick = () => {
    setAction('APPROVED');
    setShowCommentInput(false);
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
        onSuccess?.();
        onClose();
      } catch (err) {
        setError('Error submitting review. Please try again.');
      }
    } else {
      setError('Please select an action before submitting.');
    }
  };

  return (
    <>
      {open && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white dark:bg-gray-800 rounded-lg shadow-lg p-6 w-full max-w-md">
            {/* Header */}
            <div className="flex justify-between items-center mb-6">
              <h2 className="text-xl font-bold text-gray-900 dark:text-white">Review Document</h2>
              <button
                onClick={onClose}
                className="text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
              >
                ✕
              </button>
            </div>

            {/* Document Info */}
            {document && (
              <div className="mb-6 p-4 bg-gray-50 dark:bg-gray-700 rounded">
                <p className="text-sm text-gray-600 dark:text-gray-400">
                  <span className="font-semibold">Type:</span> {document.docType}
                </p>
                <p className="text-sm text-gray-600 dark:text-gray-400 mt-2">
                  <span className="font-semibold">Current Status:</span> {document.status}
                </p>
                {document.customTitle && (
                  <p className="text-sm text-gray-600 dark:text-gray-400 mt-2">
                    <span className="font-semibold">Title:</span> {document.customTitle}
                  </p>
                )}
              </div>
            )}

            {/* Error Message */}
            {error && (
              <div className="mb-4 p-3 bg-red-100 dark:bg-red-900 text-red-800 dark:text-red-200 rounded text-sm">
                {error}
              </div>
            )}

            {/* Action Buttons (show when no action selected) */}
            {action === null && (
              <div className="flex gap-3 mb-6">
                <button
                  onClick={handleApproveClick}
                  className="flex-1 bg-green-500 hover:bg-green-600 text-white font-semibold py-2 rounded transition"
                >
                  Approve
                </button>
                <button
                  onClick={handleNeedsRevisionClick}
                  className="flex-1 bg-yellow-500 hover:bg-yellow-600 text-white font-semibold py-2 rounded transition"
                >
                  Needs Revision
                </button>
              </div>
            )}

            {/* Action Selected - show comment input */}
            {action !== null && showCommentInput && (
              <div className="mb-6">
                <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-2">
                  {action === 'APPROVED' ? 'Approval Notes (Optional)' : 'Revision Notes (Required)'}
                </label>
                <textarea
                  value={comment}
                  onChange={(e) => setComment(e.target.value)}
                  placeholder="Add your comments here..."
                  className="w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-400 dark:placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  rows={4}
                />
              </div>
            )}

            {/* Footer Buttons */}
            <div className="flex gap-3">
              <button
                onClick={onClose}
                className="flex-1 bg-gray-300 dark:bg-gray-600 hover:bg-gray-400 dark:hover:bg-gray-700 text-gray-900 dark:text-white font-semibold py-2 rounded transition"
              >
                Cancel
              </button>
              {action !== null && (
                <button
                  onClick={handleSubmit}
                  disabled={reviewMutation.isPending}
                  className="flex-1 bg-blue-500 hover:bg-blue-600 disabled:bg-blue-300 text-white font-semibold py-2 rounded transition"
                >
                  {reviewMutation.isPending ? 'Submitting...' : 'Submit'}
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  );
}