import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { useForm, useWatch } from 'react-hook-form';
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
import { useAddProperty, useUpdateProperty } from '@/features/transactions/api/mutations';
import type { Property, PropertyOfferStatus, PropertyRequestDTO } from '@/shared/api/types';
import { formatPostalCode, normalizePostalCode } from '@/shared/utils/postal-code';

interface AddPropertyModalProps {
    isOpen: boolean;
    onClose: () => void;
    transactionId: string;
    existingProperty?: Property | null;
    onSuccess?: () => void;
}

const offerStatusOptions: PropertyOfferStatus[] = [
    'OFFER_TO_BE_MADE',
    'OFFER_MADE',
    'COUNTERED',
    'ACCEPTED',
    'DECLINED',
];

const propertyFormSchema = z.object({
    street: z.string().min(1, 'streetRequired'),
    city: z.string().min(1, 'cityRequired'),
    province: z.string().min(1, 'provinceRequired'),
    postalCode: z.string().optional(),
    askingPrice: z.string().optional(),
    offerStatus: z.enum(['OFFER_TO_BE_MADE', 'OFFER_MADE', 'COUNTERED', 'ACCEPTED', 'DECLINED']),
    offerAmount: z.string().optional(),
    centrisNumber: z.string().optional(),
    notes: z.string().optional(),
});

type PropertyFormValues = z.infer<typeof propertyFormSchema>;

