import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Upload, X, FileText } from 'lucide-react';
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
import { useAddOffer, useUploadOfferDocument } from '@/features/transactions/api/mutations';
import { ConditionSelector } from './ConditionSelector';
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

export function AddOfferModal({ isOpen, onClose, transactionId }: AddOfferModalProps) {
    const { t } = useTranslation('transactions');
    const { mutateAsync: addOffer, isPending: isAddingOffer } = useAddOffer();
    const { mutateAsync: uploadDocument, isPending: isUploading } = useUploadOfferDocument();
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [selectedConditionIds, setSelectedConditionIds] = useState<string[]>([]);

    const isPending = isAddingOffer || isUploading;

    const form = useForm<OfferFormData>({
        resolver: zodResolver(offerSchema),
        defaultValues: {
            buyerName: '',
            offerAmount: '',
            expiryDate: '',
            status: 'PENDING',
            notes: '',
        },
    });

    const onSubmit = async (data: OfferFormData) => {
        try {
            // Create the offer first
            const createdOffer = await addOffer({
                transactionId,
                data: {
                    buyerName: data.buyerName,
                    offerAmount: data.offerAmount ? parseFloat(data.offerAmount) : undefined,
                    expiryDate: data.expiryDate || undefined,
                    status: data.status,
                    notes: data.notes || undefined,
                    conditionIds: selectedConditionIds.length > 0 ? selectedConditionIds : undefined,
                },
            });

            // If a file was selected, upload it to the newly created offer
            if (selectedFile && createdOffer.offerId) {
                try {
                    await uploadDocument({
                        transactionId,
                        offerId: createdOffer.offerId,
                        file: selectedFile,
                    });
                    toast.success(t('offerAdded'));
                } catch {
                    // Offer was created but document upload failed
                    toast.warning(t('offerAdded') + ' - ' + t('errorUploadingDocument'));
                }
            } else {
                toast.success(t('offerAdded'));
            }

            form.reset();
            setSelectedFile(null);
            setSelectedConditionIds([]);
            onClose();
        } catch (error) {
            toast.error(t('errorAddingOffer') + ': ' + (error as Error).message);
        }
    };

    const handleClose = () => {
        form.reset();
        setSelectedFile(null);
        onClose();
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            setSelectedFile(e.target.files[0]);
        }
    };

    const handleRemoveFile = () => {
        setSelectedFile(null);
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

                        {/* File Upload */}
                        <div className="space-y-2">
                            <FormLabel>{t('attachOfferDocument')}</FormLabel>
                            {selectedFile ? (
                                <div className="flex items-center gap-2 p-3 border rounded-md bg-muted/30">
                                    <FileText className="w-4 h-4 text-primary" />
                                    <span className="text-sm flex-1 truncate">{selectedFile.name}</span>
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="h-6 w-6"
                                        onClick={handleRemoveFile}
                                    >
                                        <X className="w-4 h-4" />
                                    </Button>
                                </div>
                            ) : (
                                <div className="relative">
                                    <input
                                        type="file"
                                        id="offer-document-upload"
                                        className="hidden"
                                        onChange={handleFileChange}
                                        accept=".pdf,.doc,.docx,.jpg,.jpeg,.png"
                                    />
                                    <Button
                                        type="button"
                                        variant="outline"
                                        size="sm"
                                        asChild
                                        className="cursor-pointer w-full"
                                    >
                                        <label htmlFor="offer-document-upload">
                                            <Upload className="w-4 h-4 mr-2" />
                                            {t('uploadDocument')}
                                        </label>
                                    </Button>
                                </div>
                            )}
                        </div>

                        {/* Condition Selection */}
                        <ConditionSelector
                            transactionId={transactionId}
                            selectedConditionIds={selectedConditionIds}
                            onChange={setSelectedConditionIds}
                            disabled={isPending}
                            showCreateButton
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

