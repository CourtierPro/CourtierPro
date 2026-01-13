import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, DollarSign, GripHorizontal } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Checkbox } from '@/shared/components/ui/checkbox';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { EmptyState } from '@/shared/components/branded/EmptyState';
import { Section } from '@/shared/components/branded/Section';
import { OfferCard } from './OfferCard';
import { OfferDetailModal } from './OfferDetailModal';
import { AddOfferModal } from './AddOfferModal';
import { OfferComparisonModal } from './OfferComparisonModal';
import { useTransactionOffers } from '@/features/transactions/api/queries';
import type { Offer } from '@/shared/api/types';

interface OfferListProps {
    transactionId: string;
    isReadOnly?: boolean;
    clientId?: string;
}

export function OfferList({ transactionId, isReadOnly = false, clientId }: OfferListProps) {
    const { t } = useTranslation('transactions');
    const { data: offers, isLoading, error, refetch } = useTransactionOffers(transactionId, true, clientId);

    const [selectedOffer, setSelectedOffer] = useState<Offer | null>(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    // Comparison State
    const [isComparing, setIsComparing] = useState(false);
    const [selectedForComparison, setSelectedForComparison] = useState<string[]>([]);
    const [isComparisonModalOpen, setIsComparisonModalOpen] = useState(false);

    const handleOfferClick = (offer: Offer) => {
        if (isComparing) {
            handleToggleSelection(offer.offerId);
        } else {
            setSelectedOffer(offer);
            setIsDetailModalOpen(true);
        }
    };

    const handleToggleSelection = (offerId: string) => {
        setSelectedForComparison(prev => {
            if (prev.includes(offerId)) {
                return prev.filter(id => id !== offerId);
            } else {
                if (prev.length >= 3) return prev; // Limit to 3
                return [...prev, offerId];
            }
        });
    };

    const handleStartComparison = () => {
        setIsComparing(true);
        setSelectedForComparison([]);
    };

    const handleCancelComparison = () => {
        setIsComparing(false);
        setSelectedForComparison([]);
    };

    const handleViewComparison = () => {
        setIsComparisonModalOpen(true);
    };

    const handleCloseDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedOffer(null);
    };

    // Sort offers by creation date (newest first)
    const sortedOffers = offers ? [...offers].sort((a, b) => {
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    }) : [];

    const comparisonOffers = offers?.filter(o => selectedForComparison.includes(o.offerId)) || [];

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
                <div className="flex items-center gap-2">
                    {isComparing ? (
                        <>
                            <span className="text-sm text-muted-foreground mr-2">
                                {t('selectUpToThree', { count: selectedForComparison.length })}
                            </span>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={handleCancelComparison}
                            >
                                {t('cancel')}
                            </Button>
                            <Button
                                size="sm"
                                onClick={handleViewComparison}
                                disabled={selectedForComparison.length < 2}
                            >
                                {t('compareSelected')}
                            </Button>
                        </>
                    ) : (
                        <>
                            {offers.length > 1 && (
                                <Button variant="ghost" size="sm" onClick={handleStartComparison}>
                                    <GripHorizontal className="w-4 h-4 mr-2" />
                                    {t('compare')}
                                </Button>
                            )}
                            {!isReadOnly && (
                                <Button onClick={() => setIsAddModalOpen(true)} size="sm" className="gap-2">
                                    <Plus className="w-4 h-4" />
                                    {t('addOffer')}
                                </Button>
                            )}
                        </>
                    )}
                </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {sortedOffers.map((offer) => (
                    <div key={offer.offerId} className="relative group">
                        {isComparing && (
                            <div className="absolute top-2 right-2 z-10">
                                <Checkbox
                                    checked={selectedForComparison.includes(offer.offerId)}
                                    className="h-5 w-5 bg-background data-[state=checked]:bg-primary data-[state=checked]:text-primary-foreground"
                                    onClick={(e) => {
                                        e.stopPropagation();
                                        handleToggleSelection(offer.offerId);
                                    }}
                                />
                            </div>
                        )}
                        <div
                            className={`transition-all ${isComparing && selectedForComparison.includes(offer.offerId) ? 'ring-2 ring-primary rounded-lg' : ''}`}
                            onClick={() => handleOfferClick(offer)}
                        >
                            <OfferCard
                                offer={offer}
                                onClick={() => { }} // Handled by parent div
                            />
                            {isComparing && (
                                <div className="absolute inset-0 bg-transparent cursor-pointer rounded-lg" />
                            )}
                        </div>
                    </div>
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

            {/* Comparison Modal */}
            <OfferComparisonModal
                isOpen={isComparisonModalOpen}
                onClose={() => setIsComparisonModalOpen(false)}
                offers={comparisonOffers}
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
