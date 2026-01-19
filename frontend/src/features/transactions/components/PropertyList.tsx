import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus, Home, X } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { EmptyState } from '@/shared/components/branded/EmptyState';
import { Section } from '@/shared/components/branded/Section';
import { PropertyCard } from './PropertyCard';
import { PropertyDetailModal } from './PropertyDetailModal';
import { AddPropertyModal } from './AddPropertyModal';
import { useTransactionProperties } from '@/features/transactions/api/queries';
import { useSetActiveProperty, useClearActiveProperty } from '@/features/transactions/api/mutations';
import type { Property } from '@/shared/api/types';

interface PropertyListProps {
    transactionId: string;
    isReadOnly?: boolean;
    onTransactionUpdate?: () => void;
    currentTransactionAddress?: { street: string; city?: string; postalCode?: string };
    canEdit?: boolean;
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

    const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);

    const handlePropertyClick = (property: Property) => {
        setSelectedProperty(property);
        setIsDetailModalOpen(true);
    };

    const handleCloseDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedProperty(null);
    };

    // Sort properties: active property first, then by creation date (newest first)
    const sortedProperties = properties ? [...properties].sort((a, b) => {
        const aIsActive = currentTransactionAddress?.street === a.address?.street;
        const bIsActive = currentTransactionAddress?.street === b.address?.street;

        // Active property comes first
        if (aIsActive && !bIsActive) return -1;
        if (!aIsActive && bIsActive) return 1;

        // Then sort by creation date (newest first)
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
    }) : [];

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

    if (!properties || properties.length === 0) {
        return (
            <Section>
                <EmptyState
                    icon={<Home />}
                    title={t('noProperties')}
                    description={isReadOnly ? t('noPropertiesClientDescription') : t('noPropertiesDescription')}
                    action={
                        (!isReadOnly && canEdit) ? (
                            <Button variant="outline" onClick={() => setIsAddModalOpen(true)}>
                                <Plus className="w-4 h-4 mr-2" />
                                <Button variant="outline" onClick={() => setIsAddModalOpen(true)}>
                                    <Plus className="w-4 h-4 mr-2" />
                                    {t('addProperty')}
                                </Button>
                            </Button>
                        ) : undefined
                    }
                />

                {!isReadOnly && canEdit && (
                    <AddPropertyModal
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
                <h3 className="text-lg font-semibold">{t('properties')}</h3>
                <div className="flex gap-2">
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
                    {!isReadOnly && canEdit && (
                        <Button onClick={() => setIsAddModalOpen(true)} size="sm" className="gap-2">
                            <Plus className="w-4 h-4" />
                            {t('addProperty')}
                        </Button>
                    )}
                </div>
            </div>

            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {sortedProperties.map((property) => {
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

            {/* Detail Modal */}
            <PropertyDetailModal
                isOpen={isDetailModalOpen}
                onClose={handleCloseDetailModal}
                property={selectedProperty}
                transactionId={transactionId}
                isReadOnly={isReadOnly}
            />

            {/* Add Modal (broker only) */}
            {!isReadOnly && canEdit && (
                <AddPropertyModal
                    isOpen={isAddModalOpen}
                    onClose={() => setIsAddModalOpen(false)}
                    transactionId={transactionId}
                />
            )}
        </div>
    );
}
