import { useState, useCallback } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { SectionHeader } from "@/shared/components/branded/SectionHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Plus, FileText, Download, Eye, Tag, Upload } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { toast } from "sonner";

import { useDocumentsPageLogic } from "@/features/documents/hooks/useDocumentsPageLogic";
import { RequestDocumentModal } from "@/features/documents/components/RequestDocumentModal";
import { UploadForClientModal } from "@/features/documents/components/UploadForClientModal";
import { UploadDocumentModal } from "@/features/documents/components/UploadDocumentModal";
import { DocumentReviewModal } from "@/features/documents/components/DocumentReviewModal";
import { DocumentList } from "@/features/documents/components/DocumentList";
import { EditDocumentModal } from "@/features/documents/components/EditDocumentModal";
import { useUpdateDocument, useSendDocumentRequest, useDeleteDocument, useUploadFileToDocument, useShareDocumentWithClient } from "@/features/documents/api/mutations";
import { useTranslation } from "react-i18next";
import { StageDropdownSelector } from '@/features/documents/components/StageDropdownSelector';
import { useStageOptions } from '@/features/documents/hooks/useStageOptions';
import { type Document } from "@/features/documents/types";
import { getRoleFromUser } from "@/features/auth/roleUtils";
import { useAllTransactionDocuments } from "@/features/transactions/api/queries";
import { OutstandingDocumentsDashboard } from "@/features/documents/components/OutstandingDocumentsDashboard";
import axiosInstance from "@/shared/api/axiosInstance";
import { format } from "date-fns";
import type { UnifiedDocument } from "@/shared/api/types";
import { useParticipantPermissions } from "@/features/transactions/hooks/useParticipantPermissions";

interface DocumentsPageProps {
  transactionId: string;
  focusDocumentId?: string | null;
  isReadOnly?: boolean;
  transactionSide?: 'BUY_SIDE' | 'SELL_SIDE';
  hideRequestButton?: boolean;
  clientId?: string;
  currentStage?: string;
}

