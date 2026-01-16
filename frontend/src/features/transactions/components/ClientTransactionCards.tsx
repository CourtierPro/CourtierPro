import { Home } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { Badge } from "@/shared/components/ui/badge";
import { getStagesForSide, resolveStageIndex, getStageLabel } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';
import { formatDate } from '@/shared/utils/date';

interface ClientTransactionCardsProps {
    transactions: Transaction[];
    onNavigate: (route: string) => void;
}

export function ClientTransactionCards({ transactions, onNavigate }: ClientTransactionCardsProps) {
    const { t } = useTranslation('transactions');

    const getStageName = (tx: Transaction) => {
        const stageEnums = getStagesForSide(tx.side);
        const idx = resolveStageIndex(tx.currentStage, stageEnums);
        // Use getStageLabel for translated value
        return getStageLabel(stageEnums[idx], t, tx.side);
    };

    return (
        <div className="lg:hidden space-y-4">
            {transactions.map((transaction) => (
                <button
                    key={transaction.transactionId}
                    onClick={() => {
                        if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                    }}
                    onKeyDown={(e) => {
                        if (e.key === 'Enter' || e.key === ' ') {
                            e.preventDefault();
                            if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                        }
                    }}
                    className="w-full p-4 rounded-xl shadow-sm border border-border bg-card text-card-foreground hover:shadow-md focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 transition-all text-left"
                    aria-label={`View transaction for ${transaction.propertyAddress?.street}`}
                >
                    <div className="space-y-3">
                        <div className="flex items-start justify-between gap-2">
                            <div className="flex items-center gap-2 flex-1">
                                <Home className="w-5 h-5 flex-shrink-0 text-muted-foreground/50" />
                                <p className="font-medium">
                                    {transaction.propertyAddress?.street ? (
                                        transaction.propertyAddress.street
                                    ) : transaction.side === 'BUY_SIDE' ? (
                                        <span className="text-muted-foreground italic font-normal">{t('noPropertySelected')}</span>
                                    ) : (
                                        <span className="text-muted-foreground">-</span>
                                    )}
                                </p>
                            </div>
                            <StatusBadge status={transaction.status} />
                        </div>

                        <div className="flex items-center justify-between">
                            <Badge variant={transaction.side === 'BUY_SIDE' ? 'info' : 'warning'}>
                                {transaction.side === 'BUY_SIDE' ? t('buy') : t('sell')}
                            </Badge>
                            <p className="text-sm text-foreground">
                                {getStageName(transaction)}
                            </p>
                        </div>

                        <p className="text-sm text-muted-foreground">
                            {t('openedDate')}: {formatDate(transaction.openedDate ?? transaction.openedAt)}
                        </p>
                    </div>
                </button>
            ))}
        </div>
    );
}
