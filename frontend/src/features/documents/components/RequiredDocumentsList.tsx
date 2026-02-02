import { useState } from 'react';
import confetti from 'canvas-confetti';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useAuth0 } from "@auth0/auth0-react";
import { fetchDocuments } from '../api/documentsApi';
import { DocumentStatusEnum, type Document } from '../types';
import { DocumentList } from './DocumentList';
import { UploadDocumentModal } from './UploadDocumentModal';
import { DocumentReviewModal } from './DocumentReviewModal';
import { DocumentListSkeleton } from './DocumentListSkeleton';
import { StatusFilterBar, type FilterStatus } from './StatusFilterBar';
import { formatDocumentTitle } from '../utils/formatDocumentTitle';
import { getRoleFromUser } from "@/features/auth/roleUtils";

interface RequiredDocumentsListProps {
    transactionId: string;
}

export function RequiredDocumentsList({ transactionId }: RequiredDocumentsListProps) {
    const { t } = useTranslation('documents');
    const { user } = useAuth0();
    const role = getRoleFromUser(user);
    const canReview = role === "broker";
    const [selectedDocument, setSelectedDocument] = useState<Document | null>(null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);
    const [initialFile, setInitialFile] = useState<File | null>(null);
    const [currentFilter, setCurrentFilter] = useState<FilterStatus>('ALL');
    const [reviewModalOpen, setReviewModalOpen] = useState(false);
    const [selectedDocumentForReview, setSelectedDocumentForReview] = useState<Document | null>(null);

    const { data: documents, isLoading, error, refetch } = useQuery({
        queryKey: ['documents', transactionId],
        queryFn: () => fetchDocuments(transactionId),
    });

    const filteredDocuments = documents?.filter(doc =>
        currentFilter === 'ALL' ? true : doc.status === currentFilter
    );

    const counts = documents ? {
        ALL: documents.length,
        [DocumentStatusEnum.REQUESTED]: documents.filter(d => d.status === DocumentStatusEnum.REQUESTED).length + documents.filter(d => d.status === DocumentStatusEnum.NEEDS_REVISION).length,
        [DocumentStatusEnum.SUBMITTED]: documents.filter(d => d.status === DocumentStatusEnum.SUBMITTED).length,
        [DocumentStatusEnum.APPROVED]: documents.filter(d => d.status === DocumentStatusEnum.APPROVED).length,
        [DocumentStatusEnum.NEEDS_REVISION]: 0, // Merged with REQUESTED for simplicity in filter count, or can be separate
    } : { ALL: 0, [DocumentStatusEnum.REQUESTED]: 0, [DocumentStatusEnum.SUBMITTED]: 0, [DocumentStatusEnum.APPROVED]: 0, [DocumentStatusEnum.NEEDS_REVISION]: 0 };

    if (isLoading) return <DocumentListSkeleton />;
    if (error) return <div>{t('errorLoadingDocuments')}</div>;

    const handleUploadClick = (doc: Document, file?: File) => {
        setSelectedDocument(doc);
        if (file) {
            // We need to pass this file to the modal. 
            // Since the modal controls its own file state, we might need a way to pass initialFile.
            // For now, let's keep it simple and just open the modal. 
            // To properly support drop-on-card -> open-modal-with-file, we need to add initialFile prop to UploadDocumentModal.
            setInitialFile(file);
        }
        setIsUploadModalOpen(true);
    };

    const handleUploadSuccess = () => {
        refetch();
        setIsUploadModalOpen(false);
        setSelectedDocument(null);
        setInitialFile(null);
        confetti({
            particleCount: 100,
            spread: 70,
            origin: { y: 0.6 }
        });
    };

    const handleReviewClick = (doc: Document) => {
        setSelectedDocumentForReview(doc);
        setReviewModalOpen(true);
    };

    const handleReviewSuccess = () => {
        refetch();
        setReviewModalOpen(false);
        setSelectedDocumentForReview(null);
    };

    return (
        <div className="space-y-4">
            <h3 className="text-lg font-semibold">{t('requiredDocuments')}</h3>

            {documents && documents.length > 0 && (
                <StatusFilterBar
                    currentFilter={currentFilter}
                    onFilterChange={setCurrentFilter}
                    counts={counts as Record<FilterStatus, number>}
                />
            )}

            {documents && (
                <DocumentList documents={filteredDocuments || []} onUpload={handleUploadClick} onReview={canReview ? handleReviewClick : undefined} />
            )}

            {selectedDocument && (
                <UploadDocumentModal
                    open={isUploadModalOpen}
                    onClose={() => setIsUploadModalOpen(false)}
                    documentId={selectedDocument.documentId}
                    transactionId={transactionId}
                    documentTitle={formatDocumentTitle(selectedDocument, t)}
                    docType={selectedDocument.docType}
                    initialFile={initialFile}
                    onSuccess={handleUploadSuccess}
                />
            )}

            {selectedDocumentForReview && (
                <DocumentReviewModal
                    open={reviewModalOpen}
                    onClose={() => setReviewModalOpen(false)}
                    document={selectedDocumentForReview}
                    transactionId={transactionId}
                    onSuccess={handleReviewSuccess}
                />
            )}
        </div>
    );
}
