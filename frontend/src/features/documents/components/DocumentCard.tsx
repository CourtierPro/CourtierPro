import { useState, useCallback, useRef, useEffect } from "react";
import { Link } from "react-router-dom";
import { useDropzone } from "react-dropzone";
import { Section } from "@/shared/components/branded/Section";
import { type DocumentRequest, DocumentStatusEnum, DocumentTypeEnum } from "@/features/documents/types";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { FileText, Upload, CheckCircle, Clock, File, Eye, Loader2 } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getDocumentDownloadUrl } from "@/features/documents/api/documentsApi";
import { formatDocumentTitle } from "../utils/formatDocumentTitle";
import { toast } from "sonner";

interface DocumentCardProps {
    document: DocumentRequest;
    onUpload?: (document: DocumentRequest, file?: File) => void;
    onReview?: (document: DocumentRequest) => void;
    isFocused?: boolean;
    showBrokerNotes?: boolean;
}

export function DocumentCard({ document, onUpload, onReview, isFocused, showBrokerNotes = true }: DocumentCardProps) {
    const { t, i18n } = useTranslation('documents');
    const [isLoadingView, setIsLoadingView] = useState(false);
    const title = formatDocumentTitle(document, t);


    const locale = i18n.language === 'fr' ? fr : enUS;
    const date = document.lastUpdatedAt ? format(new Date(document.lastUpdatedAt), 'PPP', { locale }) : '...';

    const getStatusVariant = (status: DocumentStatusEnum) => {
        switch (status) {
            case DocumentStatusEnum.APPROVED: return "success";
            case DocumentStatusEnum.SUBMITTED: return "secondary";
            case DocumentStatusEnum.NEEDS_REVISION: return "destructive";
            default: return "secondary";
        }
    };

    const getIcon = (type: DocumentTypeEnum) => {
        switch (type) {
            case DocumentTypeEnum.MORTGAGE_APPROVAL:
            case DocumentTypeEnum.MORTGAGE_PRE_APPROVAL:
                return <FileText className="w-5 h-5 text-blue-500 dark:text-blue-400" />;
            case DocumentTypeEnum.ID_VERIFICATION:
                return <CheckCircle className="w-5 h-5 text-purple-500 dark:text-purple-400" />;
            default:
                return <File className="w-5 h-5 text-muted-foreground" />;
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

    const { ref: dropzoneRef, ...rootProps } = getRootProps();

    const sectionRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (isFocused && sectionRef.current) {
            sectionRef.current.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }, [isFocused]);

    const setRefs = useCallback((node: HTMLDivElement | null) => {
        sectionRef.current = node;
        if (dropzoneRef) {
            if (typeof dropzoneRef === 'function') {
                dropzoneRef(node);
            } else if (
                typeof dropzoneRef === 'object' &&
                'current' in dropzoneRef
            ) {
                dropzoneRef.current = node;
            }
        }
    }, [dropzoneRef]);

    return (
        <div
            ref={setRefs}
            className={`rounded-lg transition-all duration-300 ${isFocused ? 'ring-[3px] ring-primary ring-offset-2 shadow-lg shadow-primary/20' : ''}`}
            role="article"
            aria-label={isFocused ? t('focusedDocument', 'Focused document from search') : undefined}
            {...rootProps}
        >
            <input {...getInputProps()} />
            <Section className={`p-4 transition-all hover:shadow-md border relative ${isDragActive ? 'border-primary bg-primary/5' : 'border-border'}`}>
                {isDragActive && (
                    <div className="absolute inset-0 z-10 flex items-center justify-center bg-background/80 rounded-lg backdrop-blur-sm border-2 border-primary border-dashed">
                        <p className="text-primary font-semibold flex items-center gap-2">
                            <Upload className="w-6 h-6" />
                            {t('dropToUpload', 'Drop file to upload')}
                        </p>
                    </div>
                )}
                <div className="flex items-start justify-between">
                    <div className="flex gap-4">
                        <div className="p-2 bg-muted rounded-lg">
                            {getIcon(document.docType)}
                        </div>
                        <div>
                            <h3 className="font-semibold text-foreground">{title}</h3>
                            <div className="flex flex-col gap-1 mt-1">
                                <div className="flex items-center gap-2 text-sm text-muted-foreground">
                                    <Clock className="w-3 h-3" />
                                    <span>{t('lastUpdated')}: {date}</span>
                                </div>
                                <div className="flex items-center gap-1 text-xs text-muted-foreground/80">
                                    <span>Ref:</span>
                                    <Link
                                        to={`/transactions/${document.transactionRef.transactionId}`}
                                        className="hover:underline hover:text-primary transition-colors"
                                        onClick={(e) => e.stopPropagation()}
                                    >
                                        #{document.transactionRef.transactionId.substring(0, 8)}
                                    </Link>
                                </div>
                            </div>
                            {showBrokerNotes && document.brokerNotes && (
                                <p className="mt-2 text-sm text-muted-foreground bg-muted/50 p-2 rounded">
                                    {document.brokerNotes}
                                </p>
                            )}
                        </div>
                    </div>

                    <div className="flex flex-col items-end gap-3">
                        <Badge variant={getStatusVariant(document.status)}>
                            {t(`status.${document.status}`, document.status)}
                        </Badge>

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

                            {document.status === DocumentStatusEnum.SUBMITTED && onReview && (
                                <Button size="sm" onClick={() => onReview(document)} className="gap-2 bg-orange-500 hover:bg-orange-600">
                                    <CheckCircle className="w-4 h-4" />
                                    {t('review', 'Review')}
                                </Button>
                            )}

                            {(document.status === DocumentStatusEnum.REQUESTED || document.status === DocumentStatusEnum.NEEDS_REVISION) && onUpload && (
                                <Button size="sm" onClick={() => onUpload(document)} className="gap-2">
                                    <Upload className="w-4 h-4" />
                                    {document.status === DocumentStatusEnum.NEEDS_REVISION ? t('reupload') : t('upload')}
                                </Button>
                            )}
                        </div>
                    </div >
                </div >
            </Section >
        </div >
    );
}
