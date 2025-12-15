
import { useState } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { FileText, Plus } from "lucide-react";
import { Button } from "@/shared/components/ui/button";

import { useDocumentsPageLogic } from "@/features/documents/hooks/useDocumentsPageLogic";
import { RequestDocumentModal } from "@/features/documents/components/RequestDocumentModal";
import { UploadDocumentModal } from "@/features/documents/components/UploadDocumentModal";
import { DocumentReviewModal } from "@/features/documents/components/DocumentReviewModal";
import { DocumentList } from "@/features/documents/components/DocumentList";
import { useTranslation } from "react-i18next";
import { type DocumentRequest } from "@/features/documents/types";
import { getRoleFromUser } from "@/features/auth/roleUtils";

interface DocumentsPageProps {
  transactionId: string;
  focusDocumentId?: string | null;
  isReadOnly?: boolean;
}

export function DocumentsPage({ transactionId, focusDocumentId, isReadOnly = false }: DocumentsPageProps) {
  const { t } = useTranslation('documents');
  const { user } = useAuth0();
  const role = getRoleFromUser(user);
  const canReview = role === "broker";
  const canUpload = role === "client";
  const {
    documents,
    isLoading,
    error,
    refetch,
    isModalOpen,
    setIsModalOpen,
    handleRequestDocument
  } = useDocumentsPageLogic(transactionId);

  const [selectedDocument, setSelectedDocument] = useState<DocumentRequest | null>(null);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedDocumentForReview, setSelectedDocumentForReview] = useState<DocumentRequest | null>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

  const handleUploadClick = (document: DocumentRequest) => {
    setSelectedDocument(document);
    setIsUploadModalOpen(true);
  };

  const handleUploadSuccess = () => {
    refetch();
    setIsUploadModalOpen(false);
    setSelectedDocument(null);
  };

  const handleReviewClick = (document: DocumentRequest) => {
    setSelectedDocumentForReview(document);
    setIsReviewModalOpen(true);
  };

  const handleReviewSuccess = () => {
    refetch();
    setIsReviewModalOpen(false);
    setSelectedDocumentForReview(null);
  };

  if (isLoading) return <LoadingState />;
  if (error) return <ErrorState message={error.message} onRetry={() => refetch()} />;

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('title', 'Documents')}
        subtitle={t('subtitle', 'Manage all your transaction documents in one place.')}
        actions={
          !isReadOnly && documents.length > 0 && (
            <Button onClick={() => setIsModalOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              {t('requestDocument', 'Request Document')}
            </Button>
          )
        }
      />

      {documents.length === 0 ? (
        <Section>
          <EmptyState
            icon={<FileText />}
            title={t('noDocumentsTitle', 'No documents found')}
            description={t('noDocumentsDesc', "You haven't uploaded any documents yet. Start by creating a transaction.")}
            action={
              !isReadOnly ? (
                <Button onClick={() => setIsModalOpen(true)}>{t('requestDocument', 'Request Document')}</Button>
              ) : undefined
            }
          />
        </Section>
      ) : (
        <DocumentList
          documents={documents}
          onUpload={canUpload ? handleUploadClick : undefined}
          onReview={canReview ? handleReviewClick : undefined}
          focusDocumentId={focusDocumentId}
        />
      )}

      <RequestDocumentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleRequestDocument}
        transactionType="buy"
        currentStage="offer"
      />

      {selectedDocument && (
        <UploadDocumentModal
          open={isUploadModalOpen}
          onClose={() => setIsUploadModalOpen(false)}
          requestId={selectedDocument.requestId}
          transactionId={transactionId}
          documentTitle={selectedDocument.customTitle || t(`types.${selectedDocument.docType}`)}
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
    </div>
  );
}