export function DocumentsPage({ transactionId, focusDocumentId, isReadOnly = false, transactionSide = 'BUY_SIDE', hideRequestButton = false, clientId, currentStage }: DocumentsPageProps) {
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

  const { checkPermission } = useParticipantPermissions(transactionId);
  const canEditDocuments = checkPermission('EDIT_DOCUMENTS');

  // Fetch unified documents (includes offer attachments)
  const { data: allDocuments = [], isLoading: isLoadingAllDocs } = useAllTransactionDocuments(transactionId, clientId);

  // Filter out CLIENT_UPLOAD (shown in DocumentList) and keep only offer attachments
  const offerDocuments = allDocuments.filter(doc => doc.source !== 'CLIENT_UPLOAD');

  // Determine stages based on transaction side
  const rawStageOptions = useStageOptions(transactionSide);
  const allStagesOption = { value: '', label: tTransactions('allStages') };
  const stageOptions = [allStagesOption, ...rawStageOptions];
  const [selectedStage, setSelectedStage] = useState('');

  const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
  const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
  const [selectedDocumentForReview, setSelectedDocumentForReview] = useState<Document | null>(null);
  const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);
  // Edit modal state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editingDocument, setEditingDocument] = useState<Document | null>(null);
  // Upload for client modal state
  const [isUploadForClientModalOpen, setIsUploadForClientModalOpen] = useState(false);

  const updateDocumentMutation = useUpdateDocument();
  const sendDocumentRequestMutation = useSendDocumentRequest();
  const deleteDocumentMutation = useDeleteDocument();
  const uploadFileToDocumentMutation = useUploadFileToDocument();
  const shareDocumentWithClientMutation = useShareDocumentWithClient();

  const handleUploadClick = (document: Document) => {
    setSelectedDocument(document);
    setIsUploadModalOpen(true);
  };

  // Handler for uploading file to an existing document without changing status (used by UploadForClientModal for drafts)
  const handleUploadFileForDocument = useCallback(
    async (documentId: string, file: File) => {
      await uploadFileToDocumentMutation.mutateAsync({
        transactionId,
        documentId,
        file
      });
      refetch();
    },
    [transactionId, uploadFileToDocumentMutation, refetch]
  );

  // Send Request handler for draft documents
  const handleSendDocumentRequest = useCallback(
    (document: Document) => {
      sendDocumentRequestMutation.mutate(
        { transactionId, documentId: document.documentId },
        {
          onSuccess: () => {
            toast.success(tDocuments('success.requestSent', 'Document request sent to client'));
            refetch();
          },
          onError: () => {
            toast.error(tDocuments('errors.sendRequestFailed', 'Failed to send request'));
          }
        }
      );
    },
    [transactionId, sendDocumentRequestMutation, refetch, tDocuments]
  );

  // Send Request handler by documentId (used by RequestDocumentModal for signature flow)
  const handleSendDocumentRequestById = useCallback(
    async (documentId: string) => {
      await sendDocumentRequestMutation.mutateAsync(
        { transactionId, documentId },
      );
      toast.success(tDocuments('success.requestSent', 'Document request sent to client'));
      refetch();
    },
    [transactionId, sendDocumentRequestMutation, refetch, tDocuments]
  );

  // Delete handler for draft documents
  const handleDeleteDocument = useCallback(
    (document: Document) => {
      toast(tDocuments('confirmDeleteDraft', 'Are you sure you want to delete this draft?'), {
        action: {
          label: tDocuments('actions.delete', 'Delete'),
          onClick: () => {
            deleteDocumentMutation.mutate(
              { transactionId, documentId: document.documentId },
              {
                onSuccess: () => {
                  toast.success(tDocuments('success.draftDeleted', 'Draft deleted successfully'));
                  refetch();
                },
                onError: () => {
                  toast.error(tDocuments('errors.deleteFailed', 'Failed to delete draft'));
                }
              }
            );
          }
        },
        cancel: {
          label: tDocuments('actions.cancel', 'Cancel'),
          onClick: () => { }
        }
      });
    },
    [transactionId, deleteDocumentMutation, refetch, tDocuments]
  );

  // Share handler for UPLOAD flow draft documents
  const handleShareDocument = useCallback(
    (document: Document) => {
      toast(tDocuments('confirmShareDraft', 'Are you sure you want to share this document with the client?'), {
        action: {
          label: tDocuments('actions.share', 'Share'),
          onClick: () => {
            shareDocumentWithClientMutation.mutate(
              { transactionId, documentId: document.documentId },
              {
                onSuccess: () => {
                  toast.success(tDocuments('success.documentShared', 'Document shared with client'));
                  refetch();
                },
                onError: () => {
                  toast.error(tDocuments('errors.shareFailed', 'Failed to share document'));
                }
              }
            );
          }
        },
        cancel: {
          label: tDocuments('actions.cancel', 'Cancel'),
          onClick: () => { }
        }
      });
    },
    [transactionId, shareDocumentWithClientMutation, refetch, tDocuments]
  );

  // Edit click handler
  const handleEditClick = useCallback(
    (document: Document) => {
      setEditingDocument(document);
      setIsEditModalOpen(true);
    },
    [setEditingDocument, setIsEditModalOpen]
  );

  // Edit submit handler
  const handleEditSubmit = useCallback(
    (formValues: import('@/shared/schemas').RequestDocumentFormValues) => {
      if (!editingDocument) return;
      const { instructions, ...restFormValues } = formValues;
      // Compare only fields present in RequestDocumentFormValues
      const changed =
        restFormValues.docType !== editingDocument.docType ||
        restFormValues.customTitle !== editingDocument.customTitle ||
        restFormValues.stage !== editingDocument.stage ||
        instructions !== editingDocument.brokerNotes;

      if (!changed) {
        // Optionally show a message to user: No changes made
        setIsEditModalOpen(false);
        setEditingDocument(null);
        return;
      }
      updateDocumentMutation.mutate(
        {
          transactionId: editingDocument.transactionRef.transactionId,
          documentId: editingDocument.documentId,
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
    [editingDocument, updateDocumentMutation, refetch, setIsEditModalOpen, setEditingDocument]
  );

  const handleUploadSuccess = () => {
    refetch();
    setIsUploadModalOpen(false);
    setSelectedDocument(null);
  };

  const handleReviewClick = (document: Document) => {
    setSelectedDocumentForReview(document);
    setIsReviewModalOpen(true);
  };

  const handleReviewSuccess = () => {
    refetch();
    setIsReviewModalOpen(false);
    setSelectedDocumentForReview(null);
  };

  const handleDownloadOfferDoc = async (doc: UnifiedDocument) => {
    try {
      const res = await axiosInstance.get<string>(`/transactions/documents/${doc.documentId}/download`);
      const url = res.data;
      if (url) {
        window.open(url, '_blank');
      }
    } catch {
      toast.error(tDocuments('errorDownloading', 'Error downloading document'));
    }
  };

  const getSourceBadgeVariant = (source: string) => {
    switch (source) {
      case 'OFFER_ATTACHMENT':
        return 'secondary';
      case 'PROPERTY_OFFER_ATTACHMENT':
        return 'outline';
      default:
        return 'default';
    }
  };

  const getSourceLabel = (source: string) => {
    switch (source) {
      case 'OFFER_ATTACHMENT':
        return tTransactions('offerAttachment', 'Offer Attachment');
      case 'PROPERTY_OFFER_ATTACHMENT':
        return tTransactions('propertyOfferAttachment', 'Property Offer Attachment');
      default:
        return source;
    }
  };

  if (isLoading) return <LoadingState />;
  if (error) return <ErrorState message={error.message} onRetry={() => refetch()} />;

  // Filter documents by selected stage for all roles
  let filteredDocuments = documents;
  if (selectedStage) {
    filteredDocuments = documents.filter(doc => doc.stage === selectedStage);
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={tDocuments('title', 'Documents')}
        subtitle={tDocuments('subtitle', 'Manage all your transaction documents in one place.')}
        actions={
          !isReadOnly && !hideRequestButton && canReview && canEditDocuments && (
            <>
              <Button variant="outline" onClick={() => setIsUploadForClientModalOpen(true)}>
                <Upload className="w-4 h-4 mr-2" />
                {tDocuments('uploadForClient', 'Upload for Client')}
              </Button>
              <Button onClick={() => setIsModalOpen(true)}>
                <Plus className="w-4 h-4 mr-2" />
                {tDocuments('requestDocument', 'Request Document')}
              </Button>
            </>
          )
        }
      />

      {/* Outstanding Documents Dashboard (Only visible to brokers) */}
      {role === 'broker' && <OutstandingDocumentsDashboard />}

      {/* Stage dropdown selector */}
      <StageDropdownSelector
        stages={stageOptions}
        selectedStage={selectedStage}
        onSelectStage={setSelectedStage}
      />

      <DocumentList
        documents={filteredDocuments}
        onUpload={canUpload || canReview ? handleUploadClick : undefined}
        onReview={canReview ? handleReviewClick : undefined}
        onEdit={canReview && canEditDocuments ? handleEditClick : undefined}
        onSendRequest={canReview ? handleSendDocumentRequest : undefined}
        onShare={canReview && canEditDocuments ? handleShareDocument : undefined}
        onDelete={canReview && canEditDocuments ? handleDeleteDocument : undefined}
        focusDocumentId={focusDocumentId}
        isBroker={role === 'broker'}
      />

      {/* Offer Documents Section */}
      {offerDocuments.length > 0 && (
        <Section>
          <SectionHeader
            title={tTransactions('offerDocuments', 'Offer Documents')}
            description={tTransactions('offerDocumentsDescription', 'Documents attached to offers and property offers')}
          />
          <div className="space-y-2 mt-4">
            {isLoadingAllDocs ? (
              <LoadingState />
            ) : (
              offerDocuments.map((doc) => (
                <div
                  key={doc.documentId}
                  className="flex items-center justify-between p-3 border rounded-lg bg-card hover:bg-accent/50 transition-colors"
                >
                  <div className="flex items-center gap-3 flex-1 min-w-0">
                    <FileText className="w-5 h-5 text-muted-foreground flex-shrink-0" />
                    <div className="flex-1 min-w-0">
                      <p className="font-medium truncate">{doc.fileName}</p>
                      <div className="flex items-center gap-2 text-xs text-muted-foreground">
                        <span>{doc.sourceName}</span>
                        {doc.uploadedAt && (
                          <>
                            <span>•</span>
                            <span>{format(new Date(doc.uploadedAt), 'MMM d, yyyy')}</span>
                          </>
                        )}
                      </div>
                    </div>
                    <Badge variant={getSourceBadgeVariant(doc.source)} className="flex-shrink-0">
                      <Tag className="w-3 h-3 mr-1" />
                      {getSourceLabel(doc.source)}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-1 ml-2">
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => handleDownloadOfferDoc(doc)}
                      title={tTransactions('view')}
                    >
                      <Eye className="w-4 h-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="icon"
                      className="h-8 w-8"
                      onClick={() => handleDownloadOfferDoc(doc)}
                      title={tTransactions('download')}
                    >
                      <Download className="w-4 h-4" />
                    </Button>
                  </div>
                </div>
              ))
            )}
          </div>
        </Section>
      )}
      {editingDocument && (
        <EditDocumentModal
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
            requiresSignature: editingDocument.requiresSignature ?? false,
          }}
        />
      )}

      <RequestDocumentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleRequestDocument}
        onUploadFile={handleUploadFileForDocument}
        onSendDocumentRequest={handleSendDocumentRequestById}
        transactionType={transactionSide === 'BUY_SIDE' ? 'buy' : 'sell'}
        currentStage={currentStage || ''}
        transactionId={transactionId}
      />

      {/* Key forces remount on open to reset all state */}
      <UploadForClientModal
        key={isUploadForClientModalOpen ? 'open' : 'closed'}
        isOpen={isUploadForClientModalOpen}
        onClose={() => setIsUploadForClientModalOpen(false)}
        onSubmit={handleRequestDocument}
        onUploadFile={handleUploadFileForDocument}
        transactionType={transactionSide === 'BUY_SIDE' ? 'buy' : 'sell'}
        currentStage={currentStage || ''}
        transactionId={transactionId}
      />

      {selectedDocument && (
        <UploadDocumentModal
          open={isUploadModalOpen}
          onClose={() => setIsUploadModalOpen(false)}
          documentId={selectedDocument.documentId}
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
