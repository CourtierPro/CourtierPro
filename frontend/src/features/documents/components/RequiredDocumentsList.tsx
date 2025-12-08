import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { fetchDocuments } from '../api/documentsApi';
import { DocumentStatusEnum } from '../types';
import type { DocumentRequest } from '../types';
import { DocumentStatusBadge } from './DocumentStatusBadge';
import { Button } from '@/shared/components/ui/button';
import { UploadDocumentModal } from './UploadDocumentModal';
import { FileText, Upload } from 'lucide-react';
import { format } from 'date-fns';

interface RequiredDocumentsListProps {
    transactionId: string;
}

export function RequiredDocumentsList({ transactionId }: RequiredDocumentsListProps) {
    const { t } = useTranslation('documents');
    const [selectedRequest, setSelectedRequest] = useState<DocumentRequest | null>(null);
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    const { data: documents, isLoading, error, refetch } = useQuery({
        queryKey: ['documents', transactionId],
        queryFn: () => fetchDocuments(transactionId),
    });

    if (isLoading) return <div>{t('loading')}</div>;
    if (error) return <div>{t('errorLoadingDocuments')}</div>;

    const handleUploadClick = (request: DocumentRequest) => {
        setSelectedRequest(request);
        setIsUploadModalOpen(true);
    };

    const handleUploadSuccess = () => {
        refetch();
        setIsUploadModalOpen(false);
        setSelectedRequest(null);
    };

    return (
        <div className="space-y-4">
            <h3 className="text-lg font-semibold">{t('requiredDocuments')}</h3>
            <div className="grid gap-4">
                {documents?.map((doc) => (
                    <div
                        key={doc.requestId}
                        className="flex items-center justify-between p-4 border rounded-lg bg-white shadow-sm"
                    >
                        <div className="flex items-center gap-4">
                            <div className="p-2 bg-gray-100 rounded-full">
                                <FileText className="w-6 h-6 text-gray-600" />
                            </div>
                            <div>
                                <h4 className="font-medium">
                                    {doc.customTitle || t(`types.${doc.docType}`)}
                                </h4>
                                <div className="text-sm text-gray-500">
                                    {doc.submittedDocuments.length > 0 ? (
                                        <span>
                                            {t('lastSubmitted')}:{' '}
                                            {format(new Date(doc.submittedDocuments[doc.submittedDocuments.length - 1].uploadedAt), 'PP')}
                                        </span>
                                    ) : (
                                        <span>{t('notYetSubmitted')}</span>
                                    )}
                                </div>
                            </div>
                        </div>
                        <div className="flex items-center gap-4">
                            <DocumentStatusBadge status={doc.status} />
                            {doc.status !== DocumentStatusEnum.APPROVED && (
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleUploadClick(doc)}
                                >
                                    <Upload className="w-4 h-4 mr-2" />
                                    {doc.status === DocumentStatusEnum.REQUESTED ? t('upload') : t('reupload')}
                                </Button>
                            )}
                        </div>
                    </div>
                ))}
                {documents?.length === 0 && (
                    <div className="text-center p-8 text-gray-500">
                        {t('noDocumentsRequired')}
                    </div>
                )}
            </div>

            {selectedRequest && (
                <UploadDocumentModal
                    open={isUploadModalOpen}
                    onClose={() => setIsUploadModalOpen(false)}
                    requestId={selectedRequest.requestId}
                    transactionId={transactionId}
                    documentTitle={selectedRequest.customTitle || t(`types.${selectedRequest.docType}`)}
                    onSuccess={handleUploadSuccess}
                />
            )}
        </div>
    );
}
