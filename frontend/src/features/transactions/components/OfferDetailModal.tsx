import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { FileText, Download, Trash2, Upload } from 'lucide-react';
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

import { useUpdateOffer, useRemoveOffer, useUploadOfferDocument, useDeleteOfferDocument } from '@/features/transactions/api/mutations';
import { useOfferDocuments } from '@/features/transactions/api/queries';
import { OfferRevisionList } from './OfferRevisionList';
import type { Offer, ReceivedOfferStatus, OfferDocument } from '@/shared/api/types';

interface OfferDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    offer: Offer | null;
    transactionId: string;
    isReadOnly?: boolean;
}

const offerSchema = z.object({
    buyerName: z.string().min(1, 'buyerNameRequired'),
    offerAmount: z.string().min(1, 'offerAmountRequired').refine(
        (val) => parseFloat(val) > 0,
        { message: 'offerAmountMustBePositive' }
    ),
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

export function OfferDetailModal({
    isOpen,
    onClose,
    offer,
    transactionId,
    isReadOnly = false,
}: OfferDetailModalProps) {
    const { t } = useTranslation('transactions');
    const { mutate: updateOffer, isPending: isUpdating } = useUpdateOffer();
    const { mutate: removeOffer, isPending: isDeleting } = useRemoveOffer();
    const { mutate: uploadDocument, isPending: isUploading } = useUploadOfferDocument();
    const { mutate: deleteDocument, isPending: isDeletingDoc } = useDeleteOfferDocument();

    // Fetch documents
    const { data: documents } = useOfferDocuments(transactionId, offer?.offerId || '');

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
                                        {offer.notes && (
                                            <div className="pt-2 border-t">
                                                <span className="text-sm text-muted-foreground block mb-1">
                                                    {isReadOnly ? t('notes') : t('brokerNotes')}
                                                </span>
                                                <p className="text-sm">{offer.notes}</p>
                                            </div>
                                        )}
                                    </div>

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
                                                        {format(new Date(doc.uploadedAt), 'MMM d, yyyy')} • {(doc.fileSize ? (doc.fileSize / 1024).toFixed(0) : '?')} KB
                                                    </p>
                                                </div>
                                            </div>
                                            <div className="flex items-center gap-1">
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
