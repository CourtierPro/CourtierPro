import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { type Transaction } from '@/features/transactions/api/queries';

interface TransactionInfoProps {
    transaction: Transaction;
}

export function TransactionInfo({ transaction }: TransactionInfoProps) {
    const { t } = useTranslation('transactions');

    return (
        <Section className="p-6">
            <div className="flex flex-col md:flex-row justify-between gap-6">
                <div>
                    <h2 className="text-2xl font-bold text-foreground mb-2">
                        {transaction.propertyAddress.street}
                    </h2>
                    <p className="text-muted-foreground">
                        {transaction.propertyAddress.city}, {transaction.propertyAddress.province}{' '}
                        {transaction.propertyAddress.postalCode}
                    </p>
                    <div className="flex items-center gap-4 mt-4">
                        <span
                            className={`px-3 py-1 rounded-full text-sm font-medium ${transaction.side === 'BUY_SIDE'
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-amber-100 text-amber-800'
                                }`}
                        >
                            {transaction.side === 'BUY_SIDE' ? t('buySide') : t('sellSide')}
                        </span>
                        <span
                            className={`px-3 py-1 rounded-full text-sm font-medium ${transaction.status === 'active'
                                ? 'bg-green-100 text-green-800'
                                : 'bg-gray-100 text-gray-800'
                                }`}
                        >
                            {t(transaction.status)}
                        </span>
                    </div>
                </div>
                <div className="text-right">
                    <p className="text-sm text-muted-foreground">{t('client')}</p>
                    <p className="text-lg font-medium text-foreground">{transaction.clientId}</p>
                    <p className="text-sm text-muted-foreground mt-2">{t('openedDate')}</p>
                    <p className="text-lg font-medium text-foreground">
                        {new Date(transaction.openedAt).toLocaleDateString()}
                    </p>
                </div>
            </div>
        </Section>
    );
}
