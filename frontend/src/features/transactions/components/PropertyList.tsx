import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { Plus, Play } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { PropertyCard } from './PropertyCard';
import { PropertyDetailModal } from './PropertyDetailModal';
import { AddPropertyModal } from './AddPropertyModal';
import { PropertyReviewModal } from './PropertyReviewModal';
import { useTransactionProperties } from '@/features/transactions/api/queries';
import { useSetActiveProperty, useUpdatePropertyStatus } from '@/features/transactions/api/mutations';
import type { Property, PropertyOfferStatus } from '@/shared/api/types';

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

    const { mutateAsync: updateStatus } = useUpdatePropertyStatus();

    const [selectedProperty, setSelectedProperty] = useState<Property | null>(null);
    const [isDetailModalOpen, setIsDetailModalOpen] = useState(false);
    const [isAddModalOpen, setIsAddModalOpen] = useState(false);
    const [isReviewModalOpen, setIsReviewModalOpen] = useState(false);

    // Column visibility states
    const [collapsedColumns, setCollapsedColumns] = useState<Record<string, boolean>>({
        suggested: false,
        interested: false,
        notInterested: false
    });

    // Column Filters (by offerStatus)
    const [columnFilters, setColumnFilters] = useState<Record<string, PropertyOfferStatus | 'ALL'>>({
        suggested: 'ALL',
        interested: 'ALL',
        notInterested: 'ALL'
    });

    const handlePropertyClick = (property: Property) => {
        setSelectedProperty(property);
        setIsDetailModalOpen(true);
    };

    const handleCloseDetailModal = () => {
        setIsDetailModalOpen(false);
        setSelectedProperty(null);
    };

    const toggleColumn = (col: string) => {
        setCollapsedColumns(prev => ({ ...prev, [col]: !prev[col] }));
    };

    const setFilter = (col: string, status: PropertyOfferStatus | 'ALL') => {
        setColumnFilters(prev => ({ ...prev, [col]: status }));
    };

    const handleAccept = async (propertyId: string) => {
        await updateStatus({ transactionId, propertyId, status: 'INTERESTED' });
    };

    const handleReject = async (propertyId: string) => {
        await updateStatus({ transactionId, propertyId, status: 'NOT_INTERESTED' });
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

    // Categorize properties
    const suggestedProps = properties?.filter(p => p.status === 'SUGGESTED' || p.status === 'NEEDS_INFO') || [];
    const interestedProps = properties?.filter(p => p.status === 'INTERESTED' || !p.status) || [];
    const notInterestedProps = properties?.filter(p => p.status === 'NOT_INTERESTED') || [];

    // Filter helper
    const getFilteredProperties = (props: Property[], colKey: string) => {
        const filter = columnFilters[colKey];
        if (filter === 'ALL') return props;
        return props.filter(p => p.offerStatus === filter);
    };

    const offerStatuses: PropertyOfferStatus[] = ['OFFER_TO_BE_MADE', 'OFFER_MADE', 'COUNTERED', 'ACCEPTED', 'DECLINED'];

    const renderColumn = (key: string, titleKey: string, props: Property[]) => {
        const isCollapsed = collapsedColumns[key];
        const filteredProps = getFilteredProperties(props, key);
        const count = props.length;
        const displayCount = filteredProps.length !== props.length ? `${filteredProps.length}/${props.length}` : count;

        return (
            <motion.div
                layout
                initial={false}
                animate={{
                    width: isCollapsed ? "3.5rem" : "auto", // Using 3.5rem (~14 w) + padding
                    flex: isCollapsed ? 0 : 1
                }}
                transition={{ type: "spring", stiffness: 300, damping: 60 }}
                className={`flex flex-row gap-4 bg-muted/40 p-2 rounded-lg border border-border h-fit min-h-[375px] ${isCollapsed ? 'cursor-pointer hover:bg-muted/60' : 'min-w-[300px]'}`}
                onClick={isCollapsed ? () => toggleColumn(key) : undefined}
                title={isCollapsed ? t('toggleColumn') : undefined}
            >
                {/* Left Sidebar (Vertical) - Always Visible */}
                <motion.div
                    layout="position"
                    className={`flex flex-col items-center gap-4 py-2 w-10 cursor-pointer transition-colors rounded-lg ${!isCollapsed ? 'bg-background/50 hover:bg-muted/60' : ''}`}
                    onClick={(e) => {
                        // If expanded, this sidebar click toggles. If collapsed, parent click handles it, but we can also handle it here.
                        e.stopPropagation();
                        toggleColumn(key);
                    }}
                >
                    <Button
                        variant="ghost"
                        size="sm"
                        className="p-1 h-6 w-6 shrink-0"
                    >
                        <Play className={`h-3 w-3 transition-transform ${isCollapsed ? "" : "rotate-180"}`} />
                    </Button>
                    <div className="[writing-mode:vertical-rl] [text-orientation:upright] flex items-center gap-4 mt-4 tracking-widest font-semibold text-sm uppercase select-none">
                        <span className="whitespace-nowrap">{t(`columns.${titleKey}`)}</span>
                        <span className="text-muted-foreground text-xs">{displayCount}</span>
                    </div>
                </motion.div>

                {/* Filter and Content - Hidden if Collapsed */}
                <AnimatePresence mode="popLayout">
                    {!isCollapsed && (
                        <motion.div
                            initial={{ opacity: 0, x: -20 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -20 }}
                            transition={{ duration: 0.2 }}
                            className="flex flex-col gap-4 flex-1 py-2 pr-2"
                        >
                            {/* Filter Dropdown */}
                            <div className="flex items-center gap-2">
                                <span className="text-xs text-muted-foreground">{t('filterByStatus')}:</span>
                                <select
                                    className="text-xs border rounded px-2 py-1 bg-background"
                                    value={columnFilters[key]}
                                    onChange={(e) => setFilter(key, e.target.value as PropertyOfferStatus | 'ALL')}
                                >
                                    <option value="ALL">{t('all')}</option>
                                    {offerStatuses.map(s => (
                                        <option key={s} value={s}>{t(`propertyOfferStatuses.${s}`)}</option>
                                    ))}
                                </select>
                            </div>

                            <div className="flex flex-col gap-3">
                                {filteredProps.map(property => {
                                    const isActive = currentTransactionAddress?.street === property.address.street;
                                    return (
                                        <div key={property.propertyId} className="relative group max-w-xl">
                                            <PropertyCard
                                                property={property}
                                                onClick={() => handlePropertyClick(property)}
                                                isReadOnly={isReadOnly}
                                                isActive={isActive}
                                            />
                                            {/* Set Active Button (Broker/Accepted only) */}
                                            {!isReadOnly && canEdit && !isActive && key === 'interested' && (
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
                                {filteredProps.length === 0 && (
                                    <div className="text-center py-8 border-2 border-dashed border-muted rounded-md text-muted-foreground text-sm">
                                        {t('status.empty')}
                                    </div>
                                )}
                            </div>
                        </motion.div>
                    )}
                </AnimatePresence>
            </motion.div>
        );
    };

    return (
        <div className="space-y-6">
            <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold">{t('properties')}</h3>
                <div className="flex gap-2">
                    {/* Client Actions: Review All */}
                    {isReadOnly && suggestedProps.length > 0 && (
                        <Button onClick={() => setIsReviewModalOpen(true)} className="gap-2" variant="outline">
                            <Play className="w-4 h-4" />
                            {t('startReview')}
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

            <div className="flex flex-row gap-4 items-stretch h-full overflow-x-auto pb-4">
                {renderColumn('suggested', 'suggested', suggestedProps)}
                {renderColumn('interested', 'interested', interestedProps)}
                {renderColumn('notInterested', 'notInterested', notInterestedProps)}
            </div>

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
                properties={suggestedProps}
                onAccept={handleAccept}
                onReject={handleReject}
                onRequestInfo={handleRequestInfo}
            />
        </div>
    );
}
