import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Copy } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
} from "@/shared/components/ui/alert-dialog";
import { Textarea } from "@/shared/components/ui/textarea";
import { Label } from "@/shared/components/ui/label";
import { StageUpdateModal } from '@/features/transactions/components/StageUpdateModal';
import { useTransaction, type Transaction } from '@/features/transactions/api/queries';
import { useSaveTransactionNotes, useTerminateTransaction, useUpdateTransactionStage } from '@/features/transactions/api/mutations';
import { TransactionInfo } from '@/features/transactions/components/TransactionInfo';
import { StageDescription } from '@/features/transactions/components/StageDescription';
import { TransactionStageTracker } from '@/features/transactions/components/TransactionStageTracker';
import { TransactionTabs } from '@/features/transactions/components/TransactionTabs';
import { ClientDetailModal } from '@/features/clients/components/ClientDetailModal';
import type { Client } from '@/features/clients/api/clientsApi';

interface TransactionDetailProps {
  transactionId?: string;
  isReadOnly?: boolean;
  TimelineComponent?: React.ComponentType<{ transactionId: string }>;
}

function TransactionDetailContent({
  transaction,
  isReadOnly = false,
  TimelineComponent,
  onTransactionUpdate
}: {
  transaction: NonNullable<Transaction>;
  isReadOnly?: boolean;
  TimelineComponent?: React.ComponentType<{ transactionId: string }>;
  onTransactionUpdate?: () => void;
}) {
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');
  const saveNotes = useSaveTransactionNotes();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isTerminateModalOpen, setIsTerminateModalOpen] = useState(false);
  const [isClientModalOpen, setIsClientModalOpen] = useState(false);
  const [notes, setNotes] = useState<string>(transaction.notes || '');
  const [terminateReason, setTerminateReason] = useState('');
  const [terminateError, setTerminateError] = useState<string | null>(null);
  const updateStage = useUpdateTransactionStage();
  const terminateTransaction = useTerminateTransaction();

  // Construct a minimal client object from transaction data for the modal
  const clientFromTransaction: Client | null = transaction.clientId ? {
    id: transaction.clientId,
    email: '', // Will be fetched by the modal's useBrokerClientTransactions
    firstName: transaction.clientName?.split(' ')[0] || '',
    lastName: transaction.clientName?.split(' ').slice(1).join(' ') || '',
    role: 'CLIENT',
    active: true,
    preferredLanguage: 'en'
  } : null;

  // stage updates are handled by the StageUpdateModal which calls this onSubmit
  const handleStageUpdate = async (stage: string, note: string, reason?: string) => {
    try {
      await updateStage.mutateAsync({ transactionId: transaction.transactionId, data: { stage, note, reason } });
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

  const handleClientClick = () => {
    if (!isReadOnly && clientFromTransaction) {
      setIsClientModalOpen(true);
    }
  };

  const handleTerminateOpenChange = (open: boolean) => {
    setIsTerminateModalOpen(open);
    if (!open) {
      setTerminateReason('');
      setTerminateError(null);
    }
  };

  const handleTerminateConfirm = async () => {
    const trimmed = terminateReason.trim();
    const validationMessage = !trimmed
      ? t('reasonRequired')
      : trimmed.length < 10
        ? t('reasonTooShort')
        : trimmed.length > 500
          ? t('reasonTooLong')
          : '';

    if (validationMessage) {
      setTerminateError(validationMessage);
      return;
    }

    try {
      await terminateTransaction.mutateAsync({ transactionId: transaction.transactionId, reason: trimmed });
      toast.success(t('terminateSuccess'));
      handleTerminateOpenChange(false);
    } catch {
      toast.error(t('errorTerminating'));
    }
  };

  const trimmedReason = terminateReason.trim();
  const reasonValidationMessage = !trimmedReason
    ? t('reasonRequired')
    : trimmedReason.length < 10
      ? t('reasonTooShort')
      : trimmedReason.length > 500
        ? t('reasonTooLong')
        : '';
  const showReasonError = Boolean(terminateError) || (terminateReason.length > 0 && Boolean(reasonValidationMessage));
  const reasonErrorMessage = terminateError || (terminateReason.length > 0 ? reasonValidationMessage : '');

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

      <div className="flex flex-col md:flex-row gap-4 md:gap-6">
        <div className="w-full md:w-[30%]">
          <TransactionInfo
            transaction={transaction}
            hideClientLabel={isReadOnly}
            onClientClick={!isReadOnly ? handleClientClick : undefined}
          />
        </div>
        <div className="w-full md:w-[70%]">
          <StageDescription transaction={transaction} />
        </div>
      </div>

      <TransactionStageTracker
        transaction={transaction}
        onUpdateStage={() => setIsModalOpen(true)}
        onTerminate={() => setIsTerminateModalOpen(true)}
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
        onTransactionUpdate={onTransactionUpdate}
      />

      <StageUpdateModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        isLoading={updateStage.isPending}
        onSubmit={handleStageUpdate}
        transactionSide={transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}
        transactionId={transaction.transactionId}
        currentStage={typeof transaction.currentStage === 'string' ? transaction.currentStage : undefined}
      />

      <AlertDialog open={isTerminateModalOpen} onOpenChange={handleTerminateOpenChange}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('terminateConfirmTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('terminateConfirmMessage')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <div className="space-y-2">
            <Label htmlFor="terminate-reason">{t('terminateReason')}</Label>
            <Textarea
              id="terminate-reason"
              rows={4}
              placeholder={t('terminateReasonPlaceholder')}
              value={terminateReason}
              onChange={(event) => {
                setTerminateReason(event.target.value);
                if (terminateError) {
                  setTerminateError(null);
                }
              }}
              disabled={terminateTransaction.isPending}
            />
            {showReasonError && (
              <p className="text-sm text-destructive">{reasonErrorMessage}</p>
            )}
          </div>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={terminateTransaction.isPending}>
              {t('cancel')}
            </AlertDialogCancel>
            <Button
              variant="destructive"
              onClick={handleTerminateConfirm}
              disabled={terminateTransaction.isPending}
            >
              {terminateTransaction.isPending ? t('updating') ?? 'Updating...' : t('terminateTransaction')}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>

      {!isReadOnly && clientFromTransaction && (
        <ClientDetailModal
          client={clientFromTransaction}
          isOpen={isClientModalOpen}
          onClose={() => setIsClientModalOpen(false)}
        />
      )}
    </div>
  );
}

export function TransactionDetail({ transactionId: propId, isReadOnly = false, TimelineComponent }: TransactionDetailProps = {}) {
  const params = useParams();
  const id = propId || params.id || params.transactionId;
  const navigate = useNavigate();
  const { t } = useTranslation('transactions');
  const { data: transaction, isLoading, error, refetch } = useTransaction(id);

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

  return (
    <TransactionDetailContent
      transaction={transaction}
      isReadOnly={isReadOnly}
      key={transaction.transactionId}
      TimelineComponent={TimelineComponent}
      onTransactionUpdate={() => refetch()}
    />
  );
}
