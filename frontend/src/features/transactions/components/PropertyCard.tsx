import { useTranslation } from 'react-i18next';
import { MapPin, DollarSign, Tag } from 'lucide-react';
import { Badge } from '@/shared/components/ui/badge';
import { Section } from '@/shared/components/branded/Section';
import type { Property, PropertyOfferStatus } from '@/shared/api/types';

interface PropertyCardProps {
    property: Property;
    onClick?: () => void;
    isReadOnly?: boolean;
    isActive?: boolean;
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

function formatAddress(property: Property): string {
    const parts = [
        property.address?.street,
        property.address?.city,
        property.address?.province,
    ].filter(Boolean);
    return parts.join(', ') || 'No address';
}

export function PropertyCard({ property, onClick, isActive }: PropertyCardProps) {
    const { t } = useTranslation('transactions');

    const statusConfig = offerStatusConfig[property.offerStatus] || offerStatusConfig.OFFER_TO_BE_MADE;

    return (
        <Section
            className={`p-4 transition-all ${onClick ? 'cursor-pointer hover:border-primary/50 hover:shadow-md' : ''}`}
            onClick={onClick}
            tabIndex={onClick ? 0 : undefined}
            onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
            role={onClick ? 'button' : undefined}
            aria-label={onClick ? t('viewPropertyDetails') : undefined}
        >
            <div className="flex flex-col gap-3">
                {/* Address */}
                <div className="flex items-start gap-2">
                    <MapPin className="w-4 h-4 mt-0.5 text-muted-foreground shrink-0" />
                    <div className="flex flex-col">
                        <span className="font-medium text-foreground line-clamp-2">
                            {formatAddress(property)}
                            {property.address?.postalCode && <span className="ml-1">{property.address.postalCode}</span>}
                        </span>
                        {property.centrisNumber && (
                            <span className="text-xs text-muted-foreground">Centris: {property.centrisNumber}</span>
                        )}
                    </div>
                    {isActive && (
                        <Badge variant="success" className="ml-auto h-6 shrink-0">
                            Active
                        </Badge>
                    )}
                </div>
                {/* Price and Status Row */}
                <div className="flex items-center justify-between gap-2 flex-wrap">
                    <div className="flex items-center gap-2 text-sm">
                        <DollarSign className="w-4 h-4 text-muted-foreground" />
                        <span className="text-foreground font-semibold">{formatCurrency(property.askingPrice)}</span>
                        {property.offerAmount && (
                            <span className="text-muted-foreground">
                                ({t('offerAmount')}: {formatCurrency(property.offerAmount)})
                            </span>
                        )}
                    </div>

                    <Badge variant={statusConfig.variant} className={statusConfig.className}>
                        <Tag className="w-3 h-3 mr-1" />
                        {t(`propertyOfferStatuses.${property.offerStatus}`)}
                    </Badge>
                </div>

                {/* Postal Code moved to address line */}
            </div>
        </Section>
    );
}
