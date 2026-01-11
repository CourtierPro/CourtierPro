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
import { useAddOffer } from '@/features/transactions/api/mutations';
import type { ReceivedOfferStatus } from '@/shared/api/types';

interface AddOfferModalProps {
    isOpen: boolean;
    onClose: () => void;
    transactionId: string;
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

export function AddOfferModal({ isOpen, onClose, transactionId }: AddOfferModalProps) {
    const { t } = useTranslation('transactions');
    const { mutate: addOffer, isPending } = useAddOffer();

    const form = useForm<OfferFormData>({
        resolver: zodResolver(offerSchema),
        defaultValues: {
            buyerName: '',
            offerAmount: '',
            status: 'PENDING',
            notes: '',
        },
    });

    const onSubmit = (data: OfferFormData) => {
        addOffer(
            {
                transactionId,
                data: {
                    buyerName: data.buyerName,
                    offerAmount: data.offerAmount ? parseFloat(data.offerAmount) : undefined,
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
                    <DialogTitle>{t('addOffer')}</DialogTitle>
                    <DialogDescription>
                        {t('noOffersDescription')}
                    </DialogDescription>
                </DialogHeader>

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
                            <Button type="button" variant="outline" onClick={handleClose} disabled={isPending}>
                                {t('cancel')}
                            </Button>
                            <Button type="submit" disabled={isPending}>
                                {isPending ? t('saving') : t('addOffer')}
                            </Button>
                        </div>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
