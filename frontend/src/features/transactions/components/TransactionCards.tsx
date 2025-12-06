import { Users, Home } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';
import { formatDate } from '@/shared/utils/date';

interface TransactionCardsProps {
    transactions: Transaction[];
    onNavigate: (route: string) => void;
}

export function TransactionCards({ transactions, onNavigate }: TransactionCardsProps) {
    const { t } = useTranslation('transactions');

    const getStageName = (tx: Transaction) => {
        const stageEnums = getStagesForSide(tx.side);
        const idx = resolveStageIndex(tx.currentStage, stageEnums);
        return enumToLabel(stageEnums[idx]);
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
                    aria-label={`View transaction for ${transaction.clientId}`}
                >
                    <div className="space-y-3">
                        <div className="flex items-start justify-between gap-2">
                            <div className="flex items-center gap-2 flex-1">
                                <Users className="w-5 h-5 flex-shrink-0 text-primary" />
                                <p className="font-medium">{transaction.clientId}</p>
                            </div>
                            <StatusBadge status={transaction.status} />
                        </div>

                        <div className="flex items-start gap-2">
                            <Home className="w-4 h-4 flex-shrink-0 mt-1 text-muted-foreground/50" />
                            <p className="text-sm text-muted-foreground">
                                {transaction.propertyAddress?.street}
                            </p>
                        </div>

                        <div className="flex items-center justify-between">
                            <span
                                className={`px-3 py-1 rounded-full text-sm ${transaction.side === 'BUY_SIDE'
                                    ? 'bg-blue-100 text-blue-800'
                                    : 'bg-amber-100 text-amber-800'
                                    }`}
                            >
                                {transaction.side === 'BUY_SIDE' ? t('buy') : t('sell')}
                            </span>
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
