import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
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
      toast.success(t('stageUpdated'));
    } catch (error) {
      toast.error(t('errorUpdatingStage'));
    }
  };

  const handleSaveNotes = async () => {
    if (!transaction) return;
    try {
      await saveNotes.mutateAsync({ id: transaction.transactionId, notes });
      toast.success(t('notesSaved'));
    } catch (error) {
      toast.error(t('errorSavingNotes'));
    }
  };

  if (error && !isLoading) {
    return (
      <div className="space-y-6">
        <PageHeader
          title={t('transactionDetails')}
          actions={
            <Button
              variant="outline"
              onClick={() => navigate('/transactions')}
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <PageHeader
        title={t('detailsTitle')}
        subtitle={`ID: ${transaction.transactionId}`}
        actions={
          <Button
            variant="ghost"
            onClick={() => navigate('/transactions')}
            className="gap-2"
          >
            <ChevronLeft className="w-4 h-4" />
            {t('backToList')}
          </Button>
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