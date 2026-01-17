import { useTranslation } from 'react-i18next';
import { format } from 'date-fns';
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription
} from '@/shared/components/ui/dialog';
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow
} from '@/shared/components/ui/table';
import { Badge } from '@/shared/components/ui/badge';
import type { Offer, ReceivedOfferStatus } from '@/shared/api/types';

interface OfferComparisonModalProps {
    isOpen: boolean;
    onClose: () => void;
    offers: Offer[];
}

const statusVariantMap: Record<ReceivedOfferStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    UNDER_REVIEW: 'secondary',
    COUNTERED: 'outline',
    ACCEPTED: 'default',
    DECLINED: 'destructive',
};

export function OfferComparisonModal({ isOpen, onClose, offers }: OfferComparisonModalProps) {
    const { t } = useTranslation('transactions');

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

    if (!offers || offers.length === 0) return null;

    return (
        <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
            <DialogContent className="max-w-[95vw] w-fit min-w-[600px] max-h-[85vh] flex flex-col">
                <DialogHeader>
                    <DialogTitle>{t('compareOffers')}</DialogTitle>
                    <DialogDescription>
                        {t('compareOffersDescription', { count: offers.length })}
                    </DialogDescription>
                </DialogHeader>

                <div className="flex-1 mt-4 overflow-auto">
                    <Table>
                        <TableHeader>
                            <TableRow>
                                <TableHead className="w-[180px] min-w-[180px] font-bold bg-muted text-foreground sticky left-0 z-10 border-r">
                                    {t('feature')}
                                </TableHead>
                                {offers.map((offer) => (
                                    <TableHead key={offer.offerId} className="min-w-[220px] text-center font-bold text-foreground">
                                        <div className="flex flex-col items-center gap-1">
                                            <span className="text-lg">{offer.buyerName}</span>
                                            <Badge variant={statusVariantMap[offer.status]} className="text-xs">
                                                {t(`receivedOfferStatuses.${offer.status}`)}
                                            </Badge>
                                        </div>
                                    </TableHead>
                                ))}
                            </TableRow>
                        </TableHeader>
                        <TableBody>
                            <TableRow>
                                <TableCell className="font-medium bg-muted sticky left-0 z-10 border-r">{t('offerAmount')}</TableCell>
                                {offers.map((offer) => (
                                    <TableCell key={offer.offerId} className="text-center font-semibold text-lg">
                                        {formatCurrency(offer.offerAmount)}
                                    </TableCell>
                                ))}
                            </TableRow>

                            <TableRow>
                                <TableCell className="font-medium bg-muted sticky left-0 z-10 border-r">{t('expiryDate')}</TableCell>
                                {offers.map((offer) => (
                                    <TableCell key={offer.offerId} className="text-center">
                                        {formatDate(offer.expiryDate)}
                                    </TableCell>
                                ))}
                            </TableRow>

                            <TableRow>
                                <TableCell className="font-medium bg-muted sticky left-0 z-10 border-r">{t('documents')}</TableCell>
                                {offers.map((offer) => (
                                    <TableCell key={offer.offerId} className="text-center">
                                        {offer.documents?.length || 0}
                                    </TableCell>
                                ))}
                            </TableRow>

                            <TableRow>
                                <TableCell className="font-medium bg-muted sticky left-0 z-10 border-r align-top">{t('notes')}</TableCell>
                                {offers.map((offer) => (
                                    <TableCell key={offer.offerId} className="text-center align-top text-sm text-muted-foreground whitespace-pre-wrap max-w-[200px]">
                                        {offer.notes || t('noNotes')}
                                    </TableCell>
                                ))}
                            </TableRow>
                        </TableBody>
                    </Table>
                </div>
            </DialogContent>
        </Dialog>
    );
}
