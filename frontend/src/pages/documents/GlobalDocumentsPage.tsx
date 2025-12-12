import { useState } from "react";
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
    const [selectedDocument, setSelectedDocument] = useState<DocumentRequest | null>(null);
    const [selectedDocumentForReview, setSelectedDocumentForReview] = useState<DocumentRequest | null>(null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

    const { data: documents = [], isLoading, error, refetch } = useQuery({
        queryKey: ['documents', 'all'],
        queryFn: fetchAllDocuments,
    });

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

    return (
        <div className="space-y-6">
            <PageHeader
                title={t('globalTitle', 'My Documents')}
                subtitle={t('globalSubtitle', 'View and manage all your documents across transactions.')}
            />

            {documents.length === 0 ? (
                <Section>
                    <EmptyState
                        icon={<FileText />}
                        title={t('noDocumentsTitle', 'No documents found')}
                        description={t('noDocumentsDesc', "You don't have any documents yet.")}
                    />
                </Section>
            ) : (
                <DocumentList documents={documents} onUpload={canUpload ? handleUploadClick : undefined} onReview={canReview ? handleReviewClick : undefined} />
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
