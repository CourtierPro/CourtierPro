import { useParams, useNavigate } from "react-router-dom";
import { useTranslation } from 'react-i18next';
import { TransactionSummary } from "@/features/transactions/components/TransactionSummary";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { useState } from "react";
import { useDocuments } from "@/features/documents/api/queries";
import { DocumentList } from "@/features/documents/components/DocumentList";
import { UploadDocumentModal } from "@/features/documents/components/UploadDocumentModal";
import { DocumentReviewModal } from "@/features/documents/components/DocumentReviewModal";
import { type DocumentRequest } from "@/features/documents/types";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { Button } from "@/shared/components/ui/button";

export function ClientTransactionDetailsPage() {
  const { transactionId } = useParams();
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');

  const { data: documents, isLoading: isLoadingDocs } = useDocuments(transactionId ?? '');
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedDocument, setSelectedDocument] = useState<DocumentRequest | null>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  const [selectedDocumentForReview, setSelectedDocumentForReview] = useState<DocumentRequest | null>(null);

  const handleUploadClick = (doc: DocumentRequest) => {
    setSelectedDocument(doc);
    setIsUploadModalOpen(true);
  };

  const handleUploadSuccess = () => {
    setIsUploadModalOpen(false);
    setSelectedDocument(null);
    // Query invalidation is handled in the mutation
  };

  const handleReviewClick = (doc: DocumentRequest) => {
    setSelectedDocumentForReview(doc);
    setIsReviewModalOpen(true);
  };

  const handleReviewSuccess = () => {
    setIsReviewModalOpen(false);
    setSelectedDocumentForReview(null);
  };

  if (!transactionId) {
    return (
      <ErrorState
        title={t('transactionNotFound')}
        message={t('noTransactionId')}
        action={
          <Button onClick={() => navigate('/dashboard/client')}>
            {t('goBack')}
          </Button>
        }
      />
    );
  }

  return (
    <div className="space-y-6">
      <ErrorBoundary key={transactionId}>
        <TransactionSummary
          transactionId={transactionId}
        />

        <div className="space-y-4">
          <h2 className="text-xl font-semibold text-gray-900">{t('documents', { ns: 'documents' })}</h2>
          {isLoadingDocs ? (
            <LoadingState />
          ) : documents && documents.length > 0 ? (
            <DocumentList documents={documents} onUpload={handleUploadClick} onReview={handleReviewClick} />
          ) : (
            <div className="p-8 text-center bg-gray-50 rounded-lg border border-gray-100">
              <p className="text-gray-500">{t('noDocuments', { ns: 'documents' })}</p>
            </div>
          )}
        </div>

        {selectedDocument && (
          <UploadDocumentModal
            open={isUploadModalOpen}
            onClose={() => setIsUploadModalOpen(false)}
            requestId={selectedDocument.requestId}
            transactionId={transactionId}
            documentTitle={selectedDocument.customTitle || t(`types.${selectedDocument.docType}`, { ns: 'documents' })}
            onSuccess={handleUploadSuccess}
          />
        )}

        {selectedDocumentForReview && (
          <DocumentReviewModal
            open={isReviewModalOpen}
            onClose={() => setIsReviewModalOpen(false)}
            document={selectedDocumentForReview}
            transactionId={transactionId}
            onSuccess={handleReviewSuccess}
          />
        )}
      </ErrorBoundary>
    </div>
  );
}
