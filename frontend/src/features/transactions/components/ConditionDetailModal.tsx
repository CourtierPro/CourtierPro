import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar, FileCheck, Edit, Trash2, DollarSign, Home, User, ChevronRight, AlertTriangle } from 'lucide-react';
import { format } from 'date-fns';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
} from '@/shared/components/ui/dialog';
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
import { Button } from '@/shared/components/ui/button';
import { Badge } from '@/shared/components/ui/badge';
import { Section } from '@/shared/components/branded/Section';
import { Separator } from '@/shared/components/ui/separator';
import { useRemoveCondition } from '../api/mutations';
import type { Condition, ConditionStatus, ConditionType, Offer, PropertyOffer, ReceivedOfferStatus, BuyerOfferStatus } from '@/shared/api/types';

interface ConditionDetailModalProps {
    isOpen: boolean;
    onClose: () => void;
    condition: Condition | null;
    transactionId: string;
    linkedOffers?: Offer[];
    linkedPropertyOffers?: PropertyOffer[];
    onEdit?: () => void;
    onOfferClick?: (offer: Offer) => void;
    onPropertyOfferClick?: (offer: PropertyOffer) => void;
    isReadOnly?: boolean;
}

const statusVariantMap: Record<ConditionStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    SATISFIED: 'default',
    FAILED: 'destructive',
};

const offerStatusVariantMap: Record<ReceivedOfferStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    UNDER_REVIEW: 'secondary',
    COUNTERED: 'outline',
    ACCEPTED: 'default',
    DECLINED: 'destructive',
};

const propertyOfferStatusVariantMap: Record<BuyerOfferStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    DRAFT: 'secondary',
    SUBMITTED: 'secondary',
    COUNTERED: 'outline',
    ACCEPTED: 'default',
    REJECTED: 'destructive',
    WITHDRAWN: 'outline',
};

function getDeadlineStatus(deadlineDate: string): 'overdue' | 'urgent' | 'warning' | 'normal' {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const deadline = new Date(deadlineDate);
    deadline.setHours(0, 0, 0, 0);
    const diffTime = deadline.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    if (diffDays < 0) return 'overdue';
    if (diffDays <= 3) return 'urgent';
    if (diffDays <= 7) return 'warning';
    return 'normal';
}

const deadlineStyles: Record<'overdue' | 'urgent' | 'warning' | 'normal', string> = {
    overdue: 'text-red-600 dark:text-red-400',
    urgent: 'text-orange-600 dark:text-orange-400',
    warning: 'text-yellow-600 dark:text-yellow-400',
    normal: 'text-muted-foreground',
};

