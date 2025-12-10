import { Button } from "@/shared/components/ui/button";
import { useTranslation } from "react-i18next";
import { DocumentStatusEnum } from "../types";

export type FilterStatus = 'ALL' | DocumentStatusEnum;

interface StatusFilterBarProps {
    currentFilter: FilterStatus;
    onFilterChange: (status: FilterStatus) => void;
    counts: Record<FilterStatus, number>;
}

export function StatusFilterBar({ currentFilter, onFilterChange, counts }: StatusFilterBarProps) {
    const { t } = useTranslation("documents");

    const filters: { value: FilterStatus; label: string }[] = [
        { value: 'ALL', label: t('filters.all', 'All') },
        { value: DocumentStatusEnum.REQUESTED, label: t('filters.actionRequired', 'Action Required') },
        { value: DocumentStatusEnum.SUBMITTED, label: t('filters.submitted', 'Submitted') },
        { value: DocumentStatusEnum.APPROVED, label: t('filters.approved', 'Approved') },
    ];

    return (
        <div className="flex flex-wrap gap-2 pb-2">
            {filters.map((filter) => (
                <Button
                    key={filter.value}
                    variant={currentFilter === filter.value ? "default" : "outline"}
                    size="sm"
                    onClick={() => onFilterChange(filter.value)}
                    className="rounded-full"
                >
                    {filter.label}
                    <span className="ml-2 bg-white/20 px-1.5 py-0.5 rounded text-xs">
                        {counts[filter.value] || 0}
                    </span>
                </Button>
            ))}
        </div>
    );
}
