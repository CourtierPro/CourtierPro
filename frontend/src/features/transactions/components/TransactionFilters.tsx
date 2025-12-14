import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/shared/components/ui/select";
import { getStagesForSide, enumToLabel } from '@/shared/utils/stages';

interface TransactionFiltersProps {
    sideFilter: 'all' | 'buy' | 'sell';
    statusFilter: 'all' | 'active' | 'closed' | 'terminated';
    stageFilter: string;
    sortBy: 'dateAsc' | 'dateDesc';
    onSideFilterChange: (value: 'all' | 'buy' | 'sell') => void;
    onStatusFilterChange: (value: 'all' | 'active' | 'closed' | 'terminated') => void;
    onStageFilterChange: (value: string) => void;
    onSortByChange: (value: 'dateAsc' | 'dateDesc') => void;
    onResetFilters: () => void;
    hasActiveFilters: boolean;
}

export function TransactionFilters({
    sideFilter,
    statusFilter,
    stageFilter,
    sortBy,
    onSideFilterChange,
    onStatusFilterChange,
    onStageFilterChange,
    onSortByChange,
    onResetFilters,
    hasActiveFilters,
}: TransactionFiltersProps) {
    const { t } = useTranslation('transactions');

    // Get available stages based on selected side (deduplicated labels)
    const availableStages = (() => {
        if (sideFilter === 'buy') return getStagesForSide('BUY_SIDE').map(enumToLabel);
        if (sideFilter === 'sell') return getStagesForSide('SELL_SIDE').map(enumToLabel);
        const combined = [...getStagesForSide('BUY_SIDE'), ...getStagesForSide('SELL_SIDE')];
        const uniqueEnums = Array.from(new Set(combined));
        return uniqueEnums.map(enumToLabel);
    })();

    return (
        <Section className="p-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
                <div>
                    <label
                        htmlFor="side-filter"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('transactionSide')}
                    </label>
                    <Select
                        value={sideFilter}
                        onValueChange={(value) => onSideFilterChange(value as 'all' | 'buy' | 'sell')}
                    >
                        <SelectTrigger id="side-filter" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">{t('all')}</SelectItem>
                            <SelectItem value="buy">{t('buy')}</SelectItem>
                            <SelectItem value="sell">{t('sell')}</SelectItem>
                        </SelectContent>
                    </Select>
                </div>

                <div>
                    <label
                        htmlFor="status-filter"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('status')}
                    </label>
                    <Select
                        value={statusFilter}
                        onValueChange={(value) => onStatusFilterChange(value as 'all' | 'active' | 'closed' | 'terminated')}
                    >
                        <SelectTrigger id="status-filter" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">{t('all')}</SelectItem>
                            <SelectItem value="active">{t('active')}</SelectItem>
                            <SelectItem value="closed">{t('closed')}</SelectItem>
                            <SelectItem value="terminated">{t('terminated')}</SelectItem>
                        </SelectContent>
                    </Select>
                </div>

                <div>
                    <label
                        htmlFor="stage-filter"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('stage')}
                    </label>
                    <Select
                        value={stageFilter}
                        onValueChange={onStageFilterChange}
                        disabled={sideFilter === 'all'}
                    >
                        <SelectTrigger id="stage-filter" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">{t('all')}</SelectItem>
                            {availableStages.map((stage) => (
                                <SelectItem key={stage} value={stage}>
                                    {stage}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </Select>
                </div>

                <div>
                    <label
                        htmlFor="sort-by"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('sortBy')}
                    </label>
                    <Select
                        value={sortBy}
                        onValueChange={(value) => onSortByChange(value as 'dateAsc' | 'dateDesc')}
                    >
                        <SelectTrigger id="sort-by" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="dateDesc">{t('dateOpenedDesc')}</SelectItem>
                            <SelectItem value="dateAsc">{t('dateOpenedAsc')}</SelectItem>
                        </SelectContent>
                    </Select>
                </div>

                <div className="flex items-end">
                    <Button
                        variant="secondary"
                        onClick={onResetFilters}
                        disabled={!hasActiveFilters}
                        className="w-full gap-2"
                    >
                        <X className="w-4 h-4" />
                        {t('clearFilters')}
                    </Button>
                </div>
            </div>
        </Section>
    );
}

