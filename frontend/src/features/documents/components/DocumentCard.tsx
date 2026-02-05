import { useState, useCallback, useRef, useEffect } from "react";
import { Link } from "react-router-dom";
import { useDropzone } from "react-dropzone";
import { Section } from "@/shared/components/branded/Section";
import { type Document, DocumentStatusEnum, DocumentTypeEnum, DocumentFlowEnum } from "@/features/documents/types";
import { format } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { FileText, Upload, CheckCircle, Clock, File, Eye, Loader2, Send, Trash2, Share, PenLine } from "lucide-react";
import { useTranslation } from "react-i18next";
import { getDocumentDownloadUrl } from "@/features/documents/api/documentsApi";
import { formatDocumentTitle } from "../utils/formatDocumentTitle";
import { toast } from "sonner";

interface DocumentCardProps {
    document: Document;
    onUpload?: (document: Document, file?: File) => void;
    onReview?: (document: Document) => void;
    onEdit?: (document: Document) => void;
    onSendRequest?: (document: Document) => void;
    onShare?: (document: Document) => void;
    onDelete?: (document: Document) => void;
    isFocused?: boolean;
    showBrokerNotes?: boolean;
}

export function DocumentCard({ document, onUpload, onReview, onEdit, onSendRequest, onShare, onDelete, isFocused, showBrokerNotes = true }: DocumentCardProps) {
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
            case DocumentStatusEnum.DRAFT: return "outline";
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
        if (document.versions.length === 0) return;

        const latestDoc = document.versions[document.versions.length - 1];
        setIsLoadingView(true);

        try {
            const url = await getDocumentDownloadUrl(
                document.transactionRef.transactionId,
                document.documentId,
                latestDoc.versionId
            );
            window.open(url, '_blank');
        } catch {
            toast.error(t('errors.viewFailed', 'Failed to load document'));
        } finally {
            setIsLoadingView(false);
        }
    };

    const handleDownloadToSign = async () => {
        const brokerVersions = document.versions.filter(v => v.uploadedBy.uploaderType === 'BROKER');
        if (brokerVersions.length === 0) return;
        const latestBrokerVersion = brokerVersions[brokerVersions.length - 1];
        setIsLoadingView(true);
        try {
            const url = await getDocumentDownloadUrl(
                document.transactionRef.transactionId,
                document.documentId,
                latestBrokerVersion.versionId
            );
            window.open(url, '_blank');
        } catch {
            toast.error(t('errors.viewFailed'));
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
                        <div className="flex items-center gap-2">
                            {document.requiresSignature && (
                                <Badge variant="outline" className="text-xs bg-amber-50 text-amber-700 dark:bg-amber-900/20 dark:text-amber-300 border-amber-200 dark:border-amber-700 gap-1">
                                    <PenLine className="w-3 h-3" />
                                    {t('signatureRequired')}
                                </Badge>
                            )}
                            {document.flow === DocumentFlowEnum.UPLOAD && document.status !== DocumentStatusEnum.DRAFT && (
                                <Badge variant="outline" className="text-xs bg-blue-50 text-blue-700 dark:bg-blue-900/20 dark:text-blue-300 border-blue-200 dark:border-blue-700">
                                    {t('flow.UPLOAD', 'Shared')}
                                </Badge>
                            )}
                            <Badge variant={getStatusVariant(document.status)}>
                                {t(`status.${document.status}`, document.status)}
                            </Badge>
                        </div>

                        <div className="flex gap-2">
                            {onEdit && (
                                <Button size="sm" variant="outline" onClick={() => onEdit(document)} className="gap-2">
                                    ✏️ {t('edit', 'Edit')}
                                </Button>
                            )}

                            {/* Delete button - only for DRAFT documents */}
                            {document.status === DocumentStatusEnum.DRAFT && onDelete && (
                                <Button size="sm" variant="outline" onClick={() => onDelete(document)} className="gap-2 text-destructive hover:bg-destructive hover:text-destructive-foreground">
                                    <Trash2 className="w-4 h-4" />
                                    {t('actions.delete', 'Delete')}
                                </Button>
                            )}
                            {document.versions.length > 0 &&
                             !(document.requiresSignature && (document.status === DocumentStatusEnum.REQUESTED || document.status === DocumentStatusEnum.NEEDS_REVISION)) && (
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

                            {/* Send Request button - only for REQUEST flow DRAFT documents */}
                            {document.status === DocumentStatusEnum.DRAFT && document.flow !== DocumentFlowEnum.UPLOAD && onSendRequest && (
                                <Button size="sm" onClick={() => onSendRequest(document)} className="gap-2 bg-blue-500 hover:bg-blue-600">
                                    <Send className="w-4 h-4" />
                                    {t('sendRequest', 'Send Request')}
                                </Button>
                            )}

                            {/* Share button - only for UPLOAD flow DRAFT documents */}
                            {document.status === DocumentStatusEnum.DRAFT && document.flow === DocumentFlowEnum.UPLOAD && onShare && (
                                <Button size="sm" onClick={() => onShare(document)} className="gap-2 bg-blue-500 hover:bg-blue-600">
                                    <Share className="w-4 h-4" />
                                    {t('actions.share', 'Share')}
                                </Button>
                            )}

                            {document.status === DocumentStatusEnum.SUBMITTED && onReview && (
                                <Button size="sm" onClick={() => onReview(document)} className="gap-2 bg-orange-500 hover:bg-orange-600">
                                    <CheckCircle className="w-4 h-4" />
                                    {t('review', 'Review')}
                                </Button>
                            )}

                            {/* Download to Sign button - for signature requests in REQUESTED/NEEDS_REVISION status */}
                            {document.requiresSignature && document.versions.length > 0 &&
                             (document.status === DocumentStatusEnum.REQUESTED || document.status === DocumentStatusEnum.NEEDS_REVISION) && (
                                <Button
                                    size="sm"
                                    variant="outline"
                                    onClick={handleDownloadToSign}
                                    disabled={isLoadingView}
                                    className="gap-2"
                                >
                                    {isLoadingView ? (
                                        <Loader2 className="w-4 h-4 animate-spin" />
                                    ) : (
                                        <PenLine className="w-4 h-4" />
                                    )}
                                    {t('downloadToSign')}
                                </Button>
                            )}

                            {/* Upload button logic based on flow and status */}
                            {(() => {
                                // For REQUEST flow: show Upload for REQUESTED, NEEDS_REVISION
                                if (document.flow !== DocumentFlowEnum.UPLOAD) {
                                    if ((document.status === DocumentStatusEnum.REQUESTED || document.status === DocumentStatusEnum.NEEDS_REVISION) && onUpload) {
                                        return (
                                            <Button size="sm" onClick={() => onUpload(document)} className="gap-2">
                                                <Upload className="w-4 h-4" />
                                                {document.status === DocumentStatusEnum.NEEDS_REVISION ? t('reupload') : t('upload')}
                                            </Button>
                                        );
                                    }
                                    return null;
                                }

                                // For UPLOAD flow: show Upload/Update for DRAFT status
                                if (document.status === DocumentStatusEnum.DRAFT && onUpload) {
                                    const hasFile = document.versions.length > 0;
                                    return (
                                        <Button size="sm" onClick={() => onUpload(document)} className="gap-2">
                                            <Upload className="w-4 h-4" />
                                            {hasFile ? t('actions.updateFile', 'Update File') : t('upload')}
                                        </Button>
                                    );
                                }

                                return null;
                            })()}
                        </div>
                    </div >
                </div >
            </Section >
        </div >
    );
}
