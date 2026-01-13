import { useTranslation } from 'react-i18next';
import { Badge } from '@/shared/components/ui/badge';
import { Section } from '@/shared/components/branded/Section';
import { FileText, Calendar } from 'lucide-react';
import type { PropertyOffer, BuyerOfferStatus } from '@/shared/api/types';
import { format } from 'date-fns';

interface PropertyOfferCardProps {
    offer: PropertyOffer;
    onClick?: () => void;
    isActive?: boolean;
}

const statusConfig: Record<BuyerOfferStatus, { variant: 'default' | 'secondary' | 'destructive' | 'outline'; className: string }> = {
    DRAFT: { variant: 'outline', className: 'border-dashed border-muted-foreground text-muted-foreground' },
    SUBMITTED: { variant: 'secondary', className: 'bg-blue-500/20 text-blue-600 dark:text-blue-400 border-blue-500/30' },
    COUNTERED: { variant: 'secondary', className: 'bg-amber-500/20 text-amber-600 dark:text-amber-400 border-amber-500/30' },
    ACCEPTED: { variant: 'secondary', className: 'bg-emerald-500/20 text-emerald-600 dark:text-emerald-400 border-emerald-500/30' },
    DECLINED: { variant: 'destructive', className: 'bg-red-500/20 text-red-600 dark:text-red-400 border-red-500/30' },
    EXPIRED: { variant: 'outline', className: 'border-muted-foreground text-muted-foreground opacity-75' },
    WITHDRAWN: { variant: 'outline', className: 'border-muted-foreground text-muted-foreground opacity-75' },
};

export function PropertyOfferCard({ offer, onClick, isActive }: PropertyOfferCardProps) {
    const { t } = useTranslation('transactions');
    const statusInfo = statusConfig[offer.status];

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

    return (
        <Section
            className={`p-4 transition-all ${onClick ? 'cursor-pointer hover:border-primary/50 hover:shadow-md' : ''} ${isActive ? 'ring-2 ring-primary ring-offset-2' : ''}`}
            onClick={onClick}
            tabIndex={onClick ? 0 : undefined}
            role={onClick ? 'button' : undefined}
            aria-label={t('viewOfferDetails')}
        >
            <div className="flex flex-col gap-3">
                <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2">
                        <span className="font-semibold text-lg">
                            {t('offerRound', { round: offer.offerRound })}
                        </span>
                        {offer.documents?.length > 0 && (
                            <Badge variant="outline" className="text-xs gap-1">
                                <FileText className="w-3 h-3" />
                                {offer.documents.length}
                            </Badge>
                        )}
                    </div>
                    <Badge variant={statusInfo.variant} className={statusInfo.className}>
                        {t(`buyerOfferStatuses.${offer.status}`)}
                    </Badge>
                </div>

                <div className="flex items-center justify-between">
                    <span className="text-2xl font-bold tracking-tight">
                        {formatCurrency(offer.offerAmount)}
                    </span>

                    {offer.expiryDate && (
                        <div className="flex items-center gap-1.5 text-sm text-muted-foreground">
                            <Calendar className="w-4 h-4" />
                            <span>{t('expires')}: {formatDate(offer.expiryDate)}</span>
                        </div>
                    )}
                </div>

                {offer.notes && (
                    <div className="text-sm text-muted-foreground bg-muted/30 p-2 rounded border border-border/50 line-clamp-2">
                        {offer.notes}
                    </div>
                )}
            </div>
        </Section>
    );
}
