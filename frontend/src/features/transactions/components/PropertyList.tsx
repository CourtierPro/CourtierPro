import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Home, X, Play } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { EmptyState } from '@/shared/components/branded/EmptyState';
import { Section } from '@/shared/components/branded/Section';
import { PropertyCard } from './PropertyCard';
import { PropertyDetailModal } from './PropertyDetailModal';
import { AddPropertyModal } from './AddPropertyModal';
import { PropertyReviewModal } from './PropertyReviewModal';
import { useTransactionProperties } from '@/features/transactions/api/queries';
import { useSetActiveProperty, useClearActiveProperty, useUpdatePropertyStatus } from '@/features/transactions/api/mutations';
import type { Property } from '@/shared/api/types';

interface PropertyListProps {
    transactionId: string;
    isReadOnly?: boolean; // isReadOnly=true usually means Client view here
    onTransactionUpdate?: () => void;
    currentTransactionAddress?: { street: string; city?: string; postalCode?: string };
    canEdit?: boolean; // canEdit=true usually means Broker view
}

export function PropertyList({
    transactionId,
    isReadOnly = false,
    onTransactionUpdate,
    currentTransactionAddress,
    canEdit = true
}: PropertyListProps) {
    const { t } = useTranslation('transactions');
    const { data: properties, isLoading, error, refetch } = useTransactionProperties(transactionId);
    const { mutate: setActiveProperty } = useSetActiveProperty();
    const { mutate: clearActiveProperty } = useClearActiveProperty();
    const { mutateAsync: updateStatus } = useUpdatePropertyStatus();

    const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

    const handlePropertyClick = (property: Property) => {
        setSelectedProperty(property);
        setIsDetailModalOpen(true);
    };

    const handleCloseDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedProperty(null);
    };

    // Filter properties into Suggested and Active (Accepted)
    const suggestedProperties = properties?.filter(p => p.status === 'SUGGESTED') || [];

    // Legacy support: if status is null/undefined, treat as accepted for now, or check explicit status
    const acceptedProperties = properties?.filter(p =>
        p.status === 'ACCEPTED' ||
        p.status === 'REJECTED' ||
        p.status === 'NEEDS_INFO' ||
        !p.status // fallback for legacy
    ) || [];

    // Sort active properties: active property first, then by creation date
    const sortedAcceptedProperties = [...acceptedProperties].sort((a, b) => {
        const aIsActive = currentTransactionAddress?.street === a.address?.street;
        const bIsActive = currentTransactionAddress?.street === b.address?.street;

        if (aIsActive && !bIsActive) return -1;
        if (!aIsActive && bIsActive) return 1;

        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    });

    const handleAccept = async (propertyId: string) => {
        await updateStatus({ transactionId, propertyId, status: 'ACCEPTED' });
    };

    const handleReject = async (propertyId: string) => {
        await updateStatus({ transactionId, propertyId, status: 'REJECTED' });
    };

    const handleRequestInfo = async (propertyId: string, notes: string) => {
        await updateStatus({ transactionId, propertyId, status: 'NEEDS_INFO', notes });
    };

    if (isLoading) {
        return <LoadingState message={t('loadingProperties')} />;
    }

    if (error) {
        return (
            <ErrorState
                message={error.message || t('errorLoadingProperties')}
                onRetry={() => refetch()}
            />
        );
    }

    // Determine if we should show suggested section
    // Always show for Broker (canEdit), show for Client if there are suggested properties
    const showSuggestedSection = canEdit || suggestedProperties.length > 0;

    // Determine which list to show for empty state logic


    return (
        <div className="space-y-8">
            {/* Suggested Properties Section */}
            {showSuggestedSection && (
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <h3 className="text-lg font-semibold">{t('suggestedProperties')}</h3>
                            {suggestedProperties.length > 0 && (
                                <span className="bg-primary/10 text-primary px-2 py-0.5 rounded text-sm font-medium">
                                    {suggestedProperties.length}
                                </span>
                            )}
                        </div>

                        {/* Client Actions: Review All */}
                        {isReadOnly && suggestedProperties.length > 0 && (
                            <Button onClick={() => setIsReviewModalOpen(true)} className="gap-2">
                                <Play className="w-4 h-4" />
                                {t('startReview')}
                            </Button>
                        )}

                        {/* Broker Actions: Add New */}
                        {!isReadOnly && canEdit && (
                            <Button onClick={() => setIsAddModalOpen(true)} size="sm" className="gap-2">
                                <Plus className="w-4 h-4" />
                                {t('addProperty')}
                            </Button>
                        )}
                    </div>

                    {suggestedProperties.length === 0 ? (
                        <Section className="border-dashed bg-muted/20">
                            <div className="p-8 text-center text-muted-foreground">
                                {isReadOnly ? t('noSuggestedProperties') : t('noPropertiesDescription')}
                            </div>
                        </Section>
                    ) : (
                        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                            {suggestedProperties.map(property => (
                                <PropertyCard
                                    key={property.propertyId}
                                    property={property}
                                    onClick={() => handlePropertyClick(property)}
                                    // Clients can't edit details directly in list, but can review
                                    // Brokers can edit
                                    isReadOnly={isReadOnly}
                                    isActive={false} // Suggested properties are never "Active" transaction property
                                />
                            ))}
                        </div>
                    )}
                </div>
            )}

            {/* Active/Accepted Properties Section */}
            {acceptedProperties.length > 0 && (
                <div className="space-y-4">
                    <div className="flex items-center justify-between">
                        <h3 className="text-lg font-semibold">{t('activeProperties')}</h3>
                        {/* Broker Clear Active Button */}
                        {!isReadOnly && canEdit && currentTransactionAddress?.street && (
                            <Button
                                variant="outline"
                                size="sm"
                                className="gap-2"
                                onClick={() => {
                                    clearActiveProperty({ transactionId });
                                    onTransactionUpdate?.();
                                }}
                            >
                                <X className="w-4 h-4" />
                                {t('clearActiveProperty')}
                            </Button>
                        )}
                    </div>

                    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                        {sortedAcceptedProperties.map((property) => {
                            const isActive = currentTransactionAddress?.street === property.address.street;
                            return (
                                <div key={property.propertyId} className="relative group">
                                    <PropertyCard
                                        property={property}
                                        onClick={() => handlePropertyClick(property)}
                                        isReadOnly={isReadOnly}
                                        isActive={isActive}
                                    />
                                    {!isReadOnly && canEdit && !isActive && (
                                        <div className="absolute top-2 right-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                            <Button
                                                variant="secondary"
                                                size="sm"
                                                className="shadow-sm h-7 text-xs px-2"
                                                onClick={(e) => {
                                                    e.stopPropagation();
                                                    setActiveProperty({ transactionId: property.transactionId, propertyId: property.propertyId });
                                                    onTransactionUpdate?.();
                                                }}
                                            >
                                                {t('setActive')}
                                            </Button>
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                </div>
            )}

            {/* Total Empty State (if absolutely nothing exists and user is client) */}
            {!showSuggestedSection && acceptedProperties.length === 0 && (
                <Section>
                    <EmptyState
                        icon={<Home />}
                        title={t('noProperties')}
                        description={isReadOnly ? t('noPropertiesClientDescription') : t('noPropertiesDescription')}
                    />
                </Section>
            )}


            {/* Modals */}
            <PropertyDetailModal
                isOpen={isDetailModalOpen}
                onClose={handleCloseDetailModal}
                property={selectedProperty}
                transactionId={transactionId}
                isReadOnly={isReadOnly}
            />

            {!isReadOnly && canEdit && (
                <AddPropertyModal
                    isOpen={isAddModalOpen}
                    onClose={() => setIsAddModalOpen(false)}
                    transactionId={transactionId}
                />
            )}

            <PropertyReviewModal
                isOpen={isReviewModalOpen}
                onClose={() => setIsReviewModalOpen(false)}
                properties={suggestedProperties}
                onAccept={handleAccept}
                onReject={handleReject}
                onRequestInfo={handleRequestInfo}
            />
        </div>
    );
}
