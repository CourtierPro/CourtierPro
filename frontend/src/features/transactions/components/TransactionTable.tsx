import { Users, Home } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { StatusBadge } from "@/shared/components/branded/StatusBadge";
import { getStagesForSide, enumToLabel, resolveStageIndex } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';

interface TransactionTableProps {
    transactions: Transaction[];
    onNavigate: (route: string) => void;
}

export function TransactionTable({ transactions, onNavigate }: TransactionTableProps) {
    const { t } = useTranslation('transactions');

    const getStageName = (tx: Transaction) => {
        const stageEnums = getStagesForSide(tx.side);
        const idx = resolveStageIndex(tx.currentStage, stageEnums);
        return enumToLabel(stageEnums[idx]);
    };

    return (
        <Section className="hidden lg:block p-0">
            <div className="overflow-x-auto">
                <table className="w-full" role="table">
                    <thead className="bg-muted/50">
                        <tr role="row">
                            <th className="p-4 text-left font-medium text-muted-foreground" scope="col">
                                {t('clientName')}
                            </th>
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
                                aria-label={`View transaction for ${transaction.clientId}`}
                            >
                                <td className="p-4 text-foreground">
                                    <div className="flex items-center gap-2">
                                        <Users className="w-4 h-4 text-primary" />
                                        {transaction.clientId}
                                    </div>
                                </td>
                                <td className="p-4 text-foreground">
                                    <div className="flex items-center gap-2">
                                        <Home className="w-4 h-4 text-muted-foreground/50" />
                                        {transaction.propertyAddress?.street}
                                    </div>
                                </td>
                                <td className="p-4">
                                    <span
                                        className={`px-3 py-1 rounded-full text-sm ${transaction.side === 'BUY_SIDE'
                                            ? 'bg-blue-100 text-blue-800'
                                            : 'bg-amber-100 text-amber-800'
                                            }`}
                                    >
                                        {transaction.side === 'BUY_SIDE' ? t('buy') : t('sell')}
                                    </span>
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
