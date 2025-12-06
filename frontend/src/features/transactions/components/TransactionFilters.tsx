import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { getStagesForSide, enumToLabel } from '@/shared/utils/stages';

interface TransactionFiltersProps {
    sideFilter: 'all' | 'buy' | 'sell';
    statusFilter: 'all' | 'active' | 'closed' | 'terminated';
    stageFilter: string;
    sortBy: 'dateAsc' | 'dateDesc' | 'nameAsc' | 'nameDesc';
    onSideFilterChange: (value: 'all' | 'buy' | 'sell') => void;
    onStatusFilterChange: (value: 'all' | 'active' | 'closed' | 'terminated') => void;
    onStageFilterChange: (value: string) => void;
    onSortByChange: (value: 'dateAsc' | 'dateDesc' | 'nameAsc' | 'nameDesc') => void;
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
        // Deduplicate by enum
        const uniqueEnums = Array.from(new Set(combined));
        return uniqueEnums.map(enumToLabel);
    })();

    return (
        <Section className="p-4">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
                {/* Transaction Side Filter */}
                <div>
                    <label
                        htmlFor="side-filter"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('transactionSide')}
                    </label>
                    <select
                        id="side-filter"
                        value={sideFilter}
                        onChange={(e) => onSideFilterChange(e.target.value as 'all' | 'buy' | 'sell')}
                        className="w-full p-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    >
                        <option value="all">{t('all')}</option>
                        <option value="buy">{t('buy')}</option>
                        <option value="sell">{t('sell')}</option>
                    </select>
                </div>

                {/* Status Filter */}
                <div>
                    <label
                        htmlFor="status-filter"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('status')}
                    </label>
                    <select
                        id="status-filter"
                        value={statusFilter}
                        onChange={(e) => onStatusFilterChange(e.target.value as 'all' | 'active' | 'closed' | 'terminated')}
                        className="w-full p-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    >
                        <option value="all">{t('all')}</option>
                        <option value="active">{t('active')}</option>
                        <option value="closed">{t('closed')}</option>
                        <option value="terminated">{t('terminated')}</option>
                    </select>
                </div>

                {/* Stage Filter */}
                <div>
                    <label
                        htmlFor="stage-filter"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('stage')}
                    </label>
                    <select
                        id="stage-filter"
                        value={stageFilter}
                        onChange={(e) => onStageFilterChange(e.target.value)}
                        className="w-full p-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                        disabled={sideFilter === 'all'}
                    >
                        <option value="all">{t('all')}</option>
                        {availableStages.map((stage) => (
                            <option key={stage} value={stage}>
                                {stage}
                            </option>
                        ))}
                    </select>
                </div>

                {/* Sort By */}
                <div>
                    <label
                        htmlFor="sort-by"
                        className="block mb-2 text-sm font-medium text-foreground"
                    >
                        {t('sortBy')}
                    </label>
                    <select
                        id="sort-by"
                        value={sortBy}
                        onChange={(e) => onSortByChange(e.target.value as 'dateAsc' | 'dateDesc' | 'nameAsc' | 'nameDesc')}
                        className="w-full p-2 rounded-lg border border-input bg-background text-foreground focus:outline-none focus:ring-2 focus:ring-primary"
                    >
                        <option value="dateDesc">{t('dateOpenedDesc')}</option>
                        <option value="dateAsc">{t('dateOpenedAsc')}</option>
                        {/* Disabled until backend returns clientName */}
                        <option value="nameAsc" disabled>
                            {t('clientNameAsc')} (Not available)
                        </option>
                        <option value="nameDesc" disabled>
                            {t('clientNameDesc')} (Not available)
                        </option>
                    </select>
                </div>

                {/* Clear Filters Button */}
                <div className="flex items-end">
                    <button
                        onClick={onResetFilters}
                        disabled={!hasActiveFilters}
                        className="w-full p-2 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 bg-secondary text-secondary-foreground"
                    >
                        <X className="w-4 h-4" />
                        {t('clearFilters')}
                    </button>
                </div>
            </div>
        </Section>
    );
}
