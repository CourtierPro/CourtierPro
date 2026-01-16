import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { FileText, Download, Trash2, Upload, Eye, CheckCircle, XCircle, ArrowLeftRight, Clock, FileCheck, ChevronDown, Calendar } from 'lucide-react';
import { format } from 'date-fns';
import axiosInstance from '@/shared/api/axiosInstance';

import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from '@/shared/components/ui/dialog';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
import { Badge } from '@/shared/components/ui/badge';
import {
    Form,
    FormControl,
    FormField,
    FormItem,
    FormLabel,
    FormMessage,
} from '@/shared/components/ui/form';
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from '@/shared/components/ui/select';
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/components/ui/tabs';
import { Separator } from '@/shared/components/ui/separator';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/shared/components/ui/collapsible';

import { useUpdateOffer, useRemoveOffer, useUploadOfferDocument, useDeleteOfferDocument } from '@/features/transactions/api/mutations';
import { useOfferDocuments } from '@/features/transactions/api/queries';
import { OfferRevisionList } from './OfferRevisionList';
import type { Offer, ReceivedOfferStatus, OfferDocument, ClientOfferDecision } from '@/shared/api/types';

interface OfferDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    offer: Offer | null;
    transactionId: string;
    isReadOnly?: boolean;
    clientId?: string;
}

const offerSchema = z.object({
    buyerName: z.string().min(1, 'buyerNameRequired'),
    offerAmount: z.string().min(1, 'offerAmountRequired').refine(
        (val) => parseFloat(val) > 0,
        { message: 'offerAmountMustBePositive' }
    ),
    expiryDate: z.string().optional(),
    status: z.enum(['PENDING', 'UNDER_REVIEW', 'COUNTERED', 'ACCEPTED', 'DECLINED']),
    notes: z.string().optional(),
});

type OfferFormData = z.infer<typeof offerSchema>;

const OFFER_STATUSES: ReceivedOfferStatus[] = [
    'PENDING',
    'UNDER_REVIEW',
    'COUNTERED',
    'ACCEPTED',
    'DECLINED',
];

const statusVariantMap: Record<ReceivedOfferStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    UNDER_REVIEW: 'secondary',
    COUNTERED: 'outline',
    ACCEPTED: 'default',
    DECLINED: 'destructive',
};

const decisionIconMap: Record<ClientOfferDecision, React.ReactNode> = {
    ACCEPT: <CheckCircle className="w-4 h-4" />,
    DECLINE: <XCircle className="w-4 h-4" />,
    COUNTER: <ArrowLeftRight className="w-4 h-4" />,
};

const decisionVariantMap: Record<ClientOfferDecision, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    ACCEPT: 'default',
    DECLINE: 'destructive',
    COUNTER: 'outline',
};


