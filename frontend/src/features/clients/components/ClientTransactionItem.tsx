import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Home, ArrowRight, Lock } from 'lucide-react';
import { Badge } from '@/shared/components/ui/badge';
import { getStagesForSide, getStageLabel, resolveStageIndex } from '@/shared/utils/stages';
import type { Transaction } from '@/features/transactions/api/queries';

interface ClientTransactionItemProps {
    transaction: Transaction;
    currentBrokerId?: string;
}

export function ClientTransactionItem({ transaction, currentBrokerId }: ClientTransactionItemProps) {
    const navigate = useNavigate();
    const { t } = useTranslation('clients');
    const { t: tTx } = useTranslation('transactions');

    // Check if current broker owns this transaction
    const isOwned = currentBrokerId && transaction.brokerId === currentBrokerId;

    const getStageName = () => {
        const stageEnums = getStagesForSide(transaction.side);
        const idx = resolveStageIndex(transaction.currentStage, stageEnums);
        const stageKey = stageEnums[idx];
        return getStageLabel(stageKey, tTx, transaction.side as 'BUY_SIDE' | 'SELL_SIDE');
    };

    const getStatusLabel = (status: string) => {
        const statusKey = status.toLowerCase();
        return t(`status.${statusKey}`, { defaultValue: status });
    };

    const getStatusVariant = (status: string): "default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "info" => {
        const s = status.toLowerCase();
        if (s === 'active') return 'success';
        if (s === 'closed_successfully') return 'secondary';
        if (s === 'terminated') return 'destructive';
        return 'secondary';
    };

    const handleClick = () => {
        // Only navigate if the current broker owns this transaction
        if (isOwned) {
            navigate(`/transactions/${transaction.transactionId}`);
        }
    };

    const address = transaction.propertyAddress?.street;

    return (
        <div
            onClick={isOwned ? handleClick : undefined}
            onKeyDown={isOwned ? (e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    handleClick();
                }
            } : undefined}
            role={isOwned ? 'button' : undefined}
            tabIndex={isOwned ? 0 : undefined}
            className={`w-full p-3 rounded-lg border border-border bg-card text-left transition-colors ${isOwned
                    ? 'hover:bg-accent/50 cursor-pointer focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2'
                    : 'opacity-75 cursor-default'
                }`}
            aria-label={isOwned ? `${t('viewTransaction')}: ${address || t('noPropertySelected')}` : undefined}
        >
            <div className="flex items-center justify-between gap-3">
                <div className="flex items-center gap-3 flex-1 min-w-0">
                    <Home className="h-4 w-4 flex-shrink-0 text-muted-foreground" />
                    <span className="truncate text-sm">
                        {address || <span className="italic text-muted-foreground">{t('noPropertySelected')}</span>}
                    </span>
                </div>
                {isOwned ? (
                    <ArrowRight className="h-4 w-4 flex-shrink-0 text-muted-foreground" />
                ) : (
                    <Lock className="h-4 w-4 flex-shrink-0 text-muted-foreground" />
                )}
            </div>
            <div className="flex items-center gap-2 mt-2 flex-wrap">
                <Badge variant={transaction.side === 'BUY_SIDE' ? 'info' : 'warning'}>
                    {transaction.side === 'BUY_SIDE' ? t('buy') : t('sell')}
                </Badge>
                <span className="text-xs text-muted-foreground">{getStageName()}</span>
                <Badge variant={getStatusVariant(transaction.status)} className="ml-auto">
                    {getStatusLabel(transaction.status)}
                </Badge>
            </div>
            {/* Show broker name */}
            {transaction.brokerName && (
                <div className="mt-2 text-xs text-muted-foreground">
                    {t('broker')}: {transaction.brokerName}
                </div>
            )}
        </div>
    );
}
