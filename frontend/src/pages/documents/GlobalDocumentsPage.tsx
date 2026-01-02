import { useState } from "react";
import { StageDropdownSelector } from '@/features/documents/components/StageDropdownSelector';
import { useStageOptions } from '@/features/documents/hooks/useStageOptions';
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useAuth0 } from "@auth0/auth0-react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { EmptyState } from "@/shared/components/branded/EmptyState";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { FileText } from "lucide-react";
import { useTranslation } from "react-i18next";
import { fetchAllDocuments } from "@/features/documents/api/documentsApi";
import { DocumentList } from "@/features/documents/components/DocumentList";
import { UploadDocumentModal } from "@/features/documents/components/UploadDocumentModal";
import { DocumentReviewModal } from "@/features/documents/components/DocumentReviewModal";
import { type DocumentRequest } from "@/features/documents/types";
import { toast } from "sonner";
import { getRoleFromUser } from "@/features/auth/roleUtils";

export function GlobalDocumentsPage() {

    const { t } = useTranslation('documents');
    const { user } = useAuth0();
    const role = getRoleFromUser(user);
    const canReview = role === "broker";
    const canUpload = role === "client";
    const queryClient = useQueryClient();
    const { data: documents = [], isLoading, error, refetch } = useQuery({
        queryKey: ['documents', 'all'],
        queryFn: fetchAllDocuments,
    });
    // Determine transaction side for global dropdown (find first doc with valid side or default to BUY_SIDE)
    const side = (documents.find(d => d.transactionRef?.side)?.transactionRef?.side ?? 'BUY_SIDE') as 'BUY_SIDE' | 'SELL_SIDE';
    // Add "All stages" option at top of selector
    const stageOptionsRaw = useStageOptions(side);
    const allStagesOption = { value: '', label: t('allStages', 'Toutes les étapes') };
    const stageOptions = [allStagesOption, ...stageOptionsRaw];
    const [selectedStage, setSelectedStage] = useState('');
    // Restore missing hooks
    const [selectedDocument, setSelectedDocument] = useState<DocumentRequest | null>(null);
    const [selectedDocumentForReview, setSelectedDocumentForReview] = useState<DocumentRequest | null>(null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

    const handleUploadSuccess = () => {
        toast.success(t('uploadSuccess', 'Document uploaded successfully'));
        setIsUploadModalOpen(false);
        setSelectedDocument(null);
        queryClient.invalidateQueries({ queryKey: ['documents'] });
    };

    const handleUploadClick = (document: DocumentRequest) => {
        setSelectedDocument(document);
        setIsUploadModalOpen(true);
    };

    const handleReviewClick = (document: DocumentRequest) => {
        setSelectedDocumentForReview(document);
        setIsReviewModalOpen(true);
    };


    if (isLoading) return <LoadingState />;
    if (error) return <ErrorState message={(error as Error).message} onRetry={() => refetch()} />;

    // Debug: log document structure to find stage field
    if (documents.length > 0) {
        console.log('Document structure example:', documents[0]);
    }

    // Filter documents by selected stage (for all roles)
    let filteredDocuments = documents;
    if (selectedStage) {
        filteredDocuments = documents.filter(doc => doc.stage === selectedStage);
    }

    return (
        <div className="space-y-6">
            <PageHeader
                title={t('globalTitle', 'My Documents')}
                subtitle={t('globalSubtitle', 'View and manage all your documents across transactions.')}
            />

                        {/* Stage selector (dropdown) visible for broker AND client */}
                        {(role === 'client' || role === 'broker') && (
                            <StageDropdownSelector
                                stages={stageOptions}
                                selectedStage={selectedStage}
                                onSelectStage={setSelectedStage}
                            />
                        )}

            {filteredDocuments.length === 0 ? (
                <Section>
                    <EmptyState
                        icon={<FileText />}
                        title={t('noDocumentsTitle', 'No documents found')}
                        description={t('noDocumentsForStage', "No documents requested for this stage.")}
                    />
                </Section>
            ) : (
                <DocumentList documents={filteredDocuments} onUpload={canUpload ? handleUploadClick : undefined} onReview={canReview ? handleReviewClick : undefined} />
            )}

            {selectedDocument && (
                <UploadDocumentModal
                    open={isUploadModalOpen}
                    onClose={() => setIsUploadModalOpen(false)}
                    requestId={selectedDocument.requestId}
                    transactionId={selectedDocument.transactionRef.transactionId}
                    documentTitle={selectedDocument.customTitle || selectedDocument.docType}
                    onSuccess={handleUploadSuccess}
                />
            )}

            {selectedDocumentForReview && (
                <DocumentReviewModal
                    open={isReviewModalOpen}
                    onClose={() => setIsReviewModalOpen(false)}
                    document={selectedDocumentForReview}
                    transactionId={selectedDocumentForReview.transactionRef.transactionId}
                    onSuccess={() => {
                        queryClient.invalidateQueries({ queryKey: ['documents'] });
                        setIsReviewModalOpen(false);
                        setSelectedDocumentForReview(null);
                    }}
                />
            )}
        </div>
    );
}
