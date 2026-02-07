import { useEffect, useState } from 'react';
import { getStagesForSide, getStageLabel, getStageDescription } from '@/shared/utils/stages';
import { X, MessageSquare, AlertTriangle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useQueryClient } from '@tanstack/react-query';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { fetchStageChecklist, type StageChecklistItemDTO } from '@/features/documents/api/documentsApi';
import { documentKeys } from '@/features/documents/api/queries';

import { Button } from '@/shared/components/ui/button';
import { Textarea } from '@/shared/components/ui/textarea';
import { Checkbox } from '@/shared/components/ui/checkbox';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select';
import { Dialog, DialogContent, DialogTitle } from "@/shared/components/ui/dialog";
import {
  AlertDialog,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/shared/components/ui/alert-dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/shared/components/ui/form';

import { stageUpdateSchema, type StageUpdateFormValues } from '@/shared/schemas';

interface StageUpdateModalProps {
  isOpen: boolean;
  onClose: () => void;
  transactionSide: 'buy' | 'sell';
  transactionId: string;
  currentStage?: string;
  isLoading?: boolean;
  onSubmit?: (stage: string, note: string, reason?: string) => Promise<void> | void;
}

export function StageUpdateModal(props: StageUpdateModalProps) {
  return (
    <Dialog open={props.isOpen} onOpenChange={(val) => !val && props.onClose()}>
      <DialogContent className="sm:max-w-md p-0 gap-0 [&>button]:hidden">
        <StageUpdateForm {...props} />
      </DialogContent>
    </Dialog>
  );
}

function StageUpdateForm({
  isOpen,
  onClose,
  transactionSide,
  transactionId,
  currentStage,
  isLoading = false,
  onSubmit,
}: StageUpdateModalProps) {
  const { t, i18n } = useTranslation('transactions');
  const { t: tDocuments } = useTranslation('documents');
  const queryClient = useQueryClient();
  const [remainingChecklistItems, setRemainingChecklistItems] = useState<StageChecklistItemDTO[]>([]);
  const [pendingSubmission, setPendingSubmission] = useState<StageUpdateFormValues | null>(null);
  const [isCheckingChecklist, setIsCheckingChecklist] = useState(false);

  const stageEnums = transactionSide === 'buy' ? getStagesForSide('BUY_SIDE') : getStagesForSide('SELL_SIDE');
  // Compute the default next stage (current stage + 1)
  const computeNextStage = (): string => {
    if (!currentStage) return '';
    const currentIndex = stageEnums.indexOf(currentStage);
    if (currentIndex < 0) return '';
    // If not at the last stage, return the next one
    if (currentIndex < stageEnums.length - 1) {
      return stageEnums[currentIndex + 1];
    }
    // If at the last stage, return current (shouldn't happen often)
    return currentStage;
  };

  const defaultNextStage = computeNextStage();

  const form = useForm<StageUpdateFormValues>({
    resolver: zodResolver(stageUpdateSchema),
    defaultValues: {
      stage: defaultNextStage,
      note: '',
      visibleToClient: true,
      reason: '',
      currentStage: currentStage || '',
      stages: stageEnums,
    },
  });

  const { control, reset } = form; // watch is needed for detecting stage change
  const selectedStage = useWatch({ control, name: 'stage' });

  // Reset form when defaultNextStage changes
  useEffect(() => {
    reset({
      stage: defaultNextStage,
      note: '',
      visibleToClient: true,
      reason: '',
      currentStage: currentStage || '',
      stages: stageEnums,
    });
    setRemainingChecklistItems([]);
    setPendingSubmission(null);
  }, [reset, defaultNextStage, currentStage, stageEnums]);

  // Determine if it is a rollback
  const isRollback = () => {
    if (!currentStage || !selectedStage) return false;
    const currentIndex = stageEnums.indexOf(currentStage);
    const selectedIndex = stageEnums.indexOf(selectedStage);
    return selectedIndex < currentIndex;
  };

  const rollback = isRollback();

  // Determine if same stage
  const isSameStage = currentStage === selectedStage;

  useEffect(() => {
    setRemainingChecklistItems([]);
    setPendingSubmission(null);
  }, [selectedStage]);

  useEffect(() => {
    if (!isOpen) {
      setRemainingChecklistItems([]);
      setPendingSubmission(null);
      setIsCheckingChecklist(false);
    }
  }, [isOpen]);

  const handleSubmit = async (data: StageUpdateFormValues) => {
    try {
      if (onSubmit) {
        // Prevent update if same stage
        if (isSameStage) {
          return;
        }

        if (remainingChecklistItems.length > 0 && pendingSubmission) {
          return;
        }

        if (!pendingSubmission) {
          setIsCheckingChecklist(true);
          try {
            const stageToCheck = currentStage || data.currentStage;
            if (stageToCheck) {
              const checklist = await queryClient.fetchQuery({
                queryKey: documentKeys.checklist(transactionId, stageToCheck),
                queryFn: () => fetchStageChecklist(transactionId, stageToCheck),
              });
              const remainingItems = (checklist.items || []).filter((item) => !item.checked);
              if (remainingItems.length > 0) {
                setRemainingChecklistItems(remainingItems);
                setPendingSubmission(data);
                return;
              }
            }
          } catch (checklistError) {
            console.error('Failed to check stage checklist', checklistError);
          } finally {
            setIsCheckingChecklist(false);
          }
        }

        // If rollback, validation is now handled by the Zod schema resolver
        await onSubmit(data.stage, data.note || '', data.reason);
      }
      setRemainingChecklistItems([]);
      setPendingSubmission(null);
      onClose();
    } catch (err) {
      console.error('Failed to update stage', err);
      setIsCheckingChecklist(false);
    }
  };

  const handleConfirmProceed = async () => {
    if (!pendingSubmission || !onSubmit) return;
    try {
      await onSubmit(pendingSubmission.stage, pendingSubmission.note || '', pendingSubmission.reason);
      setRemainingChecklistItems([]);
      setPendingSubmission(null);
      onClose();
    } catch (err) {
      console.error('Failed to update stage after confirmation', err);
    }
  };

  const isFinalStage = (stage: string) => {
    return [
      'BUYER_POSSESSION',
      'SELLER_HANDOVER',
    ].includes(stage);
  };

  const getChecklistItemLabel = (item: StageChecklistItemDTO) => (
    tDocuments(`types.${item.docType}`, { defaultValue: item.label })
  );

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(handleSubmit)}>
        <div className="flex items-center justify-between p-6 border-b border-border">
          <DialogTitle className="text-lg font-semibold text-foreground">
            {t('updateTransactionStage')}
          </DialogTitle>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            onClick={onClose}
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </Button>
        </div>

        <div className="p-6 space-y-6">
          <FormField
            control={form.control}
            name="stage"
            render={({ field }) => (
              <FormItem>
                <FormLabel className="flex items-center gap-2">
                  {t('selectNewStage')} <span className="text-destructive">*</span>
                </FormLabel>
                <Select
                  value={field.value}
                  onValueChange={field.onChange}
                  disabled={isLoading}
                >
                  <FormControl>
                    <SelectTrigger
                      id="stage-select"
                      className="w-full"
                    >
                      <SelectValue placeholder={t('selectStagePlaceholder')} />
                    </SelectTrigger>
                  </FormControl>
                  <SelectContent>
                    {stageEnums.map((stageEnum, index) => {
                      const stageNumber = index + 1;
                      return (
                        <SelectItem key={stageEnum} value={stageEnum}>
                          {i18n.language === 'en' ? 'Stage' : 'Étape'} {stageNumber}: {getStageLabel(stageEnum, t)}
                        </SelectItem>
                      );
                    })}
                  </SelectContent>
                </Select>
                <FormMessage>{form.formState.errors.stage?.message && t(form.formState.errors.stage?.message)}</FormMessage>
              </FormItem>
            )}
          />

          {isSameStage && (
            <div className="rounded-md bg-blue-50 p-4 border border-blue-200 animate-in fade-in zoom-in-95 duration-200">
              <div className="flex">
                <div className="flex-shrink-0">
                  <AlertTriangle className="h-5 w-5 text-blue-400" aria-hidden="true" />
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-blue-800">
                    {t('currentStageSelectedTitle') || (i18n.language === 'en' ? 'Current Stage Selected' : 'Étape actuelle sélectionnée')}
                  </h3>
                  <div className="mt-2 text-sm text-blue-700">
                    <p>
                      {t('currentStageSelectedMessage') || (i18n.language === 'en' ? 'You have selected the stage the transaction is currently in. Please select a different stage to update.' : 'Vous avez sélectionné l\'étape actuelle de la transaction. Veuillez sélectionner une étape différente pour la mise à jour.')}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {rollback && (
            <div className="rounded-md bg-orange-50 p-4 border border-orange-200 animate-in fade-in zoom-in-95 duration-200">
              <div className="flex">
                <div className="flex-shrink-0">
                  <AlertTriangle className="h-5 w-5 text-orange-400" aria-hidden="true" />
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-orange-800">
                    {t('rollbackWarningTitle')}
                  </h3>
                  <div className="mt-2 text-sm text-orange-700">
                    <p>
                      {t('rollbackWarningMessage')}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {selectedStage && isFinalStage(selectedStage) && !rollback && !isSameStage && (
            <div className="rounded-md bg-yellow-50 p-4 border border-yellow-200">
              <div className="flex">
                <div className="flex-shrink-0">
                  <AlertTriangle className="h-5 w-5 text-yellow-400" aria-hidden="true" />
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-yellow-800">
                    {t('warningFinalStageTitle') || "Warning: Final Stage"}
                  </h3>
                  <div className="mt-2 text-sm text-yellow-700">
                    <p>
                      {t('warningFinalStageMessage') || "This will close the transaction. You will not be able to modify the stage afterwards."}
                    </p>
                  </div>
                </div>
              </div>
            </div>
          )}

          {selectedStage && (
            <div
              className="p-4 rounded-lg border-l-4 animate-fade-in bg-secondary/50 border-l-primary"
              id="stage-description-text"
              role="status"
              aria-live="polite"
            >
              <div className="flex items-start gap-2 mb-2">
                <MessageSquare className="w-5 h-5 flex-shrink-0 mt-0.5 text-primary" />
                <p className="text-foreground text-sm">
                  <span className="opacity-70">{t('stageDescriptionLabel')}:</span>
                </p>
              </div>
              <p className="text-foreground text-sm">
                {selectedStage && stageEnums.indexOf(selectedStage) >= 0
                  ? getStageDescription(selectedStage, t, transactionSide === 'buy' ? 'BUY_SIDE' : 'SELL_SIDE')
                  : ''}
              </p>
            </div>
          )}

          {rollback && (
            <FormField
              control={form.control}
              name="reason"
              rules={{ required: true }}
              render={({ field }) => (
                <FormItem>
                  <FormLabel className="flex items-center gap-2">
                    {t('rollbackReason')} <span className="text-destructive">*</span>
                  </FormLabel>
                  <FormControl>
                    <Textarea
                      {...field}
                      rows={3}
                      placeholder={t('rollbackReasonPlaceholder')}
                      disabled={isLoading}
                      className="border-orange-200 focus:border-orange-400 focus:ring-orange-200"
                    />
                  </FormControl>
                  <FormMessage>{form.formState.errors.reason?.message && t(form.formState.errors.reason?.message)}</FormMessage>
                </FormItem>
              )}
            />
          )}

          <FormField
            control={form.control}
            name="note"
            render={({ field }) => (
              <FormItem>
                <FormLabel>{t('progressNote')}</FormLabel>
                <FormControl>
                  <Textarea
                    {...field}
                    rows={4}
                    placeholder={t('progressNotePlaceholder')}
                    disabled={isLoading}
                  />
                </FormControl>
                <FormMessage>{form.formState.errors.note?.message && t(form.formState.errors.note?.message)}</FormMessage>
              </FormItem>
            )}
          />

          {!rollback && (
            <FormField
              control={form.control}
              name="visibleToClient"
              render={({ field }) => (
                <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md p-0">
                  <FormControl>
                    <Checkbox
                      checked={field.value}
                      onCheckedChange={field.onChange}
                      disabled={isLoading}
                    />
                  </FormControl>
                  <div className="space-y-1 leading-none">
                    <FormLabel>
                      {t('visibleToClient')}
                    </FormLabel>
                    <p className="text-sm text-muted-foreground">
                      {t('visibleToClientHelp')}
                    </p>
                  </div>
                </FormItem>
              )}
            />
          )}

        </div>

        <div className="flex items-center gap-3 p-6 border-t border-border">
          <Button
            type="button"
            variant="outline"
            onClick={onClose}
            className="flex-1"
            disabled={isLoading}
          >
            {t('cancel')}
          </Button>
          <Button
            type="submit"
            disabled={!form.formState.isValid || isSameStage || isLoading || isCheckingChecklist}
            className="flex-1"
          >
            {isLoading || isCheckingChecklist ? t('updating') ?? 'Updating...' : t('updateTransactionStage')}
          </Button>
        </div>
      </form>

      <AlertDialog
        open={remainingChecklistItems.length > 0}
        onOpenChange={(open) => {
          if (!open) {
            setRemainingChecklistItems([]);
            setPendingSubmission(null);
          }
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>{t('missingDocumentWarningTitle')}</AlertDialogTitle>
            <AlertDialogDescription>
              {t('missingDocumentWarningMessage')}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <ul className="text-sm text-muted-foreground list-disc pl-5 space-y-1">
            {remainingChecklistItems.map((item) => (
              <li key={item.itemKey}>{getChecklistItemLabel(item)}</li>
            ))}
          </ul>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isLoading}>{t('cancel')}</AlertDialogCancel>
            <Button
              type="button"
              onClick={handleConfirmProceed}
              disabled={isLoading}
            >
              {t('proceedAnyway')}
            </Button>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </Form>
  );
}
