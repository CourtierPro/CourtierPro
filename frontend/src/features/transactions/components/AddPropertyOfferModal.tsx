import { useState, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Upload, FileText, X } from 'lucide-react';
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
import { Label } from '@/shared/components/ui/label';
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
import { useAddPropertyOffer, useUploadPropertyOfferDocument } from '@/features/transactions/api/mutations';
import { ConditionSelector } from './ConditionSelector';
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

export function AddPropertyOfferModal({ isOpen, onClose, transactionId, propertyId }: AddPropertyOfferModalProps) {
    const { t } = useTranslation('transactions');
    const { mutate: addOffer, isPending } = useAddPropertyOffer();
    const { mutate: uploadDocument, isPending: isUploading } = useUploadPropertyOfferDocument();
    const [selectedConditionIds, setSelectedConditionIds] = useState<string[]>([]);
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const form = useForm<PropertyOfferFormData>({
        resolver: zodResolver(offerSchema),
        defaultValues: {
            offerAmount: '',
            expiryDate: '',
            status: 'OFFER_MADE',
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
                    conditionIds: selectedConditionIds.length > 0 ? selectedConditionIds : undefined,
                },
            },
            {
                onSuccess: (newOffer) => {
                    if (selectedFile && newOffer?.propertyOfferId) {
                        uploadDocument(
                            {
                                propertyId,
                                propertyOfferId: newOffer.propertyOfferId,
                                transactionId,
                                file: selectedFile,
                            },
                            {
                                onSuccess: () => {
                                    toast.success(t('offerAdded'));
                                    form.reset();
                                    setSelectedConditionIds([]);
                                    setSelectedFile(null);
                                    onClose();
                                },
                                onError: (error) => {
                                    toast.success(t('offerAdded'));
                                    toast.error(t('errorUploadingDocument') + ': ' + error.message);
                                    form.reset();
                                    setSelectedConditionIds([]);
                                    setSelectedFile(null);
                                    onClose();
                                },
                            }
                        );
                    } else {
                        toast.success(t('offerAdded'));
                        form.reset();
                        setSelectedConditionIds([]);
                        setSelectedFile(null);
                        onClose();
                    }
                },
                onError: (error) => {
                    toast.error(t('errorAddingOffer') + ': ' + error.message);
                },
            }
        );
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            setSelectedFile(e.target.files[0]);
        }
    };

    const handleRemoveFile = () => {
        setSelectedFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleClose = () => {
        form.reset();
        setSelectedConditionIds([]);
        setSelectedFile(null);
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
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

                        {/* Condition Selection */}
                        <ConditionSelector
                            transactionId={transactionId}
                            selectedConditionIds={selectedConditionIds}
                            onChange={setSelectedConditionIds}
                            disabled={isPending || isUploading}
                            showCreateButton
                        />

                        {/* Document Upload */}
                        <div className="space-y-2">
                            <Label>{t('attachOfferDocument')}</Label>
                            <input
                                type="file"
                                ref={fileInputRef}
                                onChange={handleFileChange}
                                className="hidden"
                                id="offer-document-input"
                                disabled={isPending || isUploading}
                            />
                            {selectedFile ? (
                                <div className="flex items-center gap-2 p-3 border rounded-md bg-muted/30">
                                    <FileText className="w-4 h-4 text-muted-foreground flex-shrink-0" />
                                    <span className="text-sm truncate flex-1">{selectedFile.name}</span>
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        className="h-6 w-6"
                                        onClick={handleRemoveFile}
                                        disabled={isPending || isUploading}
                                    >
                                        <X className="w-4 h-4" />
                                    </Button>
                                </div>
                            ) : (
                                <Button
                                    type="button"
                                    variant="outline"
                                    className="w-full"
                                    onClick={() => fileInputRef.current?.click()}
                                    disabled={isPending || isUploading}
                                >
                                    <Upload className="w-4 h-4 mr-2" />
                                    {t('uploadDocument')}
                                </Button>
                            )}
                        </div>

                        <div className="flex justify-end gap-2 pt-4">
                            <Button type="button" variant="outline" onClick={handleClose} disabled={isPending || isUploading}>
                                {t('cancel')}
                            </Button>
                            <Button type="submit" disabled={isPending || isUploading}>
                                {isPending || isUploading ? t('saving') : t('createOffer')}
                            </Button>
                        </div>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
