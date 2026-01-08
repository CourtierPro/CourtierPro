import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MapPin, DollarSign, Tag, Pencil, Trash2 } from 'lucide-react';
import { toast } from 'sonner';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from '@/shared/components/ui/dialog';
import { Button } from '@/shared/components/ui/button';
import { Badge } from '@/shared/components/ui/badge';
import { Separator } from '@/shared/components/ui/separator';
import { AddPropertyModal } from './AddPropertyModal';
import { useRemoveProperty } from '@/features/transactions/api/mutations';
import type { Property, OfferStatus } from '@/shared/api/types';

interface PropertyDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    property: Property | null;
    transactionId: string;
    isReadOnly?: boolean;
}

const offerStatusConfig: Record<OfferStatus, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; className: string }> = {
    OFFER_TO_BE_MADE: { variant: 'outline', className: 'border-muted-foreground text-muted-foreground' },
    OFFER_MADE: { variant: 'secondary', className: 'bg-blue-500/20 text-blue-600 dark:text-blue-400 border-blue-500/30' },
    COUNTERED: { variant: 'secondary', className: 'bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30' },
    ACCEPTED: { variant: 'secondary', className: 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border-emerald-500/30' },
    DECLINED: { variant: 'destructive', className: 'bg-red-500/20 text-red-600 dark:text-red-400 border-red-500/30' },
};

function formatCurrency(amount: number | undefined): string {
    if (amount === undefined || amount === null) return '—';
    return new Intl.NumberFormat('en-CA', {
        style: 'currency',
        currency: 'CAD',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    }).format(amount);
}

export function PropertyDetailModal({
    isOpen,
    onClose,
    property,
    transactionId,
    isReadOnly = false,
}: PropertyDetailModalProps) {
    const { t } = useTranslation('transactions');
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);
    const removeProperty = useRemoveProperty();

    if (!property) return null;

    const statusConfig = offerStatusConfig[property.offerStatus] || offerStatusConfig.OFFER_TO_BE_MADE;

    const handleDelete = async () => {
        try {
            await removeProperty.mutateAsync({
                transactionId,
                propertyId: property.propertyId,
            });
            toast.success(t('propertyRemoved'));
            onClose();
        } catch {
            toast.error(t('errorRemovingProperty'));
        }
    };

    const handleEditSuccess = () => {
        setIsEditModalOpen(false);
        onClose();
    };

    return (
        <>
            <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
                <DialogContent className="max-w-lg">
                    <DialogHeader>
                        <DialogTitle className="flex items-center gap-2">
                            <MapPin className="w-5 h-5 text-primary" />
                            {t('propertyDetails')}
                        </DialogTitle>
                        <DialogDescription>
                            {property.address?.street || t('unknownAddress')}
                            {property.centrisNumber && (
                                <span className="ml-2 bg-muted px-2 py-0.5 rounded text-xs">
                                    Centris: {property.centrisNumber}
                                </span>
                            )}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4 py-4">
                        {/* Address Section */}
                        <div className="space-y-2">
                            <h4 className="text-sm font-medium text-muted-foreground">{t('address')}</h4>
                            <div className="text-foreground">
                                <p>{property.address?.street}</p>
                                <p>
                                    {[property.address?.city, property.address?.province].filter(Boolean).join(', ')}
                                </p>
                                {property.address?.postalCode && (
                                    <p className="text-muted-foreground">{property.address.postalCode}</p>
                                )}
                            </div>
                        </div>

                        <Separator />

                        {/* Pricing Section */}
                        <div className="grid grid-cols-2 gap-4">
                            <div className="space-y-1">
                                <h4 className="text-sm font-medium text-muted-foreground">{t('askingPrice')}</h4>
                                <div className="flex items-center gap-2">
                                    <DollarSign className="w-4 h-4 text-muted-foreground" />
                                    <span className="text-lg font-semibold text-foreground">
                                        {formatCurrency(property.askingPrice)}
                                    </span>
                                </div>
                            </div>

                            {property.offerAmount && (
                                <div className="space-y-1">
                                    <h4 className="text-sm font-medium text-muted-foreground">{t('offerAmount')}</h4>
                                    <div className="flex items-center gap-2">
                                        <DollarSign className="w-4 h-4 text-muted-foreground" />
                                        <span className="text-lg font-semibold text-foreground">
                                            {formatCurrency(property.offerAmount)}
                                        </span>
                                    </div>
                                </div>
                            )}
                        </div>

                        <Separator />

                        {/* Status Section */}
                        <div className="space-y-2">
                            <h4 className="text-sm font-medium text-muted-foreground">{t('offerStatusLabel')}</h4>
                            <Badge variant={statusConfig.variant} className={`${statusConfig.className} text-sm px-3 py-1`}>
                                <Tag className="w-3 h-3 mr-1" />
                                {t(`offerStatus.${property.offerStatus}`)}
                            </Badge>
                        </div>

                        {/* Broker Notes (only shown to brokers) */}
                        {!isReadOnly && property.notes && (
                            <>
                                <Separator />
                                <div className="space-y-2">
                                    <h4 className="text-sm font-medium text-muted-foreground">{t('brokerNotes')}</h4>
                                    <p className="text-foreground text-sm bg-muted/50 p-3 rounded-md">
                                        {property.notes}
                                    </p>
                                </div>
                            </>
                        )}
                    </div>

                    {/* Actions (broker only) */}
                    {!isReadOnly && (
                        <div className="flex justify-end gap-2 pt-4 border-t border-border">
                            {isConfirmingDelete ? (
                                <>
                                    <span className="text-sm text-muted-foreground self-center mr-2">
                                        {t('confirmDeleteProperty')}
                                    </span>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => setIsConfirmingDelete(false)}
                                    >
                                        {t('cancel')}
                                    </Button>
                                    <Button
                                        variant="destructive"
                                        size="sm"
                                        onClick={handleDelete}
                                        disabled={removeProperty.isPending}
                                    >
                                        {removeProperty.isPending ? t('deleting') : t('confirmDelete')}
                                    </Button>
                                </>
                            ) : (
                                <>
                                    <Button
                                        variant="outline"
                                        size="sm"
                                        onClick={() => setIsEditModalOpen(true)}
                                    >
                                        <Pencil className="w-4 h-4 mr-2" />
                                        {t('edit')}
                                    </Button>
                                    <Button
                                        variant="ghost"
                                        size="sm"
                                        className="text-destructive hover:text-destructive"
                                        onClick={() => setIsConfirmingDelete(true)}
                                    >
                                        <Trash2 className="w-4 h-4 mr-2" />
                                        {t('remove')}
                                    </Button>
                                </>
                            )}
                        </div>
                    )}
                </DialogContent>
            </Dialog>

            {/* Edit Modal */}
            {!isReadOnly && (
                <AddPropertyModal
                    isOpen={isEditModalOpen}
                    onClose={() => setIsEditModalOpen(false)}
                    transactionId={transactionId}
                    existingProperty={property}
                    onSuccess={handleEditSuccess}
                />
            )}
        </>
    );
}
