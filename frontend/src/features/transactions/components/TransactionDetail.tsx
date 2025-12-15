import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Copy } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { StageUpdateModal } from '@/features/transactions/components/StageUpdateModal';
import { useTransaction, type Transaction } from '@/features/transactions/api/queries';
import { useSaveTransactionNotes } from '@/features/transactions/api/mutations';
import { TransactionInfo } from '@/features/transactions/components/TransactionInfo';
import { TransactionStageTracker } from '@/features/transactions/components/TransactionStageTracker';
import { TransactionTabs } from '@/features/transactions/components/TransactionTabs';
import { useUpdateTransactionStage } from '@/features/transactions/api/mutations';

interface TransactionDetailProps {
  transactionId?: string;
  isReadOnly?: boolean;
  TimelineComponent?: React.ComponentType<{ transactionId: string }>;
}

function TransactionDetailContent({ transaction, isReadOnly = false, TimelineComponent }: { transaction: NonNullable<Transaction>; isReadOnly?: boolean; TimelineComponent?: React.ComponentType<{ transactionId: string }> }) {
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');
  const saveNotes = useSaveTransactionNotes();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [notes, setNotes] = useState<string>(transaction.notes || '');
  const updateStage = useUpdateTransactionStage();

  // stage updates are handled by the StageUpdateModal which calls this onSubmit
  const handleStageUpdate = async (stage: string, note: string) => {
    try {
      await updateStage.mutateAsync({ transactionId: transaction.transactionId, data: { stage, note } });
      toast.success(t('stageUpdatedSuccess'));
      // modal will be closed by the modal component after onSubmit resolves
    } catch (err) {
      toast.error(t('errorUpdatingStage'));
      throw err; // rethrow so modal doesn't close if it awaits
    }
  };
  const handleSaveNotes = async () => {
    try {
      await saveNotes.mutateAsync({ id: transaction.transactionId, notes });
      toast.success(t('notesSaved'));
    } catch {
      toast.error(t('errorSavingNotes'));
    }
  };

  const handleCopyId = () => {
    navigator.clipboard.writeText(transaction.transactionId);
    toast.success(t('idCopied'));
  };

  return (
    <div className="space-y-4 md:space-y-6">
      <PageHeader
        title={t('detailsTitle')}
        subtitle={
          <div className="flex items-center gap-2">
            <span>ID: {transaction.transactionId}</span>
            <Button
              variant="ghost"
              size="icon"
              className="text-muted-foreground hover:text-foreground"
              onClick={handleCopyId}
              aria-label={t('copyTransactionId', { defaultValue: 'Copy transaction ID' })}
            >
              <Copy className="h-3 w-3" />
            </Button>
          </div>
        }
        actions={
          <Button
            variant="ghost"
            onClick={() => navigate(isReadOnly ? '/my-transaction' : '/transactions')}
            className="gap-2"
          >
            <ChevronLeft className="w-4 h-4" />
            {t('backToList')}
          </Button>
        }
      />

      <TransactionInfo transaction={transaction} hideClientLabel={isReadOnly} />

      <TransactionStageTracker
        transaction={transaction}
        onUpdateStage={() => setIsModalOpen(true)}
        isReadOnly={isReadOnly}
      />

      <TransactionTabs
        transaction={transaction}
        notes={notes}
        onNotesChange={setNotes}
        onSaveNotes={handleSaveNotes}
        isSavingNotes={saveNotes.isPending}
        isReadOnly={isReadOnly}
        TimelineComponent={TimelineComponent}
      />

      <StageUpdateModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        isLoading={updateStage.isPending}
        onSubmit={handleStageUpdate}
        transactionSide={transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}
        transactionId={transaction.transactionId}
      />
    </div>
  );
}

export function TransactionDetail({ transactionId: propId, isReadOnly = false, TimelineComponent }: TransactionDetailProps = {}) {
  const params = useParams();
  const id = propId || params.id || params.transactionId;
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');
  const { data: transaction, isLoading, error } = useTransaction(id);

  if (error && !isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title={t('transactionDetails')}
          actions={
            <Button
              variant="outline"
              onClick={() => navigate(isReadOnly ? '/my-transaction' : '/transactions')}
              className="gap-2"
            >
              <ChevronLeft className="w-4 h-4" />
              {t('backToList')}
            </Button>
          }
        />
        <ErrorState message={error.message || "Failed to load transaction"} onRetry={() => window.location.reload()} />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader title={t('detailsTitle')} />
        <LoadingState message={t('loadingDetails')} />
      </div>
    );
  }

  if (!transaction) return null;

  return <TransactionDetailContent transaction={transaction} isReadOnly={isReadOnly} key={transaction.transactionId} TimelineComponent={TimelineComponent} />;
}