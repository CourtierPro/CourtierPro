import { useTranslation } from 'react-i18next';
import { MapPin, Tag, Eye } from 'lucide-react';
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

    // Determine badge for property status if not ACCEPTED (which is default state for active properties list)
    let statusBadge = null;
    if (property.status === 'SUGGESTED') {
        statusBadge = <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">{t(`propertyStatus.SUGGESTED`)}</Badge>;
    } else if (property.status === 'NEEDS_INFO') {
        statusBadge = <Badge variant="warning">{t(`propertyStatus.NEEDS_INFO`)}</Badge>;
    } else if (property.status === 'NOT_INTERESTED') {
        statusBadge = <Badge variant="destructive">{t(`propertyStatus.NOT_INTERESTED`)}</Badge>;
    }

    return (
        <Section
            className={`p-4 transition-all relative ${onClick ? 'cursor-pointer hover:border-primary/50 hover:shadow-md' : ''} ${isActive ? 'ring-2 ring-primary ring-offset-2 dark:ring-offset-card' : ''}`}
            onClick={onClick}
            tabIndex={onClick ? 0 : undefined}
            onKeyDown={onClick ? (e) => e.key === 'Enter' && onClick() : undefined}
            role={onClick ? 'button' : undefined}
            aria-label={onClick ? t('viewPropertyDetails') : undefined}
        >
            <div className="flex flex-col gap-3">
                {/* Header Row: Active Indicator & Address */}
                <div className="flex items-start justify-between gap-2">
                    <div className="flex items-start gap-2 max-w-[85%]">
                        <MapPin className="w-4 h-4 mt-0.5 text-muted-foreground shrink-0" />
                        <div className="flex flex-col">
                            <span className="font-medium text-foreground line-clamp-2">
                                {formatAddress(property)}
                                {property.address?.postalCode && <span className="ml-1 text-muted-foreground font-normal">{property.address.postalCode}</span>}
                            </span>
                            <div className="flex gap-2 text-xs text-muted-foreground mt-1">
                                {property.centrisNumber && (
                                    <span>Centris: {property.centrisNumber}</span>
                                )}
                            </div>
                        </div>
                    </div>
                    <div className="flex items-center gap-1.5 shrink-0 ml-auto">
                        {property.timesVisited != null && property.timesVisited > 0 && (
                            <Badge variant="outline" className="bg-violet-50 text-violet-700 border-violet-200 dark:bg-violet-500/20 dark:text-violet-400 dark:border-violet-500/30">
                                <Eye className="w-3 h-3 mr-1" />
                                {t('timesVisited', '{{count}} visit', { count: property.timesVisited })}
                            </Badge>
                        )}
                        {isActive && (
                            <Badge variant="default" className="bg-primary text-primary-foreground h-6">
                                {t('active')}
                            </Badge>
                        )}
                    </div>
                </div>

                {/* Price and Status Row */}
                <div className="flex items-end justify-between gap-2 pt-2 border-t border-border/50">
                    <div className="flex flex-col">
                        <span className="text-xs text-muted-foreground uppercase tracking-wider">{t('askingPrice')}</span>
                        <span className="text-foreground font-semibold">{formatCurrency(property.askingPrice)}</span>
                    </div>

                    <div className="flex items-center gap-2">
                        {/* Show Status Badge here (Suggested, Rejected, Needs Info) */}
                        {statusBadge}

                        {/* Show Offer Status if Accepted/Active */}
                        {(property.status === 'INTERESTED' || !property.status) && (
                            <>
                                {property.offerAmount && (
                                    <div className="flex flex-col items-end mr-2 text-right">
                                        <span className="text-xs text-muted-foreground uppercase tracking-wider">{t('offerAmount')}</span>
                                        <span className="text-foreground font-medium">{formatCurrency(property.offerAmount)}</span>
                                    </div>
                                )}
                                <Badge variant={statusConfig.variant} className={statusConfig.className}>
                                    <Tag className="w-3 h-3 mr-1" />
                                    {t(`propertyOfferStatuses.${property.offerStatus}`)}
                                </Badge>
                            </>
                        )}
                    </div>
                </div>
            </div>
        </Section>
    );
}

