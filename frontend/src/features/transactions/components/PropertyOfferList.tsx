import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, History } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { EmptyState } from '@/shared/components/branded/EmptyState';
import { PropertyOfferCard } from './PropertyOfferCard';
import { PropertyOfferDetailModal } from './PropertyOfferDetailModal';
import { AddPropertyOfferModal } from './AddPropertyOfferModal';
import { usePropertyOffers } from '@/features/transactions/api/queries';
import type { PropertyOffer } from '@/shared/api/types';

interface PropertyOfferListProps {
    transactionId: string;
    propertyId: string;
    isReadOnly?: boolean;
}

export function PropertyOfferList({ transactionId, propertyId, isReadOnly = false }: PropertyOfferListProps) {
    const { t } = useTranslation('transactions');
    const { data: offers, isLoading, error, refetch } = usePropertyOffers(propertyId);

    // Use ID for selection to ensure data is always fresh from the query
    const [selectedOfferId, setSelectedOfferId] = useState<string | null>(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    const selectedOffer = offers?.find(o => o.propertyOfferId === selectedOfferId) || null;

    const handleOfferClick = (offer: PropertyOffer) => {
        setSelectedOfferId(offer.propertyOfferId);
        setIsDetailModalOpen(true);
    };

    const handleCloseDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedOfferId(null);
    };

    // Sort offers: newest round first
    const sortedOffers = offers ? [...offers].sort((a, b) => b.offerRound - a.offerRound) : [];

    if (isLoading) {
        return <LoadingState message={t('loadingOffers')} />;
    }

    if (error) {
        return (
            <ErrorState
                message={error.message || t('errorLoadingOffers')}
                onRetry={() => refetch()}
            />
        );
    }

    if (!offers || offers.length === 0) {
        return (
            <div className="py-8">
                <EmptyState
                    icon={<History />}
                    title={t('noPropertyOffers')}
                    description={isReadOnly ? t('noPropertyOffersClientDescription') : t('noPropertyOffersDescription')}
                    action={
                        !isReadOnly ? (
                            <Button variant="outline" onClick={() => setIsAddModalOpen(true)}>
                                <Plus className="w-4 h-4 mr-2" />
                                {t('makeOffer')}
                            </Button>
                        ) : undefined
                    }
                />

                {/* Add Modal Placeholder */}
                {!isReadOnly && (
                    <AddPropertyOfferModal
                        isOpen={isAddModalOpen}
                        onClose={() => setIsAddModalOpen(false)}
                        transactionId={transactionId}
                        propertyId={propertyId}
                    />
                )}
            </div>
        );
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold">{t('offerHistory')}</h3>
                {!isReadOnly && (
                    <Button onClick={() => setIsAddModalOpen(true)} size="sm" className="gap-2">
                        <Plus className="w-4 h-4" />
                        {t('makeOffer')}
                    </Button>
                )}
            </div>

            <div className="space-y-3">
                {sortedOffers.map((offer) => (
                    <PropertyOfferCard
                        key={offer.propertyOfferId}
                        offer={offer}
                        onClick={() => handleOfferClick(offer)}
                    />
                ))}
            </div>

            <PropertyOfferDetailModal
                isOpen={isDetailModalOpen}
                onClose={handleCloseDetailModal}
                offer={selectedOffer}
                transactionId={transactionId}
                propertyId={propertyId}
                isReadOnly={isReadOnly}
            />

            {!isReadOnly && (
                <AddPropertyOfferModal
                    isOpen={isAddModalOpen}
                    onClose={() => setIsAddModalOpen(false)}
                    transactionId={transactionId}
                    propertyId={propertyId}
                />
            )}
        </div>
    );
}
