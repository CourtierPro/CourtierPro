import { Users, Home, Pin } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';
import { usePinTransaction, useUnpinTransaction } from '@/features/transactions/api/mutations';
import { formatDate } from '@/shared/utils/date';

interface TransactionCardsProps {
    transactions: Transaction[];
    onNavigate: (route: string) => void;
    pinnedIds: Set<string>;
}

export function TransactionCards({ transactions, onNavigate, pinnedIds }: TransactionCardsProps) {
    const { t } = useTranslation('transactions');
    const pinMutation = usePinTransaction();
    const unpinMutation = useUnpinTransaction();

    const getStageName = (tx: Transaction) => {
        const stageEnums = getStagesForSide(tx.side);
        const idx = resolveStageIndex(tx.currentStage, stageEnums);
        return enumToLabel(stageEnums[idx]);
    };

    const handlePinToggle = (e: React.MouseEvent, transactionId: string) => {
        e.stopPropagation();
        if (pinnedIds.has(transactionId)) {
            unpinMutation.mutate(transactionId);
        } else {
            pinMutation.mutate(transactionId);
        }
    };

    return (
        <div className="lg:hidden space-y-4" role="list">
            {transactions.map((transaction) => {
                const isPinned = pinnedIds.has(transaction.transactionId);
                return (
                    <div
                        key={transaction.transactionId}
                        role="listitem"
                        className={`relative w-full p-4 rounded-xl shadow-sm border border-border bg-card text-card-foreground hover:shadow-md focus-within:ring-2 focus-within:ring-primary focus-within:ring-offset-2 transition-all ${isPinned ? 'ring-1 ring-primary/30' : ''}`}
                    >
                        <Button
                            variant="ghost"
                            size="icon"
                            className="absolute top-2 right-2 h-8 w-8"
                            onClick={(e) => handlePinToggle(e, transaction.transactionId)}
                            aria-label={isPinned ? t('unpin') : t('pin')}
                            title={isPinned ? t('unpin') : t('pin')}
                        >
                            <Pin className={`w-4 h-4 ${isPinned ? 'text-primary fill-primary' : 'text-muted-foreground'}`} />
                        </Button>
                        <button
                            onClick={() => {
                                if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                            }}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter' || e.key === ' ') {
                                    e.preventDefault();
                                    if (transaction?.transactionId) onNavigate(`/transactions/${transaction.transactionId}`);
                                }
                            }}
                            tabIndex={0}
                            className="w-full text-left focus:outline-none"
                            aria-label={`View transaction for ${transaction.clientName}${isPinned ? ' (Pinned)' : ''}`}
                        >
                            <div className="space-y-3 pr-8">
                                <div className="flex items-start justify-between gap-2">
                                    <div className="flex items-center gap-2 flex-1">
                                        <Users className="w-5 h-5 flex-shrink-0 text-primary" />
                                        <p className="font-medium">{transaction.clientName}</p>
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
                    </div>
                );
            })}
        </div>
    );
}
