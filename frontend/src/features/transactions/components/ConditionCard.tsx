import { useTranslation } from 'react-i18next';
import { Calendar, FileCheck, AlertTriangle } from 'lucide-react';
import { Badge } from '@/shared/components/ui/badge';
import { Section } from '@/shared/components/branded/Section';
import type { Condition, ConditionStatus, ConditionType } from '@/shared/api/types';

interface ConditionCardProps {
    condition: Condition;
    onClick?: () => void;
    isReadOnly?: boolean;
}

const statusVariantMap: Record<ConditionStatus, 'default' | 'secondary' | 'destructive' | 'outline'> = {
    PENDING: 'secondary',
    SATISFIED: 'default',
    FAILED: 'destructive',
};

/**
 * Calculate deadline status based on days remaining.
 * Returns: 'overdue' | 'urgent' (<=3 days) | 'warning' (<=7 days) | 'normal'
 */
function getDeadlineStatus(deadlineDate: string): 'overdue' | 'urgent' | 'warning' | 'normal' {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const deadline = new Date(deadlineDate);
    deadline.setHours(0, 0, 0, 0);

    const diffTime = deadline.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    if (diffDays < 0) return 'overdue';
    if (diffDays <= 3) return 'urgent';
    if (diffDays <= 7) return 'warning';
    return 'normal';
}

const deadlineStyles: Record<'overdue' | 'urgent' | 'warning' | 'normal', string> = {
    overdue: 'text-red-600 dark:text-red-400 font-medium',
    urgent: 'text-orange-600 dark:text-orange-400 font-medium',
    warning: 'text-yellow-600 dark:text-yellow-400',
    normal: 'text-muted-foreground',
};

const deadlineBadgeStyles: Record<'overdue' | 'urgent' | 'warning' | 'normal', string> = {
    overdue: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
    urgent: 'bg-orange-100 text-orange-800 dark:bg-orange-900/30 dark:text-orange-400',
    warning: 'bg-yellow-100 text-yellow-800 dark:bg-yellow-900/30 dark:text-yellow-400',
    normal: '',
};

export function ConditionCard({ condition, onClick, isReadOnly }: ConditionCardProps) {
    const { t, i18n } = useTranslation('transactions');

    const getConditionTitle = (type: ConditionType, customTitle?: string) => {
        if (type === 'OTHER' && customTitle) return customTitle;
        return t(`conditionTypes.${type}`);
    };


    const formatDate = (dateStr: string) => {
        // Parse date parts directly to avoid timezone issues
        const [year, month, day] = dateStr.split('-').map(Number);
        const date = new Date(year, month - 1, day); // month is 0-indexed
        // Use locale from i18n: 'en' -> 'en-CA', 'fr' -> 'fr-CA'
        const locale = i18n.language === 'fr' ? 'fr-CA' : 'en-CA';
        return date.toLocaleDateString(locale, {
            month: 'short',
            day: 'numeric',
            year: 'numeric',
        });
    };

    // Only show deadline warnings for pending conditions
    const deadlineStatus = condition.status === 'PENDING'
        ? getDeadlineStatus(condition.deadlineDate)
        : 'normal';

    const showDeadlineBadge = condition.status === 'PENDING' &&
        (deadlineStatus === 'overdue' || deadlineStatus === 'urgent');

    return (
        <Section
            className={`p-4 transition-colors ${!isReadOnly ? 'cursor-pointer hover:bg-muted/50' : ''}`}
            onClick={!isReadOnly ? onClick : undefined}
            tabIndex={!isReadOnly ? 0 : undefined}
            role={!isReadOnly ? 'button' : undefined}
            aria-label={t('viewConditionDetails')}
        >
            <div className="flex items-start justify-between gap-2">
                <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                        <FileCheck className="w-4 h-4 text-muted-foreground flex-shrink-0" />
                        <span className="font-medium truncate">
                            {getConditionTitle(condition.type, condition.customTitle)}
                        </span>
                    </div>
                    <p className="text-sm text-muted-foreground pl-6 mb-2 line-clamp-2">
                        {condition.description}
                    </p>
                    <div className={`flex items-center gap-1 text-sm pl-6 ${deadlineStyles[deadlineStatus]}`}>
                        <Calendar className="w-3.5 h-3.5" />
                        <span>{t('conditions.deadline')}: {formatDate(condition.deadlineDate)}</span>
                        {showDeadlineBadge && (
                            <span className={`ml-2 px-2 py-0.5 rounded text-xs ${deadlineBadgeStyles[deadlineStatus]}`}>
                                {deadlineStatus === 'overdue' && (
                                    <>
                                        <AlertTriangle className="w-3 h-3 inline mr-1" />
                                        {t('overdue')}
                                    </>
                                )}
                                {deadlineStatus === 'urgent' && t('dueSoon')}
                            </span>
                        )}
                    </div>
                </div>
                <Badge variant={statusVariantMap[condition.status]}>
                    {t(`conditionStatus.${condition.status}`)}
                </Badge>
            </div>
        </Section>
    );
}
