import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
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
import { type DocumentRequest } from "@/features/documents/types";
import { toast } from "sonner";

export function GlobalDocumentsPage() {
    const { t } = useTranslation('documents');
    const queryClient = useQueryClient();
    const [selectedDocument, setSelectedDocument] = useState<DocumentRequest | null>(null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

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
                <DocumentList documents={documents} onUpload={handleUploadClick} />
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
        </div>
    );
}
