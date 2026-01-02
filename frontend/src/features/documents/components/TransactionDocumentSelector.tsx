import React from 'react';
import { Button } from '@/shared/components/ui/button';
import { useTranslation } from 'react-i18next';
import { getStageLabel } from '@/shared/utils/stages';
import { FileText, MapPin, CheckCircle } from 'lucide-react';

export interface TransactionDocumentSelectorProps {
  transactions: Array<{
    transactionId: string;
    clientName: string;
    side: 'BUY_SIDE' | 'SELL_SIDE';
    propertyAddress?: string;
    currentStage?: string;
    documentCount?: number;
    documentStatuses?: {
      pending: number;
      approved: number;
      needsRevision: number;
    };
  }>;
  onSelect: (transactionId: string, side: 'BUY_SIDE' | 'SELL_SIDE') => void;
  selectedTransactionId?: string;
}

export const TransactionDocumentSelector: React.FC<TransactionDocumentSelectorProps> = ({
  transactions,
  onSelect,
  selectedTransactionId,
}) => {
  const { t: tDocuments } = useTranslation('documents');
  const { t: tTransactions } = useTranslation('transactions');
  return (
    <div className="space-y-6 mb-8">
      <div>
        <h2 className="text-2xl font-bold text-foreground">
          {tDocuments('title', 'My Documents')}
        </h2>
        <p className="text-muted-foreground mt-2">{tDocuments('selectTransaction', 'Choose a transaction to view and manage its documents')}</p>
      </div>
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {transactions.map(tx => (
          <div
            key={tx.transactionId}
            className={`border-2 rounded-xl p-5 flex flex-col gap-3 shadow-sm transition-all hover:shadow-md cursor-pointer ${
              selectedTransactionId === tx.transactionId
                ? 'border-orange-500 bg-orange-50 dark:bg-orange-950/20'
                : 'border-border bg-card hover:border-orange-300'
            }`}
            onClick={() => onSelect(tx.transactionId, tx.side)}
          >
            <div className="flex items-center justify-between">
              <div className="flex-1">
                <h3 className="font-semibold text-foreground">{tx.clientName}</h3>
              </div>
              <div className="flex items-center gap-2">
                {selectedTransactionId === tx.transactionId && (
                  <CheckCircle className="w-5 h-5 text-orange-500" />
                )}
                <span className={`px-3 py-1 rounded-full text-xs font-bold ${
                  tx.side === 'BUY_SIDE'
                    ? 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300'
                    : 'bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300'
                }`}>
                  {tx.side === 'BUY_SIDE' ? tDocuments('buyerLabel', 'Buyer') : tDocuments('sellerLabel', 'Seller')}
                </span>
              </div>
            </div>
            {tx.propertyAddress && (
              <div className="flex items-start gap-2 text-sm text-muted-foreground">
                <MapPin className="w-4 h-4 mt-0.5 flex-shrink-0" />
                <span>{tx.propertyAddress}</span>
              </div>
            )}
            {tx.currentStage && (
              <div className="text-sm text-muted-foreground border-t border-border/50 pt-3">
                <span className="font-medium">{tDocuments('currentStage', 'Current Stage')}:</span>
                <span className="ml-2">{getStageLabel(tx.currentStage, tTransactions, tx.side)}</span>
              </div>
            )}
            {/* Document status badges */}
            <div className="flex flex-wrap gap-2 pt-2">
              {tx.documentStatuses && (
                <>
                  {tx.documentStatuses.pending > 0 && (
                    <div className="text-xs px-2.5 py-1 rounded-full bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-300 font-medium">
                      {tx.documentStatuses.pending} {tDocuments('status.REQUESTED')}
                    </div>
                  )}
                  {tx.documentStatuses.submitted > 0 && (
                    <div className="text-xs px-2.5 py-1 rounded-full bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-300 font-medium">
                      {tx.documentStatuses.submitted} {tDocuments('status.SUBMITTED')}
                    </div>
                  )}
                  {tx.documentStatuses.needsRevision > 0 && (
                    <div className="text-xs px-2.5 py-1 rounded-full bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-300 font-medium">
                      {tx.documentStatuses.needsRevision} {tDocuments('status.NEEDS_REVISION')}
                    </div>
                  )}
                  {tx.documentStatuses.approved > 0 && (
                    <div className="text-xs px-2.5 py-1 rounded-full bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300 font-medium">
                      {tx.documentStatuses.approved} {tDocuments('status.APPROVED')}
                    </div>
                  )}
                </>
              )}
            </div>
            <Button
              variant={selectedTransactionId === tx.transactionId ? 'default' : 'outline'}
              onClick={() => onSelect(tx.transactionId, tx.side)}
              className="mt-2"
            >
              {tDocuments('showDocuments', 'Afficher les documents')}
            </Button>
          </div>
        ))}
      </div>
    </div>
  );
};