export function ConditionDetailModal({
    isOpen,
    onClose,
    condition,
    transactionId,
    linkedOffers = [],
    linkedPropertyOffers = [],
    onEdit,
    onOfferClick,
    onPropertyOfferClick,
    isReadOnly = false,
}: ConditionDetailModalProps) {
    const { t, i18n } = useTranslation('transactions');
    const [showDeleteDialog, setShowDeleteDialog] = useState(false);
    const removeCondition = useRemoveCondition();

    if (!condition) return null;

    const totalLinkedCount = linkedOffers.length + linkedPropertyOffers.length;
    const deadlineStatus = condition.status === 'PENDING' ? getDeadlineStatus(condition.deadlineDate) : 'normal';

    const getConditionTitle = (type: ConditionType, customTitle?: string) => {
        if (type === 'OTHER' && customTitle) return customTitle;
        return t(`conditionTypes.${type}`);
    };

    const formatDate = (dateStr: string) => {
        const [year, month, day] = dateStr.split('-').map(Number);
        const date = new Date(year, month - 1, day);
        const locale = i18n.language === 'fr' ? 'fr-CA' : 'en-CA';
        return date.toLocaleDateString(locale, {
            weekday: 'long',
            month: 'long',
            day: 'numeric',
            year: 'numeric',
        });
    };

    const formatCurrency = (amount?: number) => {
        if (!amount) return '-';
        return new Intl.NumberFormat('en-CA', {
            style: 'currency',
            currency: 'CAD',
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const handleDelete = async () => {
        try {
            await removeCondition.mutateAsync({
                transactionId,
                conditionId: condition.conditionId,
            });
            setShowDeleteDialog(false);
            onClose();
        } catch {
            // Error handled by global error system
        }
    };

    const handleOfferClick = (offer: Offer) => {
        onOfferClick?.(offer);
        onClose();
    };

    const handlePropertyOfferClick = (offer: PropertyOffer) => {
        onPropertyOfferClick?.(offer);
        onClose();
    };

    return (
        <>
            <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
                <DialogContent className="max-w-lg max-h-[85vh] flex flex-col">
                    <DialogHeader>
                        <div className="flex items-start justify-between gap-4">
                            <div className="flex items-center gap-3">
                                <div className="p-2 rounded-lg bg-blue-100 dark:bg-blue-900/30">
                                    <FileCheck className="w-5 h-5 text-blue-600 dark:text-blue-400" />
                                </div>
                                <div>
                                    <DialogTitle className="text-lg">
                                        {getConditionTitle(condition.type, condition.customTitle)}
                                    </DialogTitle>
                                    <Badge variant={statusVariantMap[condition.status]} className="mt-1">
                                        {t(`conditionStatus.${condition.status}`)}
                                    </Badge>
                                </div>
                            </div>
                        </div>
                    </DialogHeader>

                    <Tabs defaultValue="details" className="flex-1 flex flex-col min-h-0">
                        <TabsList className="w-full justify-start">
                            <TabsTrigger value="details">{t('details')}</TabsTrigger>
                            <TabsTrigger value="linked">
                                {t('conditions.linkedItems')} {totalLinkedCount > 0 && `(${totalLinkedCount})`}
                            </TabsTrigger>
                        </TabsList>

                        <TabsContent value="details" className="flex-1 overflow-y-auto space-y-4 py-4">
                            {/* Description */}
                            <div>
                                <span className="text-sm font-medium text-muted-foreground block mb-1">
                                    {t('conditions.description')}
                                </span>
                                <p className="text-sm">
                                    {condition.description || t('conditions.noDescription')}
                                </p>
                            </div>

                            <Separator />

                            {/* Deadline */}
                            <div className="flex items-center gap-3">
                                <Calendar className={`w-5 h-5 ${deadlineStyles[deadlineStatus]}`} />
                                <div>
                                    <span className="text-sm font-medium text-muted-foreground block">
                                        {t('conditions.deadline')}
                                    </span>
                                    <span className={`text-sm font-medium ${deadlineStyles[deadlineStatus]}`}>
                                        {formatDate(condition.deadlineDate)}
                                    </span>
                                    {condition.status === 'PENDING' && (deadlineStatus === 'overdue' || deadlineStatus === 'urgent') && (
                                        <div className="flex items-center gap-1 mt-1">
                                            <AlertTriangle className={`w-3.5 h-3.5 ${deadlineStyles[deadlineStatus]}`} />
                                            <span className={`text-xs ${deadlineStyles[deadlineStatus]}`}>
                                                {deadlineStatus === 'overdue' ? t('overdue') : t('dueSoon')}
                                            </span>
                                        </div>
                                    )}
                                </div>
                            </div>

                            {/* Broker Notes */}
                            {condition.notes && (
                                <>
                                    <Separator />
                                    <div>
                                        <span className="text-sm font-medium text-muted-foreground block mb-1">
                                            {t('conditions.notes')}
                                        </span>
                                        <p className="text-sm text-muted-foreground italic">
                                            {condition.notes}
                                        </p>
                                    </div>
                                </>
                            )}

                            {/* Actions */}
                            {!isReadOnly && (
                                <>
                                    <Separator />
                                    <div className="flex gap-4 pl-1">
                                        <Button
                                            variant="outline"
                                            onClick={onEdit}
                                        >
                                            <Edit className="w-4 h-4 mr-2" />
                                            {t('conditions.editCondition')}
                                        </Button>
                                        <Button
                                            variant="destructive"
                                            onClick={() => setShowDeleteDialog(true)}
                                        >
                                            <Trash2 className="w-4 h-4" />
                                        </Button>
                                    </div>
                                </>
                            )}
                        </TabsContent>

                        <TabsContent value="linked" className="flex-1 overflow-y-auto py-4">
                            <div className="space-y-4">
                                {/* Linked Offers (Sell-side) */}
                                {linkedOffers.length > 0 && (
                                    <div>
                                        <h4 className="text-sm font-medium text-muted-foreground mb-2 flex items-center gap-2">
                                            <User className="w-4 h-4" />
                                            {t('receivedOffers')} ({linkedOffers.length})
                                        </h4>
                                        <div className="space-y-2">
                                            {linkedOffers.map((offer) => (
                                                <Section
                                                    key={offer.offerId}
                                                    className="p-3 cursor-pointer hover:bg-muted/50 transition-colors"
                                                    onClick={() => handleOfferClick(offer)}
                                                    tabIndex={0}
                                                    role="button"
                                                >
                                                    <div className="flex items-center justify-between gap-2">
                                                        <div className="flex-1 min-w-0">
                                                            <div className="flex items-center gap-2">
                                                                <span className="font-medium truncate">{offer.buyerName}</span>
                                                                <Badge variant={offerStatusVariantMap[offer.status]} className="text-xs">
                                                                    {t(`receivedOfferStatuses.${offer.status}`)}
                                                                </Badge>
                                                            </div>
                                                            <div className="flex items-center gap-3 mt-1 text-sm text-muted-foreground">
                                                                <span className="flex items-center gap-1">
                                                                    <DollarSign className="w-3 h-3" />
                                                                    {formatCurrency(offer.offerAmount)}
                                                                </span>
                                                                {offer.expiryDate && (
                                                                    <span className="flex items-center gap-1 text-xs">
                                                                        <Calendar className="w-3 h-3" />
                                                                        {format(new Date(offer.expiryDate), 'MMM d, yyyy')}
                                                                    </span>
                                                                )}
                                                            </div>
                                                        </div>
                                                        <ChevronRight className="w-4 h-4 text-muted-foreground" />
                                                    </div>
                                                </Section>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* Linked Property Offers (Buy-side) */}
                                {linkedPropertyOffers.length > 0 && (
                                    <div>
                                        <h4 className="text-sm font-medium text-muted-foreground mb-2 flex items-center gap-2">
                                            <Home className="w-4 h-4" />
                                            {t('propertyOffers')} ({linkedPropertyOffers.length})
                                        </h4>
                                        <div className="space-y-2">
                                            {linkedPropertyOffers.map((offer) => (
                                                <Section
                                                    key={offer.propertyOfferId}
                                                    className="p-3 cursor-pointer hover:bg-muted/50 transition-colors"
                                                    onClick={() => handlePropertyOfferClick(offer)}
                                                    tabIndex={0}
                                                    role="button"
                                                >
                                                    <div className="flex items-center justify-between gap-2">
                                                        <div className="flex-1 min-w-0">
                                                            <div className="flex items-center gap-2">
                                                                <span className="font-medium truncate text-sm">
                                                                    {offer.propertyAddress || t('unknownProperty')}
                                                                </span>
                                                                <Badge variant={propertyOfferStatusVariantMap[offer.status]} className="text-xs">
                                                                    {t(`buyerOfferStatuses.${offer.status}`)}
                                                                </Badge>
                                                            </div>
                                                            <div className="flex items-center gap-3 mt-1 text-sm text-muted-foreground">
                                                                <span className="flex items-center gap-1">
                                                                    <DollarSign className="w-3 h-3" />
                                                                    {formatCurrency(offer.offerAmount)}
                                                                </span>
                                                                {offer.offerDate && (
                                                                    <span className="flex items-center gap-1 text-xs">
                                                                        <Calendar className="w-3 h-3" />
                                                                        {format(new Date(offer.offerDate), 'MMM d, yyyy')}
                                                                    </span>
                                                                )}
                                                            </div>
                                                        </div>
                                                        <ChevronRight className="w-4 h-4 text-muted-foreground" />
                                                    </div>
                                                </Section>
                                            ))}
                                        </div>
                                    </div>
                                )}

                                {/* Empty state */}
                                {totalLinkedCount === 0 && (
                                    <div className="text-center py-8 text-muted-foreground">
                                        <FileCheck className="w-12 h-12 mx-auto mb-3 opacity-50" />
                                        <p>{t('noLinkedItems')}</p>
                                        <p className="text-sm mt-1">{t('conditions.linkHint')}</p>
                                    </div>
                                )}
                            </div>
                        </TabsContent>
                    </Tabs>
                </DialogContent>
            </Dialog>

            {/* Delete Confirmation Dialog */}
            <AlertDialog open={showDeleteDialog} onOpenChange={setShowDeleteDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>{t('conditions.confirmDelete')}</AlertDialogTitle>
                        <AlertDialogDescription>
                            {t('conditions.confirmDeleteDescription')}
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel>{t('common.cancel')}</AlertDialogCancel>
                        <AlertDialogAction
                            onClick={handleDelete}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            {removeCondition.isPending ? t('common.deleting') : t('common.delete')}
                        </AlertDialogAction>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
}
