import { getStagesForSide, enumToLabel, resolveStageIndex, isTerminatedStage } from '@/shared/utils/stages';
/**
 * TransactionSummary Component
 * 
 * Displays a high-level summary of a transaction, including its current stage and progress.
 * Uses `useTransaction` query to fetch data, ensuring it stays up-to-date.
 */
import { useTranslation } from 'react-i18next';
import { useTransaction } from '@/features/transactions/api/queries';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { Button } from '@/shared/components/ui/button';

interface TransactionSummaryProps {
  transactionId: string;
}

export function TransactionSummary({ transactionId }: TransactionSummaryProps) {
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
  const isTerminated = isTerminatedStage(transaction.currentStage, stageEnums) || transaction.status === 'terminated';
  const displayStage = stageIndex >= 0 ? stageIndex + 1 : 1;
  const totalStages = ((transaction.totalStages ?? stages.length) || 1);

  // We intentionally show only BE-provided transaction fields here.

  return (
    <div className="space-y-6">
      {/* Header Section */}
      <div className="flex items-start justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">
            {transaction.propertyAddress.street}
          </h2>
          <div className="text-sm text-muted-foreground mt-1">
            {transaction.propertyAddress.city}, {transaction.propertyAddress.province} {transaction.propertyAddress.postalCode}
          </div>
        </div>
        <div className={`px-3 py-1 rounded-full text-sm font-medium ${transaction.side === 'BUY_SIDE' ? 'bg-blue-100 text-blue-800' : 'bg-green-100 text-green-800'
          }`}>
          {transaction.side === 'BUY_SIDE' ? t('buySide') : t('sellSide')}
        </div>
      </div>

      {/* Stage Tracker */}
      <div className="p-6 rounded-xl shadow-md bg-white border border-gray-100">
        <div className="flex items-center justify-between mb-6">
          <h3 className="text-lg font-semibold text-gray-900">
            {t('currentStage')}: <span className="text-orange-600">{stages[stageIndex]}</span>
          </h3>
          <span className="text-sm text-muted-foreground">
            {t('stepOf', { current: displayStage, total: totalStages })}
          </span>
        </div>

        {/* Progress Bar */}
        <div className="relative">
          <div className="overflow-hidden h-2 mb-4 text-xs flex rounded bg-gray-100">
            <div
              style={{ width: `${(displayStage / totalStages) * 100}%` }}
              className={`shadow-none flex flex-col text-center whitespace-nowrap text-white justify-center transition-all duration-500 ${isTerminated ? 'bg-red-500' : 'bg-orange-500'
                }`}
            />
          </div>
          <div className="flex justify-between text-xs text-gray-500">
            {stages.map((stage, idx) => (
              <div key={stage} className={`flex flex-col items-center ${idx <= stageIndex ? 'text-orange-600 font-medium' : ''}`}>
                <div className={`w-2 h-2 rounded-full mb-1 ${idx <= stageIndex ? (isTerminated && idx === stageIndex ? 'bg-red-500' : 'bg-orange-500') : 'bg-gray-200'}`} />
                <span className="hidden sm:block">{stage}</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Transaction Details Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Info */}
        <div className="lg:col-span-2 p-6 rounded-xl shadow-md" style={{ backgroundColor: '#FFFFFF' }}>
          <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('transactionDetails')}</h3>
          <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-6">
            <div>
              <dt className="text-sm font-medium text-gray-500">{t('client')}</dt>
              <dd className="mt-1 text-sm text-gray-900">{transaction.clientId}</dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t('status')}</dt>
              <dd className="mt-1">
                <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium capitalize ${transaction.status === 'active' ? 'bg-green-100 text-green-800' :
                  transaction.status === 'closed' ? 'bg-gray-100 text-gray-800' :
                    'bg-red-100 text-red-800'
                  }`}>
                  {transaction.status}
                </span>
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t('openedDate')}</dt>
              <dd className="mt-1 text-sm text-gray-900">
                {transaction.openedDate ? new Date(transaction.openedDate).toLocaleDateString() : '-'}
              </dd>
            </div>
            <div>
              <dt className="text-sm font-medium text-gray-500">{t('transactionId')}</dt>
              <dd className="mt-1 text-sm text-gray-900 font-mono">{transaction.transactionId}</dd>
            </div>
          </dl>
        </div>

        {/* Actions / Notes placeholder */}
        <div className="space-y-6">
          <div className="p-6 rounded-xl shadow-md" style={{ backgroundColor: '#FFFFFF' }}>
            <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('quickActions')}</h3>
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
      </div>
    </div>
  );
}