export function AddPropertyModal({
    isOpen,
    onClose,
    transactionId,
    existingProperty,
    onSuccess,
}: AddPropertyModalProps) {
    const { t } = useTranslation('transactions');
    const addProperty = useAddProperty();
    const updateProperty = useUpdateProperty();
    const isEditing = !!existingProperty;

    const form = useForm<PropertyFormValues>({
        resolver: zodResolver(propertyFormSchema),
        defaultValues: {
            street: '',
            city: '',
            province: '',
            postalCode: '',
            askingPrice: '',
            offerStatus: 'OFFER_TO_BE_MADE',
            offerAmount: '',
            centrisNumber: '',
            notes: '',
        },
    });

    // Watch offerStatus to conditionally disable offerAmount
    const offerStatus = useWatch({ control: form.control, name: 'offerStatus' });
    const isOfferToBeMade = offerStatus === 'OFFER_TO_BE_MADE';

    // Reset form when modal opens or existingProperty changes
    useEffect(() => {
        if (isOpen) {
            if (existingProperty) {
                const status = existingProperty.offerStatus || 'OFFER_TO_BE_MADE';
                form.reset({
                    street: existingProperty.address?.street || '',
                    city: existingProperty.address?.city || '',
                    province: existingProperty.address?.province || '',
                    postalCode: existingProperty.address?.postalCode || '',
                    askingPrice: existingProperty.askingPrice?.toString() || '',
                    offerStatus: status,
                    offerAmount: status === 'OFFER_TO_BE_MADE' ? '0' : (existingProperty.offerAmount?.toString() || ''),
                    centrisNumber: existingProperty.centrisNumber || '',
                    notes: existingProperty.notes || '',
                });
            } else {
                form.reset({
                    street: '',
                    city: '',
                    province: '',
                    postalCode: '',
                    askingPrice: '',
                    offerStatus: 'OFFER_TO_BE_MADE',
                    offerAmount: '0',
                    centrisNumber: '',
                    notes: '',
                });
            }
        }
    }, [isOpen, existingProperty, form]);

    // When status changes to OFFER_TO_BE_MADE, reset offerAmount to 0
    useEffect(() => {
        if (isOfferToBeMade) {
            form.setValue('offerAmount', '0');
        }
    }, [isOfferToBeMade, form]);

    const onSubmit = async (data: PropertyFormValues) => {
        // If status is OFFER_TO_BE_MADE, offerAmount should be 0 or undefined
        const offerAmountValue = data.offerStatus === 'OFFER_TO_BE_MADE'
            ? undefined
            : (data.offerAmount ? parseFloat(data.offerAmount) : undefined);

        const payload: PropertyRequestDTO = {
            address: {
                street: data.street,
                city: data.city,
                province: data.province,
                postalCode: normalizePostalCode(data.postalCode),
            },
            askingPrice: data.askingPrice ? parseFloat(data.askingPrice) : undefined,
            offerStatus: data.offerStatus,
            offerAmount: offerAmountValue,
            centrisNumber: data.centrisNumber || undefined,
            notes: data.notes || undefined,
        };

        try {
            if (isEditing && existingProperty) {
                await updateProperty.mutateAsync({
                    transactionId,
                    propertyId: existingProperty.propertyId,
                    data: payload,
                });
                toast.success(t('propertyUpdated'));
            } else {
                await addProperty.mutateAsync({
                    transactionId,
                    data: payload,
                });
                toast.success(t('propertyAdded'));
            }

            onSuccess?.();
            onClose();
        } catch {
            toast.error(isEditing ? t('errorUpdatingProperty') : t('errorAddingProperty'));
        }
    };

    const isPending = addProperty.isPending || updateProperty.isPending;

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="max-w-lg max-h-[90vh] overflow-y-auto">
                <DialogHeader>
                    <DialogTitle>
                        {isEditing ? t('editProperty') : t('addProperty')}
                    </DialogTitle>
                    <DialogDescription>
                        {isEditing ? t('editPropertyDescription') : t('addPropertyDescription')}
                    </DialogDescription>
                </DialogHeader>

                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                        {/* Address Section */}
                        <div className="space-y-4">
                            <h4 className="text-sm font-medium text-muted-foreground">{t('propertyAddress')}</h4>

                            <FormField
                                control={form.control}
                                name="street"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('street')} <span className="text-destructive">*</span></FormLabel>
                                        <FormControl>
                                            <Input {...field} placeholder={t('streetPlaceholder')} />
                                        </FormControl>
                                        <FormMessage>{form.formState.errors.street?.message && t(form.formState.errors.street.message)}</FormMessage>
                                    </FormItem>
                                )}
                            />

                            <div className="grid grid-cols-2 gap-4">
                                <FormField
                                    control={form.control}
                                    name="city"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{t('city')} <span className="text-destructive">*</span></FormLabel>
                                            <FormControl>
                                                <Input {...field} placeholder={t('cityPlaceholder')} />
                                            </FormControl>
                                            <FormMessage>{form.formState.errors.city?.message && t(form.formState.errors.city.message)}</FormMessage>
                                        </FormItem>
                                    )}
                                />

                                <FormField
                                    control={form.control}
                                    name="province"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{t('province')} <span className="text-destructive">*</span></FormLabel>
                                            <FormControl>
                                                <Input {...field} placeholder={t('provincePlaceholder')} />
                                            </FormControl>
                                            <FormMessage>{form.formState.errors.province?.message && t(form.formState.errors.province.message)}</FormMessage>
                                        </FormItem>
                                    )}
                                />
                            </div>

                            <FormField
                                control={form.control}
                                name="postalCode"
                                render={({ field }) => (
                                    <FormItem>
                                        <FormLabel>{t('postalCode')}</FormLabel>
                                        <FormControl>
                                            <Input
                                                {...field}
                                                placeholder={t('postalCodePlaceholder')}
                                                maxLength={7}
                                                onChange={(e) => field.onChange(formatPostalCode(e.target.value))}
                                            />
                                        </FormControl>
                                    </FormItem>
                                )}
                            />
                        </div>

                        {/* Centris Number */}
                        <FormField
                            control={form.control}
                            name="centrisNumber"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>{t('centrisNumber')}</FormLabel>
                                    <FormControl>
                                        <Input {...field} placeholder={t('centrisNumberPlaceholder')} />
                                    </FormControl>
                                </FormItem>
                            )}
                        />

                        {/* Status */}
                        <FormField
                            control={form.control}
                            name="offerStatus"
                            render={({ field }) => (
                                <FormItem>
                                    <FormLabel>{t('offerStatusLabel')} <span className="text-destructive">*</span></FormLabel>
                                    <Select onValueChange={field.onChange} value={field.value}>
                                        <FormControl>
                                            <SelectTrigger>
                                                <SelectValue placeholder={t('selectOfferStatus')} />
                                            </SelectTrigger>
                                        </FormControl>
                                        <SelectContent>
                                            {offerStatusOptions.map((status) => (
                                                <SelectItem key={status} value={status}>
                                                    {t(`propertyOfferStatuses.${status}`)}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </FormItem>
                            )}
                        />

                        {/* Pricing Section */}
                        <div className="space-y-4 pt-4 border-t border-border">
                            <h4 className="text-sm font-medium text-muted-foreground">{t('pricingAndOffer')}</h4>

                            <div className="grid grid-cols-2 gap-4">
                                <FormField
                                    control={form.control}
                                    name="askingPrice"
                                    render={({ field }) => (
                                        <FormItem>
                                            <FormLabel>{t('askingPrice')}</FormLabel>
                                            <FormControl>
                                                <Input {...field} type="number" placeholder="500000" />
                                            </FormControl>
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
                                                <Input
                                                    {...field}
                                                    type="number"
                                                    placeholder="480000"
                                                    disabled={isOfferToBeMade}
                                                    className={isOfferToBeMade ? 'bg-muted cursor-not-allowed' : ''}
                                                />
                                            </FormControl>
                                        </FormItem>
                                    )}
                                />
                            </div>
                        </div>

                        {/* Notes Section */}
                        <div className="space-y-4 pt-4 border-t border-border">
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
                        </div>

                        {/* Actions */}
                        <div className="flex justify-end gap-2 pt-4">
                            <Button type="button" variant="outline" onClick={onClose} disabled={isPending}>
                                {t('cancel')}
                            </Button>
                            <Button type="submit" disabled={isPending}>
                                {isPending
                                    ? t('saving')
                                    : isEditing
                                        ? t('saveChanges')
                                        : t('addProperty')}
                            </Button>
                        </div>
                    </form>
                </Form>
            </DialogContent>
        </Dialog>
    );
}
