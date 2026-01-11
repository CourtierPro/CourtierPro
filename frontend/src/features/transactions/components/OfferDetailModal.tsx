import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
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
import { useUpdateOffer, useRemoveOffer } from '@/features/transactions/api/mutations';
import type { Offer, ReceivedOfferStatus } from '@/shared/api/types';

interface OfferDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    offer: Offer | null;
    transactionId: string;
    isReadOnly?: boolean;
}

const offerSchema = z.object({
    buyerName: z.string().min(1, 'buyerNameRequired'),
    offerAmount: z.string().optional(),
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

    const [isEditing, setIsEditing] = useState(false);
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

    const form = useForm<OfferFormData>({
        resolver: zodResolver(offerSchema),
    });

    // Reset form when offer changes
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

    const formatCurrency = (amount?: number) => {
        if (!amount) return '-';
        return new Intl.NumberFormat('en-CA', {
            style: 'currency',
            currency: 'CAD',
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const onSubmit = (data: OfferFormData) => {
        if (!offer) return;

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
                    onClose();
                },
                onError: (error) => {
                    toast.error(t('errorUpdatingOffer') + ': ' + error.message);
                },
            }
        );
    };

    const handleDelete = () => {
        if (!offer) return;

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

    const handleClose = () => {
        setIsEditing(false);
        onClose();
    };

    if (!offer) return null;

    return (
        <>
            <Dialog open={isOpen} onOpenChange={handleClose}>
                <DialogContent className="max-w-md">
                    <DialogHeader>
                        <DialogTitle>
                            {isEditing ? t('editOffer') : t('offerDetails')}
                        </DialogTitle>
                    </DialogHeader>

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
                                            <FormMessage>{form.formState.errors.buyerName?.message && t(form.formState.errors.buyerName.message)}</FormMessage>
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={form.control}
                                    name="offerAmount"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{t('offerAmount')}</FormLabel>
                                            <FormControl>
                                                <Input {...field} type="number" placeholder="0" />
                                            </FormControl>
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
                                        {t('edit')}
                                    </Button>
                                </div>
                            )}
                        </div>
                    )}
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
