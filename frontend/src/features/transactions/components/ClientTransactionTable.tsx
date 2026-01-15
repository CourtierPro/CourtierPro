import { Home } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { Badge } from "@/shared/components/ui/badge";
import { getStagesForSide, resolveStageIndex, getStageLabel } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';

interface ClientTransactionTableProps {
    transactions: Transaction[];
    onNavigate: (route: string) => void;
}

export function ClientTransactionTable({ transactions, onNavigate }: ClientTransactionTableProps) {
    const { t } = useTranslation('transactions');

    const getStageName = (tx: Transaction) => {
        const stageEnums = getStagesForSide(tx.side);
        const idx = resolveStageIndex(tx.currentStage, stageEnums);
        // Use getStageLabel for translated value
        return getStageLabel(stageEnums[idx], t, tx.side);
    };

    return (
        <Section className="hidden lg:block p-0">
            <div className="overflow-x-auto">
                <table className="w-full" role="table">
                    <thead className="bg-muted/50">
                        <tr role="row">
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('propertyAddress')}
                            </th>
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('side')}
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
                        </tr>
                    </thead>
                    <tbody>
                        {transactions.map((transaction) => (
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
                                className="border-t border-border hover:bg-muted/50 focus:outline-none focus:ring-2 focus:ring-inset focus:ring-primary cursor-pointer transition-colors"
                                aria-label={`View transaction for ${transaction.propertyAddress?.street}`}
                            >
                                <td className="p-4 text-foreground">
                                    <div className="flex items-center gap-2">
                                        <Home className="w-4 h-4 text-muted-foreground/50" />
                                        {transaction.propertyAddress?.street ? (
                                            transaction.propertyAddress.street
                                        ) : transaction.side === 'BUY_SIDE' ? (
                                            <span className="text-muted-foreground italic">{t('noPropertySelected')}</span>
                                        ) : (
                                            <span className="text-muted-foreground">-</span>
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
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
        </Section>
    );
}
