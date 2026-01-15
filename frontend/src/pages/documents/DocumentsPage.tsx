import { useState } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { SectionHeader } from "@/shared/components/branded/SectionHeader";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Plus, FolderOpen, FileText, Download, Eye, Tag } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { toast } from "sonner";

import { useDocumentsPageLogic } from "@/features/documents/hooks/useDocumentsPageLogic";
import { RequestDocumentModal } from "@/features/documents/components/RequestDocumentModal";
import { UploadDocumentModal } from "@/features/documents/components/UploadDocumentModal";
import { DocumentReviewModal } from "@/features/documents/components/DocumentReviewModal";
import { DocumentList } from "@/features/documents/components/DocumentList";
import { useTranslation } from "react-i18next";
import { StageDropdownSelector } from '@/features/documents/components/StageDropdownSelector';
import { useStageOptions } from '@/features/documents/hooks/useStageOptions';
import { type DocumentRequest } from "@/features/documents/types";
import { getRoleFromUser } from "@/features/auth/roleUtils";
import { useAllTransactionDocuments } from "@/features/transactions/api/queries";
import axiosInstance from "@/shared/api/axiosInstance";
import { format } from "date-fns";
import type { UnifiedDocument } from "@/shared/api/types";

interface DocumentsPageProps {
  transactionId: string;
  focusDocumentId?: string | null;
  isReadOnly?: boolean;
  transactionSide?: 'BUY_SIDE' | 'SELL_SIDE';
  hideRequestButton?: boolean;
  clientId?: string;
}

export function DocumentsPage({ transactionId, focusDocumentId, isReadOnly = false, transactionSide = 'BUY_SIDE', hideRequestButton = false, clientId }: DocumentsPageProps) {
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

  // Fetch unified documents (includes offer attachments)
  const { data: allDocuments = [], isLoading: isLoadingAllDocs } = useAllTransactionDocuments(transactionId, clientId);

  // Filter out CLIENT_UPLOAD (shown in DocumentList) and keep only offer attachments
  const offerDocuments = allDocuments.filter(doc => doc.source !== 'CLIENT_UPLOAD');

  // Determine stages based on transaction side
  const rawStageOptions = useStageOptions(transactionSide);
  const allStagesOption = { value: '', label: tTransactions('allStages') };
  const stageOptions = [allStagesOption, ...rawStageOptions];
  const [selectedStage, setSelectedStage] = useState('');

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
          !isReadOnly && !hideRequestButton && canReview && (
            <Button onClick={() => setIsModalOpen(true)}>
              <Plus className="w-4 h-4 mr-2" />
              {tDocuments('requestDocument', 'Request Document')}
            </Button>
          )
        }
      />

      {/* Stage dropdown selector */}
      <StageDropdownSelector
        stages={stageOptions}
        selectedStage={selectedStage}
        onSelectStage={setSelectedStage}
      />

      {/* Client-submitted documents */}
      {filteredDocuments.length === 0 && offerDocuments.length === 0 ? (
        <Section>
          <EmptyState
            icon={<FolderOpen className="w-12 h-12 text-muted-foreground" />}
            title={tDocuments('noDocumentsTitle', 'No documents found')}
            description={tDocuments('noDocumentsForStage', 'No documents for this transaction yet.')}
            action={
              !isReadOnly && !hideRequestButton && canReview ? (
                <Button onClick={() => setIsModalOpen(true)} className="mt-4">
                  <Plus className="w-4 h-4 mr-2" />
                  {tDocuments('requestDocument', 'Request Document')}
                </Button>
              ) : undefined
            }
          />
        </Section>
      ) : (
        <>
          {filteredDocuments.length > 0 && (
            <DocumentList
              documents={filteredDocuments}
              onUpload={canUpload ? handleUploadClick : undefined}
              onReview={canReview ? handleReviewClick : undefined}
              focusDocumentId={focusDocumentId}
            />
          )}

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
        </>
      )}

      <RequestDocumentModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={handleRequestDocument}
        transactionType={transactionSide === 'BUY_SIDE' ? 'buy' : 'sell'}
        currentStage={selectedStage}
        transactionId={transactionId}
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