export function OfferDetailModal({
    isOpen,
    onClose,
    offer,
    transactionId,
    isReadOnly = false,
    clientId,
}: OfferDetailModalProps) {
    const { t } = useTranslation('transactions');
    const { mutate: updateOffer, isPending: isUpdating } = useUpdateOffer();
    const { mutate: removeOffer, isPending: isDeleting } = useRemoveOffer();
    const { mutate: uploadDocument, isPending: isUploading } = useUploadOfferDocument();
    const { mutate: deleteDocument, isPending: isDeletingDoc } = useDeleteOfferDocument();
    // Fetch documents - use client endpoint if clientId is provided
    const { data: documents } = useOfferDocuments(transactionId, offer?.offerId || '', clientId);

    const [isEditing, setIsEditing] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

    const form = useForm<OfferFormData>({
        resolver: zodResolver(offerSchema),
    });

    useEffect(() => {
        if (offer) {
            form.reset({
                buyerName: offer.buyerName,
                offerAmount: offer.offerAmount?.toString() || '',
                expiryDate: offer.expiryDate ? format(new Date(offer.expiryDate), 'yyyy-MM-dd') : '',
                status: offer.status,
                notes: offer.notes || '',
            });
        }
    }, [offer, form]);

    if (!offer) return null;

    const formatCurrency = (amount?: number) => {
        if (!amount) return '-';
        return new Intl.NumberFormat('en-CA', {
            style: 'currency',
            currency: 'CAD',
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const onSubmit = (data: OfferFormData) => {
        updateOffer(
            {
                transactionId,
                offerId: offer.offerId,
                data: {
                    buyerName: data.buyerName,
                    offerAmount: data.offerAmount ? parseFloat(data.offerAmount) : undefined,
                    expiryDate: data.expiryDate || undefined,
                    status: data.status,
                    notes: data.notes || undefined,
                },
            },
            {
                onSuccess: () => {
                    toast.success(t('offerUpdated'));
                    setIsEditing(false);
                    // Don't close immediately in case they want to upload docs
                },
                onError: (error) => {
                    toast.error(t('errorUpdatingOffer') + ': ' + error.message);
                },
            }
        );
    };

    const handleDelete = () => {
        removeOffer(
            { transactionId, offerId: offer.offerId },
            {
                onSuccess: () => {
                    toast.success(t('offerRemoved'));
                    setShowDeleteConfirm(false);
                    onClose();
                },
                onError: (error) => {
                    toast.error(t('errorRemovingOffer') + ': ' + error.message);
                },
            }
        );
    };

    const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            uploadDocument(
                {
                    transactionId,
                    offerId: offer.offerId,
                    file,
                },
                {
                    onSuccess: () => {
                        toast.success(t('documentUploaded'));
                    },
                    onError: (error) => {
                        toast.error(t('errorUploadingDocument') + ': ' + error.message);
                    },
                }
            );
        }
    };

    const handleDownload = async (document: OfferDocument) => {
        try {
            const res = await axiosInstance.get<string>(`/transactions/documents/${document.documentId}/download`);
            const url = res.data;
            if (url) {
                window.open(url, '_blank');
            }
        } catch {
            toast.error(t('errorDownloadingDocument'));
        }
    };

    const handleDeleteDocument = (documentId: string) => {
        if (confirm(t('confirmDeleteDocument'))) {
            deleteDocument(
                {
                    documentId,
                    transactionId
                },
                {
                    onSuccess: () => {
                        toast.success(t('documentDeleted'));
                    },
                    onError: (error) => {
                        toast.error(t('errorDeletingDocument') + ': ' + error.message);
                    },
                }
            );
        }
    };

    const handleClose = () => {
        setIsEditing(false);
        onClose();
    };

    return (
        <>
            <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
                <DialogContent className="max-w-xl">
                    <DialogHeader>
                        <DialogTitle>
                            {t('offerDetails')} - {offer.buyerName}
                        </DialogTitle>
                    </DialogHeader>

                    <Tabs defaultValue="details" className="w-full">
                        <TabsList className="grid w-full grid-cols-3">
                            <TabsTrigger value="details">{t('details')}</TabsTrigger>
                            <TabsTrigger value="documents" className="gap-2">
                                {t('documents')}
                                {documents && documents.length > 0 && (
                                    <Badge variant="secondary" className="px-1 py-0 h-5 text-xs">
                                        {documents.length}
                                    </Badge>
                                )}
                            </TabsTrigger>
                            <TabsTrigger value="revisions">{t('revisions')}</TabsTrigger>
                        </TabsList>

                        <TabsContent value="details" className="py-4">
                            {isEditing ? (
                                <Form {...form}>
                                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                                        <FormField
                                            control={form.control}
                                            name="buyerName"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{t('buyerName')} <span className="text-destructive">*</span></FormLabel>
                                                    <FormControl>
                                                        <Input {...field} placeholder={t('buyerNamePlaceholder')} />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            name="offerAmount"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{t('offerAmount')} <span className="text-destructive">*</span></FormLabel>
                                                    <FormControl>
                                                        <Input {...field} type="number" placeholder="0" />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            name="expiryDate"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{t('expiryDate')}</FormLabel>
                                                    <FormControl>
                                                        <Input {...field} type="date" />
                                                    </FormControl>
                                                    <FormMessage />
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            name="status"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{t('offerStatusLabel')}</FormLabel>
                                                    <Select onValueChange={field.onChange} value={field.value}>
                                                        <FormControl>
                                                            <SelectTrigger>
                                                                <SelectValue placeholder={t('selectOfferStatus')} />
                                                            </SelectTrigger>
                                                        </FormControl>
                                                        <SelectContent>
                                                            {OFFER_STATUSES.map((status) => (
                                                                <SelectItem key={status} value={status}>
                                                                    {t(`receivedOfferStatuses.${status}`)}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                </FormItem>
                                            )}
                                        />

                                        <FormField
                                            control={form.control}
                                            name="notes"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{t('brokerNotes')}</FormLabel>
                                                    <FormControl>
                                                        <Textarea
                                                            {...field}
                                                            placeholder={t('brokerNotesPlaceholder')}
                                                            className="h-20"
                                                        />
                                                    </FormControl>
                                                    <p className="text-xs text-muted-foreground">{t('brokerNotesHint')}</p>
                                                </FormItem>
                                            )}
                                        />

                                        <div className="flex justify-end gap-2 pt-4">
                                            <Button type="button" variant="outline" onClick={() => setIsEditing(false)}>
                                                {t('cancel')}
                                            </Button>
                                            <Button type="submit" disabled={isUpdating}>
                                                {isUpdating ? t('saving') : t('saveChanges')}
                                            </Button>
                                        </div>
                                    </form>
                                </Form>
                            ) : (
                                <div className="space-y-4">
                                    <div className="space-y-3">
                                        <div className="flex items-center justify-between">
                                            <span className="text-sm text-muted-foreground">{t('buyerName')}</span>
                                            <span className="font-medium">{offer.buyerName}</span>
                                        </div>
                                        <div className="flex items-center justify-between">
                                            <span className="text-sm text-muted-foreground">{t('offerAmount')}</span>
                                            <span className="font-medium">{formatCurrency(offer.offerAmount)}</span>
                                        </div>
                                        <div className="flex items-center justify-between">
                                            <span className="text-sm text-muted-foreground">{t('status')}</span>
                                            <Badge variant={statusVariantMap[offer.status]}>
                                                {t(`receivedOfferStatuses.${offer.status}`)}
                                            </Badge>
                                        </div>
                                        {offer.expiryDate && (
                                            <div className="flex items-center justify-between">
                                                <span className="text-sm text-muted-foreground">{t('expiryDate')}</span>
                                                <span className="font-medium flex items-center gap-2">
                                                    <Clock className="w-4 h-4 text-muted-foreground" />
                                                    {format(new Date(offer.expiryDate), 'MMM d, yyyy')}
                                                </span>
                                            </div>
                                        )}
                                        {offer.notes && (
                                            <div className="pt-2 border-t">
                                                <span className="text-sm text-muted-foreground block mb-1">
                                                    {isReadOnly ? t('notes') : t('brokerNotes')}
                                                </span>
                                                <p className="text-sm">{offer.notes}</p>
                                            </div>
                                        )}
                                        {/* Linked Conditions */}
                                        {offer.conditions && offer.conditions.length > 0 && (
                                            <div className="pt-3 border-t">
                                                <div className="flex items-center gap-2 mb-3">
                                                    <FileCheck className="w-4 h-4 text-blue-500" />
                                                    <span className="text-sm font-medium">
                                                        {t('linkedConditions')} ({offer.conditions.length})
                                                    </span>
                                                </div>
                                                <div className="space-y-2">
                                                    {offer.conditions.map((condition) => (
                                                        <Collapsible key={condition.conditionId}>
                                                            <CollapsibleTrigger className="w-full group">
                                                                <div className="flex items-center gap-3 p-3 bg-blue-50 dark:bg-blue-950/30 rounded-lg border border-blue-200 dark:border-blue-800 hover:bg-blue-100 dark:hover:bg-blue-950/50 transition-colors cursor-pointer">
                                                                    <div className="flex-1 text-left">
                                                                        <p className="text-sm font-medium text-blue-900 dark:text-blue-100">
                                                                            {condition.customTitle || t(`conditionTypes.${condition.type}`)}
                                                                        </p>
                                                                        <div className="flex items-center gap-3 mt-1">
                                                                            <span className="flex items-center gap-1 text-xs text-blue-700 dark:text-blue-300">
                                                                                <Calendar className="w-3 h-3" />
                                                                                {condition.deadlineDate}
                                                                            </span>
                                                                            <Badge 
                                                                                variant={condition.status === 'SATISFIED' ? 'default' : condition.status === 'FAILED' ? 'destructive' : 'secondary'}
                                                                                className="text-xs px-1.5 py-0"
                                                                            >
                                                                                {t(`conditionStatus.${condition.status}`)}
                                                                            </Badge>
                                                                        </div>
                                                                    </div>
                                                                    <ChevronDown className="w-4 h-4 text-blue-500 transition-transform duration-200 group-data-[state=open]:rotate-180" />
                                                                </div>
                                                            </CollapsibleTrigger>
                                                            <CollapsibleContent>
                                                                <div className="px-3 py-2 mt-1 bg-muted/50 rounded-b-lg border-x border-b border-border text-sm text-muted-foreground">
                                                                    {condition.description || t('conditions.noDescription', 'No description provided.')}
                                                                </div>
                                                            </CollapsibleContent>
                                                        </Collapsible>
                                                    ))}
                                                </div>
                                            </div>
                                        )}
                                    </div>

                                    {/* Show existing client decision if any (historical data) */}
                                    {offer.clientDecision && (
                                        <>
                                            <Separator />
                                            <div className="bg-muted/50 p-4 rounded-lg space-y-2">
                                                <div className="flex items-center gap-2">
                                                    {decisionIconMap[offer.clientDecision]}
                                                    <span className="font-semibold">{t('clientHasDecided')}</span>
                                                </div>
                                                <div className="flex items-center gap-2">
                                                    <Badge variant={decisionVariantMap[offer.clientDecision]}>
                                                        {t(`clientOfferDecisions.${offer.clientDecision}`)}
                                                    </Badge>
                                                    {offer.clientDecisionAt && (
                                                        <span className="text-xs text-muted-foreground">
                                                            {format(new Date(offer.clientDecisionAt), 'MMM d, yyyy HH:mm')}
                                                        </span>
                                                    )}
                                                </div>
                                                {offer.clientDecisionNotes && (
                                                    <p className="text-sm text-muted-foreground mt-2">
                                                        "{offer.clientDecisionNotes}"
                                                    </p>
                                                )}
                                            </div>
                                        </>
                                    )}

                                    {!isReadOnly && (
                                        <div className="flex justify-between gap-2 pt-4 border-t">
                                            <Button
                                                variant="destructive"
                                                onClick={() => setShowDeleteConfirm(true)}
                                            >
                                                {t('remove')}
                                            </Button>
                                            <Button onClick={() => setIsEditing(true)}>
                                                {t('editDetails')}
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            )}
                        </TabsContent>

                        <TabsContent value="documents" className="py-4 space-y-4">
                            {!isReadOnly && (
                                <div className="flex items-center justify-end">
                                    <div className="relative">
                                        <input
                                            type="file"
                                            id="offer-doc-upload"
                                            className="hidden"
                                            onChange={handleFileUpload}
                                            disabled={isUploading}
                                        />
                                        <Button
                                            asChild
                                            variant="outline"
                                            size="sm"
                                            disabled={isUploading}
                                            className="cursor-pointer"
                                        >
                                            <label htmlFor="offer-doc-upload">
                                                <Upload className="w-4 h-4 mr-2" />
                                                {isUploading ? t('uploading') : t('uploadDocument')}
                                            </label>
                                        </Button>
                                    </div>
                                </div>
                            )}

                            {!documents || documents.length === 0 ? (
                                <div className="text-center py-8 text-muted-foreground border-2 border-dashed rounded-lg">
                                    <FileText className="w-8 h-8 mx-auto mb-2 opacity-50" />
                                    <p>{t('noDocumentsAttached')}</p>
                                </div>
                            ) : (
                                <div className="space-y-2">
                                    {documents.map((doc) => (
                                        <div
                                            key={doc.documentId}
                                            className="flex items-center justify-between p-3 border rounded-md bg-card hover:bg-muted/30 transition-colors"
                                        >
                                            <div className="flex items-center gap-3 min-w-0">
                                                <div className="p-2 bg-primary/10 rounded">
                                                    <FileText className="w-4 h-4 text-primary" />
                                                </div>
                                                <div className="min-w-0">
                                                    <p className="font-medium text-sm truncate">{doc.fileName}</p>
                                                    <p className="text-xs text-muted-foreground">
                                                        {doc.createdAt ? format(new Date(doc.createdAt), 'MMM d, yyyy') : '-'} • {doc.sizeBytes ? (doc.sizeBytes / 1024).toFixed(0) : (doc.fileSize ? (doc.fileSize / 1024).toFixed(0) : '?')} KB
                                                    </p>
                                                </div>
                                            </div>
                                            <div className="flex items-center gap-1">
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8"
                                                    onClick={() => handleDownload(doc)}
                                                    title={t('view')}
                                                >
                                                    <Eye className="w-4 h-4" />
                                                </Button>
                                                <Button
                                                    variant="ghost"
                                                    size="icon"
                                                    className="h-8 w-8"
                                                    onClick={() => handleDownload(doc)}
                                                    title={t('download')}
                                                >
                                                    <Download className="w-4 h-4" />
                                                </Button>
                                                {!isReadOnly && (
                                                    <Button
                                                        variant="ghost"
                                                        size="icon"
                                                        className="h-8 w-8 text-destructive hover:text-destructive"
                                                        onClick={() => handleDeleteDocument(doc.documentId)}
                                                        disabled={isDeletingDoc}
                                                        title={t('delete')}
                                                    >
                                                        <Trash2 className="w-4 h-4" />
                                                    </Button>
                                                )}
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </TabsContent>

                        <TabsContent value="revisions" className="py-4">
                            <OfferRevisionList transactionId={transactionId} offerId={offer.offerId} />
                        </TabsContent>
                    </Tabs>
                </DialogContent>
            </Dialog>

            <AlertDialog open={showDeleteConfirm} onOpenChange={setShowDeleteConfirm}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t('confirmDeleteOffer')}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t('confirmDeleteOfferDescription')}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t('cancel')}</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDelete}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            {isDeleting ? t('deleting') : t('confirmDelete')}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
}
