import { useState, useCallback } from "react";
import { Link } from "react-router-dom";
import { useDropzone } from "react-dropzone";
import { Section } from "@/shared/components/branded/Section";
import { type DocumentRequest, DocumentStatusEnum, DocumentTypeEnum } from "@/features/documents/types";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { Button } from "@/shared/components/ui/button";
import { FileText, Upload, CheckCircle, Clock, File, Eye, Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getDocumentDownloadUrl } from "@/features/documents/api/documentsApi";
import { formatDocumentTitle } from "../utils/formatDocumentTitle";
import { toast } from "sonner";

interface DocumentCardProps {
    document: DocumentRequest;
    onUpload?: (document: DocumentRequest, file?: File) => void;
}

export function DocumentCard({ document, onUpload }: DocumentCardProps) {
    const { t, i18n } = useTranslation('documents');
    const [isLoadingView, setIsLoadingView] = useState(false);
    const title = formatDocumentTitle(document, t);

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

    const onDrop = useCallback((acceptedFiles: File[]) => {
        if (acceptedFiles.length > 0 && onUpload && (document.status === DocumentStatusEnum.REQUESTED || document.status === DocumentStatusEnum.NEEDS_REVISION)) {
            onUpload(document, acceptedFiles[0]);
        }
    }, [document, onUpload]);

    const { getRootProps, getInputProps, isDragActive } = useDropzone({
        onDrop,
        noClick: true, // We want click to only trigger on buttons
        noKeyboard: true,
        accept: {
            'application/pdf': ['.pdf'],
            'image/jpeg': ['.jpg', '.jpeg'],
            'image/png': ['.png']
        },
        disabled: !onUpload || (document.status !== DocumentStatusEnum.REQUESTED && document.status !== DocumentStatusEnum.NEEDS_REVISION)
    });

    return (
        <Section {...getRootProps()} className={`p-4 transition-all hover:shadow-md border relative ${isDragActive ? 'border-blue-500 bg-blue-50' : 'border-gray-100'}`}>
            <input {...getInputProps()} />
            {isDragActive && (
                <div className="absolute inset-0 z-10 flex items-center justify-center bg-blue-50/80 rounded-lg backdrop-blur-sm">
                    <p className="text-blue-600 font-semibold flex items-center gap-2">
                        <Upload className="w-6 h-6" />
                        {t('dropToUpload', 'Drop file to upload')}
                    </p>
                </div>
            )}
            <div className="flex items-start justify-between">
                <div className="flex gap-4">
                    <div className="p-2 bg-gray-50 rounded-lg">
                        {getIcon(document.docType)}
                    </div>
                    <div>
                        <h3 className="font-semibold text-gray-900">{title}</h3>
                        <div className="flex flex-col gap-1 mt-1">
                            <div className="flex items-center gap-2 text-sm text-gray-500">
                                <Clock className="w-3 h-3" />
                                <span>{t('lastUpdated')}: {date}</span>
                            </div>
                            <div className="flex items-center gap-1 text-xs text-gray-400">
                                <span>Ref:</span>
                                <Link
                                    to={`/transactions/${document.transactionRef.transactionId}`}
                                    className="hover:underline hover:text-blue-600"
                                    onClick={(e) => e.stopPropagation()}
                                >
                                    #{document.transactionRef.transactionId.substring(0, 8)}
                                </Link>
                            </div>
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

                        {(document.status === DocumentStatusEnum.REQUESTED || document.status === DocumentStatusEnum.NEEDS_REVISION) && onUpload && (
                            <Button size="sm" onClick={() => onUpload(document)} className="gap-2">
                                <Upload className="w-4 h-4" />
                                {document.status === DocumentStatusEnum.NEEDS_REVISION ? t('reupload') : t('upload')}
                            </Button>
                        )}
                    </div>
                </div>
            </div>
        </Section>
    );
}

