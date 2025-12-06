import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { StageUpdateModal } from '@/features/transactions/components/StageUpdateModal';
import { useTransaction } from '@/features/transactions/api/queries';
import { useUpdateTransactionStage, useSaveTransactionNotes } from '@/features/transactions/api/mutations';
import { TransactionInfo } from '@/features/transactions/components/TransactionInfo';
import { TransactionStageTracker } from '@/features/transactions/components/TransactionStageTracker';
import { TransactionTabs } from '@/features/transactions/components/TransactionTabs';

interface TransactionDetailProps {
  transactionId?: string;
}

export function TransactionDetail({ transactionId: propId }: TransactionDetailProps = {}) {
  const { id: paramId } = useParams<{ id: string }>();
  const id = propId || paramId;
  const navigate = useNavigate();
  const { data: transaction, isLoading, error } = useTransaction(id);
  const updateStage = useUpdateTransactionStage();
  const saveNotes = useSaveTransactionNotes();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [notes, setNotes] = useState<string>('');

  const { t } = useTranslation('transactions');

  // Sync notes when transaction loads
  // Sync notes when transaction loads
  useEffect(() => {
    if (transaction?.notes) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setNotes(transaction.notes);
    }
  }, [transaction?.notes]);

  const handleStageUpdate = async (newStage: number) => {
    if (!transaction) return;
    try {
      await updateStage.mutateAsync({ id: transaction.transactionId, stage: newStage });
      setIsModalOpen(false);
    } catch (error) {
      console.error('Failed to update stage:', error);
    }
  };

  const handleSaveNotes = async () => {
    if (!transaction) return;
    try {
      await saveNotes.mutateAsync({ id: transaction.transactionId, notes });
    } catch (error) {
      console.error('Failed to save notes:', error);
    }
  };

  if (error && !isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title={t('transactionDetails')}
          actions={
            <button
              onClick={() => navigate('/transactions')}
              className="flex items-center gap-2 px-4 py-2 text-sm font-medium text-gray-600 bg-white border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              <ChevronLeft className="w-4 h-4" />
              {t('backToList')}
            </button>
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title={t('detailsTitle')}
        subtitle={`ID: ${transaction.transactionId}`}
        actions={
          <button
            onClick={() => navigate('/transactions')}
            className="flex items-center gap-2 px-4 py-2 rounded-lg hover:bg-muted transition-colors text-foreground"
          >
            <ChevronLeft className="w-4 h-4" />
            {t('backToList')}
          </button>
        }
      />

      {/* Main Info Card */}
      <TransactionInfo transaction={transaction} />

      {/* Stage Tracker */}
      <TransactionStageTracker
        transaction={transaction}
        onUpdateStage={() => setIsModalOpen(true)}
      />

      {/* Tabs */}
      <TransactionTabs
        transaction={transaction}
        notes={notes}
        onNotesChange={setNotes}
        onSaveNotes={handleSaveNotes}
        isSavingNotes={saveNotes.isPending}
      />

      <StageUpdateModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSubmit={(stageIndex) => handleStageUpdate(stageIndex)}
        transactionSide={transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}
      />
    </div>
  );
}