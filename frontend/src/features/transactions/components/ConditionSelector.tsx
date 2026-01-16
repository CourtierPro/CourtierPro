import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Plus } from 'lucide-react';
import { Badge } from '@/shared/components/ui/badge';
import { Button } from '@/shared/components/ui/button';
import { Checkbox } from '@/shared/components/ui/checkbox';
import {
    Popover,
    PopoverContent,
    PopoverTrigger,
} from '@/shared/components/ui/popover';
import { useTransactionConditions } from '@/features/transactions/api/queries';
import { AddConditionModal } from './AddConditionModal';
import type { Condition } from '@/shared/api/types';

interface ConditionSelectorProps {
    transactionId: string;
    selectedConditionIds: string[];
    onChange: (conditionIds: string[]) => void;
    disabled?: boolean;
    showCreateButton?: boolean;
}

/**
 * Multi-select component for selecting conditions to link to an offer or document.
 */
export function ConditionSelector({
    transactionId,
    selectedConditionIds,
    onChange,
    disabled = false,
    showCreateButton = false,
}: ConditionSelectorProps) {
    const { t } = useTranslation('transactions');
    const { data: conditions = [], isLoading } = useTransactionConditions(transactionId);
    const [open, setOpen] = useState(false);
    const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

    const handleToggle = (conditionId: string) => {
        if (selectedConditionIds.includes(conditionId)) {
            onChange(selectedConditionIds.filter((id) => id !== conditionId));
        } else {
            onChange([...selectedConditionIds, conditionId]);
        }
    };

    const getConditionLabel = (condition: Condition) => {
        if (condition.customTitle) {
            return condition.customTitle;
        }
        return t(`conditionTypes.${condition.type}`);
    };

    const selectedConditions = conditions.filter((c) =>
        selectedConditionIds.includes(c.conditionId)
    );

    if (conditions.length === 0 && !isLoading) {
        return (
            <div className="space-y-2">
                <label className="text-sm font-medium">{t('linkedConditions')}</label>
                <div className="flex items-center gap-2">
                    <p className="text-xs text-muted-foreground">{t('noConditionsAvailable')}</p>
                    {showCreateButton && (
                        <>
                            <Button
                                type="button"
                                variant="outline"
                                size="sm"
                                onClick={() => setIsCreateModalOpen(true)}
                                className="gap-1"
                            >
                                <Plus className="w-3 h-3" />
                                {t('createCondition')}
                            </Button>
                            <AddConditionModal
                                open={isCreateModalOpen}
                                onOpenChange={setIsCreateModalOpen}
                                transactionId={transactionId}
                                nestedInModal
                            />
                        </>
                    )}
                </div>
            </div>
        );
    }

    return (
        <div className="space-y-2">
            <label className="text-sm font-medium">{t('linkedConditions')}</label>
            <Popover open={open} onOpenChange={setOpen}>
                <PopoverTrigger asChild>
                    <Button
                        variant="outline"
                        role="combobox"
                        aria-expanded={open}
                        className="w-full justify-between text-left font-normal"
                        disabled={disabled || isLoading}
                    >
                        {selectedConditions.length > 0 ? (
                            <span className="truncate">
                                {selectedConditions.length} {t('conditionsSelected')}
                            </span>
                        ) : (
                            <span className="text-muted-foreground">{t('selectConditions')}</span>
                        )}
                    </Button>
                </PopoverTrigger>
                <PopoverContent className="w-[300px] p-2" align="start">
                    <div className="space-y-2 max-h-64 overflow-y-auto">
                        {conditions.map((condition) => (
                            <div
                                key={condition.conditionId}
                                className="flex items-center space-x-2 p-2 rounded hover:bg-muted cursor-pointer"
                                onClick={() => handleToggle(condition.conditionId)}
                            >
                                <Checkbox
                                    id={condition.conditionId}
                                    checked={selectedConditionIds.includes(condition.conditionId)}
                                    onCheckedChange={() => handleToggle(condition.conditionId)}
                                />
                                <div className="flex-1 min-w-0">
                                    <p className="text-sm font-medium truncate">
                                        {getConditionLabel(condition)}
                                    </p>
                                    <p className="text-xs text-muted-foreground truncate">
                                        {t(`conditionStatus.${condition.status}`)} • {condition.deadlineDate}
                                    </p>
                                </div>
                            </div>
                        ))}
                    </div>
                </PopoverContent>
            </Popover>

            {/* Show selected conditions as badges */}
            {selectedConditions.length > 0 && (
                <div className="flex flex-wrap gap-1 mt-2">
                    {selectedConditions.map((condition) => (
                        <Badge
                            key={condition.conditionId}
                            variant="secondary"
                            className="text-xs cursor-pointer"
                            onClick={() => handleToggle(condition.conditionId)}
                        >
                            {getConditionLabel(condition)}
                            <span className="ml-1 text-muted-foreground hover:text-foreground">×</span>
                        </Badge>
                    ))}
                </div>
            )}

            {/* Create Condition Button */}
            {showCreateButton && (
                <>
                    <Button
                        type="button"
                        variant="ghost"
                        size="sm"
                        onClick={() => setIsCreateModalOpen(true)}
                        className="gap-1 mt-2 text-muted-foreground hover:text-foreground"
                    >
                        <Plus className="w-3 h-3" />
                        {t('createCondition')}
                    </Button>
                    <AddConditionModal
                        open={isCreateModalOpen}
                        onOpenChange={setIsCreateModalOpen}
                        transactionId={transactionId}
                        nestedInModal
                    />
                </>
            )}
        </div>
    );
}
