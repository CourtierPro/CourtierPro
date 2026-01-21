import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Section } from '@/shared/components/branded/Section';
import { ConditionCard } from './ConditionCard';
import type { Condition, Offer, PropertyOffer } from '@/shared/api/types';

interface ConditionListProps {
    conditions: Condition[];
    onAddClick?: () => void;
    onConditionClick?: (condition: Condition) => void;
    isLoading?: boolean;
    isReadOnly?: boolean;
    offers?: Offer[];
    propertyOffers?: PropertyOffer[];
    canEdit?: boolean;
}

export function ConditionList({
    conditions,
    onAddClick,
    onConditionClick,
    isLoading,
    isReadOnly,
    offers = [],
    propertyOffers = [],
    canEdit = true,
}: ConditionListProps) {
    const { t } = useTranslation('transactions');

    // Compute linked count for each condition
    const getLinkedCount = (conditionId: string) => {
        const linkedOfferCount = offers.filter(
            offer => offer.conditions?.some(c => c.conditionId === conditionId)
        ).length;
        const linkedPropertyOfferCount = propertyOffers.filter(
            po => po.conditions?.some(c => c.conditionId === conditionId)
        ).length;
        return linkedOfferCount + linkedPropertyOfferCount;
    };

    if (isLoading) {
        return (
            <Section className="p-6">
                <div className="animate-pulse space-y-4">
                    <div className="h-20 bg-muted rounded-lg" />
                    <div className="h-20 bg-muted rounded-lg" />
                </div>
            </Section>
        );
    }

    return (
        <div className="space-y-4">
            {!isReadOnly && canEdit && (
                <div className="flex justify-between items-center">
                    <h3 className="text-lg font-semibold">{t('conditions.title')}</h3>
                    <Button onClick={onAddClick} size="sm">
                        <Plus className="w-4 h-4 mr-1" />
                        {t('conditions.addCondition')}
                    </Button>
                </div>
            )}

            {conditions.length === 0 ? (
                <Section className="p-6 text-center text-muted-foreground">
                    {t('conditions.noConditions')}
                </Section>
            ) : (
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                    {conditions.map((condition) => (
                        <ConditionCard
                            key={condition.conditionId}
                            condition={condition}
                            onClick={() => onConditionClick?.(condition)}
                            isReadOnly={isReadOnly}
                            linkedCount={getLinkedCount(condition.conditionId)}
                        />
                    ))}
                </div>
            )}
        </div>
    );
}
