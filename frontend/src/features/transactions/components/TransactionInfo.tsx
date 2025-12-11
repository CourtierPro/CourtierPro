import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { type Transaction } from '@/features/transactions/api/queries';
import { formatDate } from '@/shared/utils/date';

interface TransactionInfoProps {
    transaction: Transaction;
}

function formatAddress(address: Transaction['propertyAddress']) {
    const street = address.street.trim();
    if (!street) {
        return "Address not available";
    }
    // If street is just a number (e.g. "1"), treat it as needing a label
    if (/^\d+$/.test(street)) {
        return `Street Number: ${street}`;
    }
    return street;
}

export function TransactionInfo({ transaction }: TransactionInfoProps) {
    const { t } = useTranslation('transactions');

    return (
        <Section className="p-3 md:p-6">
            <div className="flex flex-col md:flex-row justify-between gap-2 md:gap-6">
                <div>
                    <h2 className="text-lg md:text-2xl font-bold text-foreground mb-1 md:mb-2 line-clamp-1">
                        {formatAddress(transaction.propertyAddress)}
                    </h2>
                    <p className="text-sm md:text-base text-muted-foreground">
                        {transaction.propertyAddress.city}, {transaction.propertyAddress.province}{' '}
                        {transaction.propertyAddress.postalCode}
                    </p>
                    <div className="flex items-center gap-2 md:gap-4 mt-2 md:mt-4">
                        <span
                            className={`px-2 md:px-3 py-0.5 md:py-1 rounded-full text-xs md:text-sm font-medium ${transaction.side === 'BUY_SIDE'
                                ? 'bg-blue-100 text-blue-800'
                                : 'bg-amber-100 text-amber-800'
                                }`}
                        >
                            {transaction.side === 'BUY_SIDE' ? t('buySide') : t('sellSide')}
                        </span>
                        <span
                            className={`px-2 md:px-3 py-0.5 md:py-1 rounded-full text-xs md:text-sm font-medium ${transaction.status === 'active'
                                ? 'bg-green-100 text-green-800'
                                : 'bg-gray-100 text-gray-800'
                                }`}
                        >
                            {t(transaction.status)}
                        </span>
                    </div>
                </div>
                <div className="text-left md:text-right mt-1 md:mt-0">
                    <p className="text-xs md:text-sm text-muted-foreground">{t('client')}</p>
                    <p className="text-base md:text-lg font-medium text-foreground">{transaction.clientName}</p>
                    <p className="text-xs md:text-sm text-muted-foreground mt-1 md:mt-2">{t('openedDate')}</p>
                    <p className="text-base md:text-lg font-medium text-foreground">
                        {formatDate(transaction.openedDate ?? transaction.openedAt)}
                    </p>
                </div>
            </div>
        </Section>
    );
}
