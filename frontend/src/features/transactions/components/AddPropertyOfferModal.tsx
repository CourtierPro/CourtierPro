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
    DialogDescription,
} from '@/shared/components/ui/dialog';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
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
import { useAddPropertyOffer } from '@/features/transactions/api/mutations';
import type { BuyerOfferStatus } from '@/shared/api/types';

interface AddPropertyOfferModalProps {
    isOpen: boolean;
    onClose: () => void;
    transactionId: string;
    propertyId: string;
}

const offerSchema = z.object({
    offerAmount: z.string().min(1, 'offerAmountRequired').refine(
        (val) => parseFloat(val) > 0,
        { message: 'offerAmountMustBePositive' }
    ),
    expiryDate: z.string().optional(),
    status: z.enum(['DRAFT', 'SUBMITTED', 'COUNTERED', 'ACCEPTED', 'DECLINED', 'EXPIRED', 'WITHDRAWN']),
    notes: z.string().optional(),
});

type PropertyOfferFormData = z.infer<typeof offerSchema>;

const BUYER_OFFER_STATUSES: BuyerOfferStatus[] = [
    'DRAFT',
    'SUBMITTED',
    'COUNTERED',
    'ACCEPTED',
    'DECLINED',
    'EXPIRED',
    'WITHDRAWN',
];

export function AddPropertyOfferModal({ isOpen, onClose, transactionId, propertyId }: AddPropertyOfferModalProps) {
    const { t } = useTranslation('transactions');
    const { mutate: addOffer, isPending } = useAddPropertyOffer();

    const form = useForm<PropertyOfferFormData>({
        resolver: zodResolver(offerSchema),
        defaultValues: {
            offerAmount: '',
            expiryDate: '',
            status: 'SUBMITTED',
            notes: '',
        },
    });

    const onSubmit = (data: PropertyOfferFormData) => {
        addOffer(
            {
                transactionId,
                propertyId,
                data: {
                    offerAmount: parseFloat(data.offerAmount),
                    expiryDate: data.expiryDate || undefined,
                    status: data.status,
                    notes: data.notes || undefined,
                },
            },
            {
                onSuccess: () => {
                    toast.success(t('offerAdded'));
                    form.reset();
                    onClose();
                },
                onError: (error) => {
                    toast.error(t('errorAddingOffer') + ': ' + error.message);
                },
            }
        );
    };

    const handleClose = () => {
        form.reset();
        onClose();
    };

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
            <DialogContent className="max-w-md">
                <DialogHeader>
                    <DialogTitle>{t('makePropertyOffer')}</DialogTitle>
                    <DialogDescription>
                        {t('makePropertyOfferDescription')}
                    </DialogDescription>
                </DialogHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        <FormField
                            control={form.control}
                            name="offerAmount"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>{t('offerAmount')} <span className="text-destructive">*</span></FormLabel>
                                    <FormControl>
                                        <Input {...field} type="number" placeholder="0" />
                                    </FormControl>
                                    <FormMessage>{form.formState.errors.offerAmount?.message && t(form.formState.errors.offerAmount.message)}</FormMessage>
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
                                    <FormMessage>{form.formState.errors.expiryDate?.message && t(form.formState.errors.expiryDate.message)}</FormMessage>
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
                                            {BUYER_OFFER_STATUSES.map((status) => (
                                                <SelectItem key={status} value={status}>
                                                    {t(`buyerOfferStatuses.${status}`)}
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
                            <Button type="button" variant="outline" onClick={handleClose} disabled={isPending}>
                                {t('cancel')}
                            </Button>
                            <Button type="submit" disabled={isPending}>
                                {isPending ? t('saving') : t('createOffer')}
                            </Button>
                        </div>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
