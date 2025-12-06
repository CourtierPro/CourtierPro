import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { StageUpdateModal } from '@/features/transactions/components/StageUpdateModal';
import { useTransaction, type Transaction } from '@/features/transactions/api/queries';
import { useUpdateTransactionStage, useSaveTransactionNotes } from '@/features/transactions/api/mutations';
import { TransactionInfo } from '@/features/transactions/components/TransactionInfo';
import { TransactionStageTracker } from '@/features/transactions/components/TransactionStageTracker';
import { TransactionTabs } from '@/features/transactions/components/TransactionTabs';

interface TransactionDetailProps {
  transactionId?: string;
}

function TransactionDetailContent({ transaction }: { transaction: NonNullable<Transaction> }) {
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');
  const updateStage = useUpdateTransactionStage();
  const saveNotes = useSaveTransactionNotes();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [notes, setNotes] = useState<string>(transaction.notes || '');

  const handleStageUpdate = async (newStage: number) => {
    try {
      await updateStage.mutateAsync({ id: transaction.transactionId, stage: newStage });
      setIsModalOpen(false);
      toast.success(t('stageUpdated'));
    } catch {
      toast.error(t('errorUpdatingStage'));
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

  return (
    <div className="space-y-6">
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

      <TransactionInfo transaction={transaction} />

      <TransactionStageTracker
        transaction={transaction}
        onUpdateStage={() => setIsModalOpen(true)}
      />

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

export function TransactionDetail({ transactionId: propId }: TransactionDetailProps = {}) {
  const { id: paramId } = useParams<{ id: string }>();
  const id = propId || paramId;
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

  return <TransactionDetailContent transaction={transaction} key={transaction.transactionId} />;
}