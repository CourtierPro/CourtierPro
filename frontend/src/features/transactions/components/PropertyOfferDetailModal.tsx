import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { FileText, Download, Trash2, Upload, Calendar, Eye, FileCheck, ChevronDown } from 'lucide-react';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/components/ui/tabs';
import { Separator } from '@/shared/components/ui/separator';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '@/shared/components/ui/collapsible';

import { useUpdatePropertyOffer, useUploadPropertyOfferDocument, useDeleteOfferDocument } from '@/features/transactions/api/mutations';
import type { PropertyOffer, BuyerOfferStatus, OfferDocument } from '@/shared/api/types';

interface PropertyOfferDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    offer: PropertyOffer | null;
    propertyId: string;
    transactionId: string;
    isReadOnly?: boolean;
}

const offerSchema = z.object({
    offerAmount: z.string().min(1, 'offerAmountRequired').refine(
        (val) => parseFloat(val) > 0,
        { message: 'offerAmountMustBePositive' }
    ),
    expiryDate: z.string().optional(),
    status: z.enum(['OFFER_MADE', 'COUNTERED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'WITHDRAWN']),
    notes: z.string().optional(),
});

type PropertyOfferFormData = z.infer<typeof offerSchema>;

const BUYER_OFFER_STATUSES: BuyerOfferStatus[] = [
    'OFFER_MADE',
    'COUNTERED',
    'ACCEPTED',
    'DECLINED',
    'EXPIRED',
    'WITHDRAWN',
];

const statusVariantMap: Record<BuyerOfferStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    OFFER_MADE: 'secondary',
    COUNTERED: 'secondary',
    ACCEPTED: 'default',
    DECLINED: 'destructive',
    EXPIRED: 'outline',
    WITHDRAWN: 'outline',
};

export function PropertyOfferDetailModal({
    isOpen,
    onClose,
    offer,
    propertyId,
    transactionId,
    isReadOnly = false,
}: PropertyOfferDetailModalProps) {
    const { t } = useTranslation('transactions');
    const { mutate: updateOffer, isPending: isUpdating } = useUpdatePropertyOffer();
    const { mutate: uploadDocument, isPending: isUploading } = useUploadPropertyOfferDocument();
    const { mutate: deleteDocument, isPending: isDeletingDoc } = useDeleteOfferDocument();

    const [isEditing, setIsEditing] = useState(false);

    const form = useForm<PropertyOfferFormData>({
        resolver: zodResolver(offerSchema),
    });

    useEffect(() => {
        if (offer) {
            form.reset({
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

    const formatDate = (dateString?: string) => {
        if (!dateString) return '-';
        return format(new Date(dateString), 'MMM d, yyyy');
    };

    const handleUpdate = (data: PropertyOfferFormData) => {
        updateOffer(
            {
                propertyId,
                propertyOfferId: offer.propertyOfferId,
                transactionId,
                data: {
                    offerAmount: parseFloat(data.offerAmount),
                    expiryDate: data.expiryDate || undefined,
                    status: data.status,
                    notes: data.notes || undefined,
                },
            },
            {
                onSuccess: () => {
                    toast.success(t('offerUpdated'));
                    setIsEditing(false);
                },
                onError: (error) => {
                    toast.error(t('errorUpdatingOffer') + ': ' + error.message);
                },
            }
        );
    };

    const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            const file = e.target.files[0];
            uploadDocument(
                {
                    propertyId,
                    propertyOfferId: offer.propertyOfferId,
                    transactionId,
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
                    propertyOfferId: offer.propertyOfferId,
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

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="max-w-xl">
                <DialogHeader>
                    <DialogTitle>
                        {t('offerRound', { round: offer.offerRound })} - {t('details')}
                    </DialogTitle>
                </DialogHeader>

                <Tabs defaultValue="details" className="w-full">
                    <TabsList className="grid w-full grid-cols-2">
                        <TabsTrigger value="details">{t('details')}</TabsTrigger>
                        <TabsTrigger value="documents" className="gap-2">
                            {t('documents')}
                            {offer.documents && offer.documents.length > 0 && (
                                <Badge variant="secondary" className="px-1 py-0 h-5 text-xs">
                                    {offer.documents.length}
                                </Badge>
                            )}
                        </TabsTrigger>
                    </TabsList>

                    <TabsContent value="details" className="py-4">
                        {isEditing ? (
                            <Form {...form}>
                                <form onSubmit={form.handleSubmit(handleUpdate)} className="space-y-4">
                                    <div className="grid grid-cols-2 gap-4">
                                        <FormField
                                            control={form.control}
                                            name="offerAmount"
                                            render={({ field }) => (
                                                <FormItem>
                                                    <FormLabel>{t('offerAmount')}</FormLabel>
                                                    <FormControl>
                                                        <Input {...field} type="number" />
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
                                    </div>

                                    <FormField
                                        control={form.control}
                                        name="status"
                                        render={({ field }) => (
                                            <FormItem>
                                                <FormLabel>{t('status')}</FormLabel>
                                                <Select onValueChange={field.onChange} value={field.value}>
                                                    <FormControl>
                                                        <SelectTrigger>
                                                            <SelectValue />
                                                        </SelectTrigger>
                                                    </FormControl>
                                                    <SelectContent>
                                                        {BUYER_OFFER_STATUSES.map((status) => (
                                                            <SelectItem key={status} value={status}>
                                                                {t(`buyerOfferStatuses.${status}`)}
                                                            </SelectItem>
                                                        ))}
                                                    </SelectContent>
                                                </Select>
                                                <FormMessage />
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
                                                    <Textarea {...field} placeholder={t('brokerNotesPlaceholder')} className="h-24" />
                                                </FormControl>
                                                <FormMessage />
                                            </FormItem>
                                        )}
                                    />

                                    <div className="flex justify-end gap-2 pt-2">
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
                                <div className="grid grid-cols-2 gap-4">
                                    <div className="space-y-1">
                                        <span className="text-sm text-muted-foreground">{t('offerAmount')}</span>
                                        <p className="font-semibold text-lg">{formatCurrency(offer.offerAmount)}</p>
                                    </div>
                                    <div className="space-y-1">
                                        <span className="text-sm text-muted-foreground">{t('expiryDate')}</span>
                                        <p className="font-medium flex items-center gap-2">
                                            <Calendar className="w-4 h-4 text-muted-foreground" />
                                            {formatDate(offer.expiryDate)}
                                        </p>
                                    </div>
                                </div>

                                <div>
                                    <span className="text-sm text-muted-foreground block mb-1">{t('status')}</span>
                                    <Badge variant={statusVariantMap[offer.status]}>
                                        {t(`buyerOfferStatuses.${offer.status}`)}
                                    </Badge>
                                </div>

                                {offer.notes && (
                                    <>
                                        <Separator />
                                        <div>
                                            <span className="text-sm text-muted-foreground block mb-1">
                                                {isReadOnly ? t('notes') : t('brokerNotes')}
                                            </span>
                                            <div className="bg-muted/50 p-3 rounded-md text-sm whitespace-pre-wrap">
                                                {offer.notes}
                                            </div>
                                        </div>
                                    </>
                                )}

                                {/* Linked Conditions */}
                                {offer.conditions && offer.conditions.length > 0 && (
                                    <>
                                        <Separator />
                                        <div>
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
                                    </>
                                )}

                                {!isReadOnly && (
                                    <div className="pt-4 flex justify-end">
                                        <Button onClick={() => setIsEditing(true)} variant="outline">
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
                                        id="document-upload"
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
                                        <label htmlFor="document-upload">
                                            <Upload className="w-4 h-4 mr-2" />
                                            {isUploading ? t('uploading') : t('uploadDocument')}
                                        </label>
                                    </Button>
                                </div>
                            </div>
                        )}

                        {!offer.documents || offer.documents.length === 0 ? (
                            <div className="text-center py-8 text-muted-foreground border-2 border-dashed rounded-lg">
                                <FileText className="w-8 h-8 mx-auto mb-2 opacity-50" />
                                <p>{t('noDocumentsAttached')}</p>
                            </div>
                        ) : (
                            <div className="space-y-2">
                                {offer.documents.map((doc) => (
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
                                                    {doc.createdAt ? format(new Date(doc.createdAt), 'MMM d, yyyy') : '-'} • {doc.sizeBytes ? (doc.sizeBytes / 1024).toFixed(0) : ((doc.fileSize ?? 0) / 1024).toFixed(0)} KB
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
                </Tabs>
            </DialogContent>
        </Dialog>
    );
}
