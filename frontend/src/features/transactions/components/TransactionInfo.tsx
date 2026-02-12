import { useTranslation } from 'react-i18next';
import { Section } from '@/shared/components/branded/Section';
import { type Transaction } from '@/features/transactions/api/queries';
import { formatDate } from '@/shared/utils/date';
import { Badge } from '@/shared/components/ui/badge';

interface TransactionInfoProps {
  transaction: Transaction;
  hideClientLabel?: boolean;
  onClientClick?: () => void;
}

export function TransactionInfo({ transaction, hideClientLabel = false, onClientClick }: TransactionInfoProps) {
  const { t } = useTranslation('transactions');

  const formatAddress = (address: Transaction['propertyAddress'] | null): string => {
    if (!address || !address.street) {
      return t('noPropertySelected');
    }
    const street = address.street.trim();
    if (!street) {
      return t('noPropertySelected');
    }
    if (/^\d+$/.test(street)) {
      return t('streetAddressIncomplete');
    }
    return street;
  };

  return (
    <Section className="p-3 md:p-6 h-full">
      <div className="flex flex-col gap-2">
        <h2 className="text-lg md:text-xl font-bold text-foreground line-clamp-2">
          {formatAddress(transaction.propertyAddress)}
        </h2>
        {transaction.propertyAddress?.street?.trim() && (
          <p className="text-sm text-muted-foreground">
            {transaction.propertyAddress.city}, {transaction.propertyAddress.province}{' '}
            {transaction.propertyAddress.postalCode}
          </p>
        )}
        {transaction.centrisNumber && (
          <p className="text-sm text-muted-foreground">
            {t('centrisNumber')}: {transaction.centrisNumber}
          </p>
        )}
        <div className="flex items-center gap-2 mt-1">
          <Badge variant={transaction.side === 'BUY_SIDE' ? 'info' : 'warning'}>
            {transaction.side === 'BUY_SIDE' ? t('buySide') : t('sellSide')}
          </Badge>
          <Badge variant={transaction.status === 'ACTIVE' ? 'success' : transaction.status === 'CLOSED_SUCCESSFULLY' ? 'secondary' : 'destructive'}>
            {t(transaction.status)}
          </Badge>
        </div>
        {!hideClientLabel && (
          <div className="mt-2">
            <p className="text-xs text-muted-foreground">{t('client')}</p>
            {onClientClick ? (
              <button
                onClick={onClientClick}
                className="text-sm font-medium text-primary hover:underline focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 rounded"
              >
                {transaction.clientName}
              </button>
            ) : (
              <p className="text-sm font-medium text-foreground">{transaction.clientName}</p>
            )}
          </div>
        )}
        <div className={hideClientLabel ? 'mt-2' : ''}>
          <p className="text-xs text-muted-foreground">{t('openedDate')}</p>
          <p className="text-sm font-medium text-foreground">
            {formatDate(transaction.openedDate ?? transaction.openedAt)}
          </p>
        </div>
        {transaction.side === 'BUY_SIDE' && transaction.houseVisitCount != null && (
          <div className="mt-2">
            <p className="text-xs text-muted-foreground">{t('houseVisits')}</p>
            <p className="text-sm font-medium text-foreground">{transaction.houseVisitCount}</p>
          </div>
        )}
      </div>
    </Section>
  );
}
