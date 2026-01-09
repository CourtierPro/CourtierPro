import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { type Transaction } from '@/features/transactions/api/queries';
import { formatDate } from '@/shared/utils/date';
import { Badge } from "@/shared/components/ui/badge";

interface TransactionInfoProps {
    transaction: Transaction;
    hideClientLabel?: boolean;
}

function formatAddress(address: Transaction['propertyAddress']) {
    const street = address.street.trim();
    if (!street) {
        return "No property selected";
    }
    // If street is just a number (e.g. "1"), treat it as needing a label
    if (/^\d+$/.test(street)) {
        return "Street address incomplete";
    }
    return street;
}

export function TransactionInfo({ transaction, hideClientLabel = false }: TransactionInfoProps) {
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
                        <Badge variant={transaction.side === 'BUY_SIDE' ? 'info' : 'warning'}>
                            {transaction.side === 'BUY_SIDE' ? t('buySide') : t('sellSide')}
                        </Badge>
                        <Badge variant={transaction.status === 'ACTIVE' ? 'success' : transaction.status === 'CLOSED_SUCCESSFULLY' ? 'secondary' : 'destructive'}>
                            {t(transaction.status)}
                        </Badge>
                    </div>
                </div>
                <div className="text-left md:text-right mt-1 md:mt-0">
                    {!hideClientLabel && (
                        <>
                            <p className="text-xs md:text-sm text-muted-foreground">{t('client')}</p>
                            <p className="text-base md:text-lg font-medium text-foreground">{transaction.clientName}</p>
                        </>
                    )}
                    <p className={`text-xs md:text-sm text-muted-foreground ${hideClientLabel ? '' : 'mt-1 md:mt-2'}`}>{t('openedDate')}</p>
                    <p className="text-base md:text-lg font-medium text-foreground">
                        {formatDate(transaction.openedDate ?? transaction.openedAt)}
                    </p>
                </div>
            </div>
        </Section>
    );
}
