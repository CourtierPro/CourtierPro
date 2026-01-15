import { useState, useCallback } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Plus, FolderOpen } from "lucide-react";
import { Button } from "@/shared/components/ui/button";

import { useDocumentsPageLogic } from "@/features/documents/hooks/useDocumentsPageLogic";
import { RequestDocumentModal } from "@/features/documents/components/RequestDocumentModal";
import { UploadDocumentModal } from "@/features/documents/components/UploadDocumentModal";
import { DocumentReviewModal } from "@/features/documents/components/DocumentReviewModal";
import { DocumentList } from "@/features/documents/components/DocumentList";
import { EditDocumentRequestModal } from "@/features/documents/components/EditDocumentRequestModal";
import { useUpdateDocumentRequest } from "@/features/documents/api/mutations";
import { useTranslation } from "react-i18next";
import { StageDropdownSelector } from '@/features/documents/components/StageDropdownSelector';
import { useStageOptions } from '@/features/documents/hooks/useStageOptions';
import { type DocumentRequest } from "@/features/documents/types";
import { getRoleFromUser } from "@/features/auth/roleUtils";

interface DocumentsPageProps {
  transactionId: string;
  focusDocumentId?: string | null;
  isReadOnly?: boolean;
  transactionSide?: 'BUY_SIDE' | 'SELL_SIDE'; // Optionally passed, fallback to BUY_SIDE
  hideRequestButton?: boolean;
}

export function DocumentsPage({ transactionId, focusDocumentId, isReadOnly = false, transactionSide = 'BUY_SIDE', hideRequestButton = false }: DocumentsPageProps) {
  const { t: tDocuments } = useTranslation('documents');
  const { t: tTransactions } = useTranslation('transactions');
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

  // Determine stages based on transaction side
  const rawStageOptions = useStageOptions(transactionSide);
  const allStagesOption = { value: '', label: tTransactions('allStages') };
  const stageOptions = [allStagesOption, ...rawStageOptions];
  const [selectedStage, setSelectedStage] = useState('');

  const [selectedDocument, setSelectedDocument] = useState<DocumentRequest | null>(null);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedDocumentForReview, setSelectedDocumentForReview] = useState<DocumentRequest | null>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  // Edit modal state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingDocument, setEditingDocument] = useState<DocumentRequest | null>(null);

  const updateDocumentRequestMutation = useUpdateDocumentRequest();

  const handleUploadClick = (document: DocumentRequest) => {
    setSelectedDocument(document);
    setIsUploadModalOpen(true);
  };

  // Edit click handler
  const handleEditClick = useCallback((document: DocumentRequest) => {
    setEditingDocument(document);
    setIsEditModalOpen(true);
  }, []);

  // Edit submit handler
  const handleEditSubmit = useCallback(
    (formValues: import('@/shared/schemas').RequestDocumentFormValues) => {
      if (!editingDocument) return;
        const { instructions, ...restFormValues } = formValues;
        updateDocumentRequestMutation.mutate(
          {
            transactionId: editingDocument.transactionRef.transactionId,
            requestId: editingDocument.requestId,
            data: {
              ...restFormValues,
              brokerNotes: instructions,
            },
          },
          {
            onSuccess: () => {
              setIsEditModalOpen(false);
              setEditingDocument(null);
              refetch();
            },
          }
        );
    },
    [editingDocument, updateDocumentRequestMutation, refetch]
  );

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

  // Debug: log document structure to find stage field
  if (documents.length > 0) {
    console.log('Document structure example:', documents[0]);
  }
  // Filter documents by selected stage for all roles
  let filteredDocuments = documents;
  if (selectedStage) {
    filteredDocuments = documents.filter(doc => doc.stage === selectedStage);
  }
  // If selectedStage is empty ("All stages" option), keep all documents

  return (
    <div className="space-y-6">
      <PageHeader
        title={tDocuments('title', 'Documents')}
        subtitle={tDocuments('subtitle', 'Manage all your transaction documents in one place.')}
        actions={
          !isReadOnly && !hideRequestButton && documents.length > 0 && (
            <Button onClick={() => setIsModalOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              {tDocuments('requestDocument', 'Request Document')}
            </Button>
          )
        }
      />

      {/* Sélecteur d'étape (dropdown) visible pour tous les rôles */}
      <StageDropdownSelector
        stages={stageOptions}
        selectedStage={selectedStage}
        onSelectStage={setSelectedStage}
      />

      {filteredDocuments.length === 0 ? (
        <Section>
          <EmptyState
            icon={<FolderOpen className="w-12 h-12 text-muted-foreground" />}
            title={tDocuments('noDocumentsTitle', 'No documents found')}
            description={tDocuments('noDocumentsForStage', 'No documents requested for this stage.')}
            action={
              !isReadOnly && !hideRequestButton ? (
                <Button onClick={() => setIsModalOpen(true)} className="mt-4">
                  <Plus className="w-4 h-4 mr-2" />
                  {tDocuments('requestDocument', 'Request Document')}
                </Button>
              ) : undefined
            }
          />
        </Section>
      ) : (
        <DocumentList
          documents={filteredDocuments}
          onUpload={canUpload ? handleUploadClick : undefined}
          onReview={canReview ? handleReviewClick : undefined}
          onEdit={canReview ? handleEditClick : undefined}
          focusDocumentId={focusDocumentId}
        />
      )}
         {editingDocument && (
            <EditDocumentRequestModal
              isOpen={isEditModalOpen}
              onClose={() => {
                setIsEditModalOpen(false);
                setEditingDocument(null);
              }}
              onSubmit={handleEditSubmit}
              transactionType={transactionSide === 'BUY_SIDE' ? 'buy' : 'sell'}
              initialValues={{
                docType: editingDocument.docType,
                customTitle: editingDocument.customTitle || '',
                instructions: editingDocument.brokerNotes || '',
                stage: editingDocument.stage,
              }}
            />
          )}

      <RequestDocumentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleRequestDocument}
        transactionType={transactionSide === 'BUY_SIDE' ? 'buy' : 'sell'}
        currentStage={selectedStage}
      />

      {selectedDocument && (
        <UploadDocumentModal
          open={isUploadModalOpen}
          onClose={() => setIsUploadModalOpen(false)}
          requestId={selectedDocument.requestId}
          transactionId={transactionId}
          documentTitle={selectedDocument.customTitle || tDocuments(`types.${selectedDocument.docType}`)}
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

