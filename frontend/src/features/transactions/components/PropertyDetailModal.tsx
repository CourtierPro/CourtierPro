import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MapPin, Tag, Pencil, Trash2 } from 'lucide-react';
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
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/shared/components/ui/tabs';
import { AddPropertyModal } from './AddPropertyModal';
import { PropertyOfferList } from './PropertyOfferList';
import { useRemoveProperty, useUpdatePropertyStatus } from '@/features/transactions/api/mutations';
import type { Property, PropertyOfferStatus } from '@/shared/api/types';

interface PropertyDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    property: Property | null;
    transactionId: string;
    isReadOnly?: boolean;
}

const offerStatusConfig: Record<PropertyOfferStatus, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; className: string }> = {
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
    const { mutateAsync: updateStatus } = useUpdatePropertyStatus();

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
                <DialogContent className="max-w-2xl max-h-[85vh] flex flex-col">
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

                    <Tabs defaultValue="details" className="w-full flex-1 flex flex-col min-h-0">
                        <TabsList className="grid w-full grid-cols-2 flex-shrink-0">
                            <TabsTrigger value="details">{t('details')}</TabsTrigger>
                            <TabsTrigger value="offers">{t('offerHistory')}</TabsTrigger>
                        </TabsList>

                        <TabsContent value="details" className="space-y-4 py-4 flex-1 overflow-y-auto">
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
                                    <span className="text-lg font-semibold text-foreground">
                                        {formatCurrency(property.askingPrice)}
                                    </span>
                                </div>

                                <div className="space-y-1">
                                    <h4 className="text-sm font-medium text-muted-foreground">{t('offerAmount')}</h4>
                                    <span className="text-lg font-semibold text-foreground">
                                        {property.offerAmount ? formatCurrency(property.offerAmount) : t('pending')}
                                    </span>
                                </div>
                            </div>

                            <Separator />

                            {/* Status Section */}
                            <div className="space-y-2">
                                <h4 className="text-sm font-medium text-muted-foreground">{t('offerStatusLabel')}</h4>
                                <Badge variant={statusConfig.variant} className={`${statusConfig.className} text-sm px-3 py-1`}>
                                    <Tag className="w-3 h-3 mr-1" />
                                    {t(`propertyOfferStatuses.${property.offerStatus}`)}
                                </Badge>
                            </div>

                            {/* Notes (visible to all) */}
                            {property.notes && (
                                <>
                                    <Separator />
                                    <div className="space-y-2">
                                        <h4 className="text-sm font-medium text-muted-foreground">
                                            {isReadOnly ? t('notes') : t('brokerNotes')}
                                        </h4>
                                        <p className="text-foreground text-sm bg-muted/50 p-3 rounded-md">
                                            {property.notes}
                                        </p>
                                    </div>
                                </>
                            )}
                        </TabsContent>

                        <TabsContent value="offers" className="py-4 flex-1 overflow-y-auto">
                            <PropertyOfferList
                                transactionId={transactionId}
                                propertyId={property.propertyId}
                                isReadOnly={isReadOnly}
                            />
                        </TabsContent>
                    </Tabs>

                    {/* Actions (broker only) - visible only in details tab? Or always? Always is fine but maybe sticky? */}
                    {/* For now keeping it at bottom, but it applies mostly to "details" context. 
                        Wait, deleting property deletes offers too. 
                    */}


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
                    {/* Client Actions for Suggested Properties */}
                    {isReadOnly && property.offerStatus === 'OFFER_TO_BE_MADE' && property.status === 'SUGGESTED' && (
                        <div className="flex justify-between items-center pt-4 border-t border-border">
                            <div className="text-sm text-muted-foreground">
                                {t('clientDecision')}
                            </div>
                            <div className="flex gap-2">
                                <Button
                                    variant="outline"
                                    size="sm"
                                    className="text-destructive hover:text-destructive"
                                    onClick={async () => {
                                        await updateStatus({
                                            transactionId,
                                            propertyId: property.propertyId,
                                            status: 'NOT_INTERESTED'
                                        });
                                        toast.success(t('propertyRejected'));
                                        onClose();
                                    }}
                                >
                                    {t('clientOfferDecisions.DECLINE')}
                                </Button>
                                <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={async () => {
                                        const note = window.prompt(t('noteRequired'));
                                        if (note) {
                                            await updateStatus({
                                                transactionId,
                                                propertyId: property.propertyId,
                                                status: 'NEEDS_INFO',
                                                notes: note
                                            });
                                            toast.success(t('infoRequested'));
                                            onClose();
                                        }
                                    }}
                                >
                                    {t('requestInfo')}
                                </Button>
                                <Button
                                    size="sm"
                                    className="bg-emerald-600 hover:bg-emerald-700"
                                    onClick={async () => {
                                        await updateStatus({
                                            transactionId,
                                            propertyId: property.propertyId,
                                            status: 'INTERESTED'
                                        });
                                        toast.success(t('propertyAccepted'));
                                        onClose();
                                    }}
                                >
                                    {t('clientOfferDecisions.ACCEPT')}
                                </Button>
                            </div>
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
