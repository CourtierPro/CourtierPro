import { useEffect } from 'react';
import { getStagesForSide, getStageLabel } from '@/shared/utils/stages';
import { X, MessageSquare, AlertTriangle } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

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
  isLoading?: boolean;
  onSubmit?: (stage: string, note: string) => Promise<void> | void;
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
  onClose,
  transactionSide,
  isLoading = false,
  onSubmit,
}: StageUpdateModalProps) {
  const { t, i18n } = useTranslation('transactions');

  const stageEnums = transactionSide === 'buy' ? getStagesForSide('BUY_SIDE') : getStagesForSide('SELL_SIDE');
  const stageDescriptions =
    transactionSide === 'buy'
      ? (t('buyStageDescriptions', { returnObjects: true }) as string[])
      : (t('sellStageDescriptions', { returnObjects: true }) as string[]);

  const form = useForm<StageUpdateFormValues>({
    resolver: zodResolver(stageUpdateSchema),
    defaultValues: {
      stage: '',
      note: '',
      visibleToClient: true,
    },
  });

  const { control, reset } = form;
  const selectedStage = useWatch({ control, name: 'stage' });

  useEffect(() => {
    reset({
      stage: '',
      note: '',
      visibleToClient: true,
    });
  }, [reset]);

  const handleSubmit = async (data: StageUpdateFormValues) => {
    try {
      if (onSubmit) {
        await onSubmit(data.stage, data.note || '');
      }
      onClose();
    } catch (err) {
      console.error('Failed to update stage', err);
    }
  };

  const isFinalStage = (stage: string) => {
    return [
      'BUYER_OCCUPANCY',
      'BUYER_TERMINATED',
      'SELLER_HANDOVER_KEYS',
      'SELLER_TERMINATED'
    ].includes(stage);
  };
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

          {selectedStage && isFinalStage(selectedStage) && (
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
                  ? stageDescriptions[stageEnums.indexOf(selectedStage)]
                  : ''}
              </p>
            </div>
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
            disabled={!form.formState.isValid || isLoading}
            className="flex-1"
          >
            {isLoading ? t('updating') ?? 'Updating...' : t('updateTransactionStage')}
          </Button>
        </div>
      </form>
    </Form>
  );
}
