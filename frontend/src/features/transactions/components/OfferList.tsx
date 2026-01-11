import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, DollarSign } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { EmptyState } from '@/shared/components/branded/EmptyState';
import { Section } from '@/shared/components/branded/Section';
import { OfferCard } from './OfferCard';
import { OfferDetailModal } from './OfferDetailModal';
import { AddOfferModal } from './AddOfferModal';
import { useTransactionOffers } from '@/features/transactions/api/queries';
import type { Offer } from '@/shared/api/types';

interface OfferListProps {
    transactionId: string;
    isReadOnly?: boolean;
    clientId?: string;
}

export function OfferList({ transactionId, isReadOnly = false, clientId }: OfferListProps) {
    const { t } = useTranslation('transactions');
    const { data: offers, isLoading, error, refetch } = useTransactionOffers(transactionId, clientId);

    const [selectedOffer, setSelectedOffer] = useState<Offer | null>(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    const handleOfferClick = (offer: Offer) => {
        setSelectedOffer(offer);
        setIsDetailModalOpen(true);
    };

    const handleCloseDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedOffer(null);
    };

    // Sort offers by creation date (newest first)
    const sortedOffers = offers ? [...offers].sort((a, b) => {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    }) : [];

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
            <Section>
                <EmptyState
                    icon={<DollarSign />}
                    title={t('noOffers')}
                    description={isReadOnly ? t('noOffersClientDescription') : t('noOffersDescription')}
                    action={
                        !isReadOnly ? (
                            <Button variant="outline" onClick={() => setIsAddModalOpen(true)}>
                                <Plus className="w-4 h-4 mr-2" />
                                {t('addOffer')}
                            </Button>
                        ) : undefined
                    }
                />

                {!isReadOnly && (
                    <AddOfferModal
                        isOpen={isAddModalOpen}
                        onClose={() => setIsAddModalOpen(false)}
                        transactionId={transactionId}
                    />
                )}
            </Section>
        );
    }

    return (
        <div className="space-y-4">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold">{t('offers')}</h3>
                {!isReadOnly && (
                    <Button onClick={() => setIsAddModalOpen(true)} size="sm" className="gap-2">
                        <Plus className="w-4 h-4" />
                        {t('addOffer')}
                    </Button>
                )}
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {sortedOffers.map((offer) => (
                    <OfferCard
                        key={offer.offerId}
                        offer={offer}
                        onClick={() => handleOfferClick(offer)}
                        isReadOnly={isReadOnly}
                    />
                ))}
            </div>

            {/* Detail Modal */}
            <OfferDetailModal
                isOpen={isDetailModalOpen}
                onClose={handleCloseDetailModal}
                offer={selectedOffer}
                transactionId={transactionId}
                isReadOnly={isReadOnly}
            />

            {/* Add Modal (broker only) */}
            {!isReadOnly && (
                <AddOfferModal
                    isOpen={isAddModalOpen}
                    onClose={() => setIsAddModalOpen(false)}
                    transactionId={transactionId}
                />
            )}
        </div>
    );
}
