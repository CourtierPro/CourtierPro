import { useState } from "react";
import { Section } from "@/shared/components/branded/Section";
import { type DocumentRequest, DocumentStatusEnum, DocumentTypeEnum } from "@/features/documents/types";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { Button } from "@/shared/components/ui/button";
import { FileText, Upload, CheckCircle, Clock, File, Eye, Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getDocumentDownloadUrl } from "@/features/documents/api/documentsApi";
import { toast } from "sonner";

interface DocumentCardProps {
    document: DocumentRequest;
    onUpload?: (document: DocumentRequest) => void;
}

export function DocumentCard({ document, onUpload }: DocumentCardProps) {
    const { t, i18n } = useTranslation('documents');
    const [isLoadingView, setIsLoadingView] = useState(false);
    const title = document.customTitle || t(`types.${document.docType}`);

    const locale = i18n.language === 'fr' ? fr : enUS;
    const date = document.lastUpdatedAt ? format(new Date(document.lastUpdatedAt), 'PPP', { locale }) : '...';

    const getStatusColor = (status: DocumentStatusEnum) => {
        switch (status) {
            case DocumentStatusEnum.APPROVED: return "bg-green-100 text-green-800";
            case DocumentStatusEnum.SUBMITTED: return "bg-blue-100 text-blue-800";
            case DocumentStatusEnum.NEEDS_REVISION: return "bg-red-100 text-red-800";
            default: return "bg-gray-100 text-gray-800";
        }
    };

    const getIcon = (type: DocumentTypeEnum) => {
        switch (type) {
            case DocumentTypeEnum.MORTGAGE_APPROVAL:
            case DocumentTypeEnum.MORTGAGE_PRE_APPROVAL:
                return <FileText className="w-5 h-5 text-blue-500" />;
            case DocumentTypeEnum.ID_VERIFICATION:
                return <CheckCircle className="w-5 h-5 text-purple-500" />;
            default:
                return <File className="w-5 h-5 text-gray-500" />;
        }
    };

    const handleViewClick = async () => {
        if (document.submittedDocuments.length === 0) return;

        const latestDoc = document.submittedDocuments[document.submittedDocuments.length - 1];
        setIsLoadingView(true);

        try {
            const url = await getDocumentDownloadUrl(
                document.transactionRef.transactionId,
                document.requestId,
                latestDoc.documentId
            );
            window.open(url, '_blank');
        } catch {
            toast.error(t('errors.viewFailed', 'Failed to load document'));
        } finally {
            setIsLoadingView(false);
        }
    };

    return (
        <Section className="p-4 transition-all hover:shadow-md border border-gray-100">
            <div className="flex items-start justify-between">
                <div className="flex gap-4">
                    <div className="p-2 bg-gray-50 rounded-lg">
                        {getIcon(document.docType)}
                    </div>
                    <div>
                        <h3 className="font-semibold text-gray-900">{title}</h3>
                        <div className="flex items-center gap-2 mt-1 text-sm text-gray-500">
                            <Clock className="w-3 h-3" />
                            <span>{t('lastUpdated')}: {date}</span>
                        </div>
                        {document.brokerNotes && (
                            <p className="mt-2 text-sm text-gray-600 bg-gray-50 p-2 rounded">
                                {document.brokerNotes}
                            </p>
                        )}
                    </div>
                </div>

                <div className="flex flex-col items-end gap-3">
                    <span className={`px-3 py-1 text-xs font-medium rounded-full ${getStatusColor(document.status)}`}>
                        {t(`status.${document.status}`, document.status)}
                    </span>

                    <div className="flex gap-2">
                        {document.submittedDocuments.length > 0 && (
                            <Button
                                size="sm"
                                variant="outline"
                                onClick={handleViewClick}
                                disabled={isLoadingView}
                                className="gap-2"
                            >
                                {isLoadingView ? (
                                    <Loader2 className="w-4 h-4 animate-spin" />
                                ) : (
                                    <Eye className="w-4 h-4" />
                                )}
                                {t('view', 'View')}
                            </Button>
                        )}

                        {document.status === DocumentStatusEnum.REQUESTED && onUpload && (
                            <Button size="sm" onClick={() => onUpload(document)} className="gap-2">
                                <Upload className="w-4 h-4" />
                                {t('upload')}
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </Section>
    );
}

