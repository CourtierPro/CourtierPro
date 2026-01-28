import { getStagesForSide, enumToLabel, resolveStageIndex, isTerminatedStage } from '@/shared/utils/stages';
import { useTranslation } from 'react-i18next';
import { useTransaction } from '@/features/transactions/api/queries';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { Button } from '@/shared/components/ui/button';
import { formatDate } from '@/shared/utils/date';
import { Badge } from "@/shared/components/ui/badge";

interface TransactionSummaryProps {
  transactionId: string;
  isReadOnly?: boolean;
}

export function TransactionSummary({ transactionId, isReadOnly = false }: TransactionSummaryProps) {
  const { t } = useTranslation('transactions');
  const { data: transaction, isLoading, error } = useTransaction(transactionId);

  if (isLoading) {
    return <LoadingState />;
  }

  if (error || !transaction) {
    return <ErrorState title={t('errorLoadingTransaction')} message={t('couldNotLoadTransaction')} />;
  }

  const stageEnums = getStagesForSide(transaction.side);
  const stages = stageEnums.map(enumToLabel);
  const stageIndex = resolveStageIndex(transaction.currentStage, stageEnums);
  const isTerminated = isTerminatedStage(transaction.currentStage, stageEnums) || transaction.status === 'TERMINATED_EARLY';
  const displayStage = stageIndex >= 0 ? stageIndex + 1 : 1;
  const totalStages = ((transaction.totalStages ?? stages.length) || 1);

  // We intentionally show only BE-provided transaction fields here.

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-foreground">
            {transaction.propertyAddress.street}
          </h2>
          <div className="text-sm text-muted-foreground mt-1">
            {transaction.propertyAddress.city}, {transaction.propertyAddress.province} {transaction.propertyAddress.postalCode}
          </div>
        </div>
        <Badge variant={transaction.side === 'BUY_SIDE' ? 'info' : 'warning'}>
          {transaction.side === 'BUY_SIDE' ? t('buySide') : t('sellSide')}
        </Badge>
      </div>

      <div className="p-6 rounded-xl shadow-md bg-card border border-border">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-foreground">
            {t('currentStage')}: <span className="text-primary">{stages[stageIndex]}</span>
          </h3>
          <span className="text-sm text-muted-foreground">
            {t('stepOf', { current: displayStage, total: totalStages })}
          </span>
        </div>

        <div className="relative">
          <div className="overflow-hidden h-2 mb-4 text-xs flex rounded bg-muted">
            <div
              style={{ width: `${(displayStage / totalStages) * 100}%` }}
              className={`shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center transition-all duration-500 ${isTerminated ? 'bg-destructive' : 'bg-primary'
                }`}
            />
          </div>
          <div className="flex justify-between text-xs text-muted-foreground">
            {stages.map((stage, idx) => (
              <div key={stage + '-' + idx} className={`flex flex-col items-center ${idx <= stageIndex ? 'text-primary font-medium' : ''}`}>
                <div className={`w-2 h-2 rounded-full mb-1 ${idx <= stageIndex ? (isTerminated && idx === stageIndex ? 'bg-destructive' : 'bg-primary') : 'bg-muted'}`} />
                <span className="hidden sm:block">{stage}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className={`grid grid-cols-1 ${isReadOnly ? 'lg:grid-cols-1' : 'lg:grid-cols-3'} gap-6`}>
        <div className={`${isReadOnly ? '' : 'lg:col-span-2'} p-6 rounded-xl shadow-md bg-card border border-border`}>
          <h3 className="text-lg font-semibold text-foreground mb-4">{t('transactionDetails')}</h3>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-6">
            <div>
              <dt className="text-sm font-medium text-muted-foreground">{t('client')}</dt>
              <dd className="mt-1 text-sm text-foreground">{transaction.clientName}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-muted-foreground">{t('status')}</dt>
              <dd className="mt-1">
                <Badge variant={transaction.status === 'ACTIVE' ? 'success' : transaction.status === 'CLOSED_SUCCESSFULLY' ? 'secondary' : 'destructive'}>
                  {t(transaction.status)}
                </Badge>
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-muted-foreground">{t('openedDate')}</dt>
              <dd className="mt-1 text-sm text-foreground">
                {formatDate(transaction.openedDate ?? transaction.openedAt)}
              </dd>
            </div>
            <div>
              <dd className="mt-1 text-sm text-foreground font-mono">{transaction.transactionId}</dd>
            </div>
          </dl>
        </div>

        {!isReadOnly && (
          <div className="space-y-6">
            <div className="p-6 rounded-xl shadow-md bg-card border border-border">
              <h3 className="text-lg font-semibold text-foreground mb-4">{t('quickActions')}</h3>
              <div className="space-y-3">
                <Button className="w-full">
                  {t('updateStage')}
                </Button>
                <Button variant="outline" className="w-full">
                  {t('addNote')}
                </Button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}