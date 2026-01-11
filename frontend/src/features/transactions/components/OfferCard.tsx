import { useTranslation } from 'react-i18next';
import { DollarSign, User } from 'lucide-react';
import { Badge } from '@/shared/components/ui/badge';
import { Section } from '@/shared/components/branded/Section';
import type { Offer, ReceivedOfferStatus } from '@/shared/api/types';

interface OfferCardProps {
    offer: Offer;
    onClick?: () => void;
}

const statusVariantMap: Record<ReceivedOfferStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    UNDER_REVIEW: 'secondary',
    COUNTERED: 'outline',
    ACCEPTED: 'default',
    DECLINED: 'destructive',
};

export function OfferCard({ offer, onClick }: OfferCardProps) {
    const { t } = useTranslation('transactions');

    const formatCurrency = (amount?: number) => {
        if (!amount) return '-';
        return new Intl.NumberFormat('en-CA', {
            style: 'currency',
            currency: 'CAD',
            maximumFractionDigits: 0,
        }).format(amount);
    };

    return (
        <Section
            className="p-4 cursor-pointer hover:bg-muted/50 transition-colors"
            onClick={onClick}
            tabIndex={0}
            role="button"
            aria-label={t('viewOfferDetails')}
        >
            <div className="flex items-start justify-between gap-2">
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                        <User className="w-4 h-4 text-muted-foreground flex-shrink-0" />
                        <span className="font-medium truncate">{offer.buyerName}</span>
                    </div>
                    <div className="flex items-center gap-2 text-sm text-muted-foreground">
                        <DollarSign className="w-4 h-4 flex-shrink-0" />
                        <span>{formatCurrency(offer.offerAmount)}</span>
                    </div>
                </div>
                <Badge variant={statusVariantMap[offer.status]}>
                    {t(`receivedOfferStatuses.${offer.status}`)}
                </Badge>
            </div>
        </Section>
    );
}
