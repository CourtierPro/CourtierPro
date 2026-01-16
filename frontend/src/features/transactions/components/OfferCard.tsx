import { useTranslation } from 'react-i18next';
import { User, Calendar, CheckCircle, FileCheck, FileText } from 'lucide-react';
import { format } from 'date-fns';
import { Badge } from '@/shared/components/ui/badge';
import { Section } from '@/shared/components/branded/Section';
import type { Offer, ReceivedOfferStatus, ClientOfferDecision } from '@/shared/api/types';

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

const clientDecisionVariantMap: Record<ClientOfferDecision, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    ACCEPT: 'default',
    DECLINE: 'destructive',
    COUNTER: 'outline',
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
                        <User className="w-4 h-4 text-muted-foreground shrink-0" />
                        <span className="font-medium truncate">{offer.buyerName}</span>
                        {offer.documents && offer.documents.length > 0 && (
                            <Badge variant="outline" className="text-xs gap-1 ml-1">
                                <FileText className="w-3 h-3" />
                                {offer.documents.length}
                            </Badge>
                        )}
                        {offer.conditions && offer.conditions.length > 0 && (
                            <Badge variant="outline" className="text-xs gap-1 border-blue-500/50 text-blue-600 dark:text-blue-400">
                                <FileCheck className="w-3 h-3" />
                                {offer.conditions.length}
                            </Badge>
                        )}
                    </div>
                    <div className="text-sm text-muted-foreground pl-6 space-y-1">
                        <span>{formatCurrency(offer.offerAmount)}</span>
                        {offer.expiryDate && (
                            <div className="flex items-center gap-1 text-xs">
                                <Calendar className="w-3 h-3" />
                                <span>{t('expires')}: {format(new Date(offer.expiryDate), 'MMM d, yyyy')}</span>
                            </div>
                        )}
                    </div>
                </div>
                <div className="flex flex-col items-end gap-1">
                    <Badge variant={statusVariantMap[offer.status]}>
                        {t(`receivedOfferStatuses.${offer.status}`)}
                    </Badge>
                    {offer.clientDecision && (
                        <Badge variant={clientDecisionVariantMap[offer.clientDecision]} className="text-xs gap-1">
                            <CheckCircle className="w-3 h-3" />
                            {t(`clientOfferDecisions.${offer.clientDecision}`)}
                        </Badge>
                    )}
                </div>
            </div>
        </Section>
    );
}
