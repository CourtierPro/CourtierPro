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
    statusFilter: 'all' | 'ACTIVE' | 'CLOSED_SUCCESSFULLY' | 'TERMINATED_EARLY';
    stageFilter: string;
    sortBy: 'dateAsc' | 'dateDesc' | 'lastUpdatedDesc' | 'lastUpdatedAsc';
    onSideFilterChange: (value: 'all' | 'buy' | 'sell') => void;
    onStatusFilterChange: (value: 'all' | 'ACTIVE' | 'CLOSED_SUCCESSFULLY' | 'TERMINATED_EARLY') => void;
    onStageFilterChange: (value: string) => void;
    onSortByChange: (value: 'dateAsc' | 'dateDesc' | 'lastUpdatedDesc' | 'lastUpdatedAsc') => void;
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
        let stages: string[] = [];
        if (sideFilter === 'buy') {
            stages = getStagesForSide('BUY_SIDE');
        } else if (sideFilter === 'sell') {
            stages = getStagesForSide('SELL_SIDE');
        } else {
            stages = [...getStagesForSide('BUY_SIDE'), ...getStagesForSide('SELL_SIDE')];
        }
        
        // Convert to labels, then deduplicate
        return Array.from(new Set(stages.map(s => enumToLabel(s))));
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
                        onValueChange={(value) => onStatusFilterChange(value as 'all' | 'ACTIVE' | 'CLOSED_SUCCESSFULLY' | 'TERMINATED_EARLY')}
                    >
                        <SelectTrigger id="status-filter" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="all">{t('all')}</SelectItem>
                            <SelectItem value="ACTIVE">{t('active')}</SelectItem>
                            <SelectItem value="CLOSED_SUCCESSFULLY">{t('closed')}</SelectItem>
                            <SelectItem value="TERMINATED_EARLY">{t('terminated')}</SelectItem>
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
                        onValueChange={(value) => onSortByChange(value as 'dateAsc' | 'dateDesc' | 'lastUpdatedDesc' | 'lastUpdatedAsc')}
                    >
                        <SelectTrigger id="sort-by" className="w-full">
                            <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value="lastUpdatedDesc">{t('lastUpdatedDesc')}</SelectItem>
                            <SelectItem value="lastUpdatedAsc">{t('lastUpdatedAsc')}</SelectItem>
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

