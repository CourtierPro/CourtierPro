import { Users, Home, Pin, Archive, ArchiveRestore } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { Badge } from "@/shared/components/ui/badge";
import { Button } from "@/shared/components/ui/button";
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';
import { usePinTransaction, useUnpinTransaction, useArchiveTransaction, useUnarchiveTransaction } from '@/features/transactions/api/mutations';

interface TransactionTableProps {
    transactions: Transaction[];
    onNavigate: (route: string) => void;
    pinnedIds: Set<string>;
    showArchived?: boolean;
}

export function TransactionTable({ transactions, onNavigate, pinnedIds, showArchived = false }: TransactionTableProps) {
    const { t } = useTranslation('transactions');
    const pinMutation = usePinTransaction();
    const unpinMutation = useUnpinTransaction();
    const archiveMutation = useArchiveTransaction();
    const unarchiveMutation = useUnarchiveTransaction();

    const getStageName = (tx: Transaction) => {
        const stageEnums = getStagesForSide(tx.side);
        const idx = resolveStageIndex(tx.currentStage, stageEnums);
        const stageKey = stageEnums[idx];
        const sideKey = tx.side === 'BUY_SIDE' ? 'buy' : 'sell';
        return t(`stages.${sideKey}.${stageKey.toLowerCase()}`, { defaultValue: enumToLabel(stageKey) });
    };

    const handlePinToggle = (e: React.MouseEvent, transactionId: string) => {
        e.stopPropagation();
        if (pinnedIds.has(transactionId)) {
            unpinMutation.mutate(transactionId);
        } else {
            pinMutation.mutate(transactionId);
        }
    };

    const handleArchiveToggle = (e: React.MouseEvent, transactionId: string, isArchived: boolean) => {
        e.stopPropagation();
        if (isArchived) {
            unarchiveMutation.mutate(transactionId);
        } else {
            archiveMutation.mutate(transactionId);
        }
    };

    return (
        <Section className="hidden lg:block p-0">
            <div className="overflow-x-auto">
                <table className="w-full" role="table">
                    <thead className="bg-muted/50">
                        <tr role="row">
                            <th className="p-4 text-left font-medium text-muted-foreground w-12" scope="col">
                                <span className="sr-only">{t('pin')}</span>
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('clientName')}
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('propertyAddress')}
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('sideLabel')}
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('currentStage')}
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('statusLabel')}
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('openedDate')}
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground w-12" scope="col">
                                <span className="sr-only">{t('actions')}</span>
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        {transactions.map((transaction) => {
                            const isPinned = pinnedIds.has(transaction.transactionId);
                            return (
                                <tr
                                    key={transaction.transactionId}
                                    role="row"
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
                                    className={`border-t border-border hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary cursor-pointer transition-colors ${isPinned ? 'bg-primary/5' : ''}`}
                                    aria-label={`View transaction for ${transaction.clientName}${isPinned ? ' (Pinned)' : ''}`}
                                >
                                    <td className="p-4">
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className="h-8 w-8"
                                            onClick={(e) => handlePinToggle(e, transaction.transactionId)}
                                            aria-label={isPinned ? t('unpin') : t('pin')}
                                            title={isPinned ? t('unpin') : t('pin')}
                                        >
                                            <Pin className={`w-4 h-4 ${isPinned ? 'text-primary fill-primary' : 'text-muted-foreground'}`} />
                                        </Button>
                                    </td>
                                    <td className="p-4 text-foreground">
                                        <div className="flex items-center gap-2">
                                            <Users className="w-4 h-4 text-primary" />
                                            {transaction.clientName}
                                        </div>
                                    </td>
                                    <td className="p-4 text-foreground">
                                        <div className="flex items-center gap-2">
                                            <Home className="w-4 h-4 text-muted-foreground/50" />
                                            {transaction.propertyAddress?.street || (
                                                <span className="text-muted-foreground italic">{t('noPropertySelected')}</span>
                                            )}
                                        </div>
                                    </td>
                                    <td className="p-4">
                                        <Badge variant={transaction.side === 'BUY_SIDE' ? 'info' : 'warning'}>
                                            {transaction.side === 'BUY_SIDE' ? t('buy') : t('sell')}
                                        </Badge>
                                    </td>
                                    <td className="p-4 text-foreground">
                                        {getStageName(transaction)}
                                    </td>
                                    <td className="p-4">
                                        <StatusBadge status={transaction.status} />
                                    </td>
                                    <td className="p-4 text-muted-foreground">
                                        {transaction.openedDate ?? (transaction.openedAt ? transaction.openedAt.substring(0, 10) : '')}
                                    </td>
                                    <td className="p-4">
                                        <Button
                                            variant="ghost"
                                            size="icon"
                                            className="h-8 w-8"
                                            onClick={(e) => handleArchiveToggle(e, transaction.transactionId, showArchived)}
                                            aria-label={showArchived ? t('unarchive') : t('archive')}
                                            title={showArchived ? t('unarchive') : t('archive')}
                                        >
                                            {showArchived ? (
                                                <ArchiveRestore className="w-4 h-4 text-muted-foreground hover:text-primary" />
                                            ) : (
                                                <Archive className="w-4 h-4 text-muted-foreground hover:text-primary" />
                                            )}
                                        </Button>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </Section>
    );
}
