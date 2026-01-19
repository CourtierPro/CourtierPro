import { useRef, useEffect, useState } from 'react';
import DOMPurify from 'dompurify';
import { Send, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';

import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/shared/components/ui/select';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/shared/components/ui/dialog';
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
  FormDescription,
} from '@/shared/components/ui/form';

import { DocumentTypeEnum } from '@/features/documents/types';
import { useTransactionStages } from '@/features/transactions/hooks/useTransactionStages';
import { getStageLabel } from '@/shared/utils/stages';
import { requestDocumentSchema, type RequestDocumentFormValues } from '@/shared/schemas';
import { ConditionSelector } from '@/features/transactions/components/ConditionSelector';
import { useParticipantPermissions } from '@/features/transactions/hooks/useParticipantPermissions';

interface RequestDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (docType: DocumentTypeEnum, customTitle: string, instructions: string, stage: string, conditionIds: string[]) => void;
  transactionType: 'buy' | 'sell';
  currentStage: string;
  transactionId: string;
}

export function RequestDocumentModal({
  isOpen,
  onClose,
  onSubmit,
  transactionType,
  currentStage,
  transactionId,
}: RequestDocumentModalProps) {
  const { t, i18n } = useTranslation('documents');
  const { t: tTx } = useTranslation('transactions');
  const { checkPermission } = useParticipantPermissions(transactionId);

  const customTitleInputRef = useRef<HTMLInputElement>(null);
  const [selectedConditionIds, setSelectedConditionIds] = useState<string[]>([]);

  // Fetch dynamic stages from backend
  const side = transactionType === 'buy' ? 'BUY_SIDE' : 'SELL_SIDE';
  const { stages, isLoading: isLoadingStages } = useTransactionStages(side);

  // Map stages to options
  const stageOptions = stages.map(stage => ({
    value: stage,
    label: getStageLabel(stage, tTx, side)
  }));

  // Define which documents are for which side
  const buySideDocs = [
    DocumentTypeEnum.MORTGAGE_PRE_APPROVAL,
    DocumentTypeEnum.MORTGAGE_APPROVAL,
    DocumentTypeEnum.PROOF_OF_FUNDS,
    DocumentTypeEnum.ID_VERIFICATION,
    DocumentTypeEnum.EMPLOYMENT_LETTER,
    DocumentTypeEnum.PAY_STUBS,
    DocumentTypeEnum.CREDIT_REPORT,
    DocumentTypeEnum.PROMISE_TO_PURCHASE,
    DocumentTypeEnum.INSPECTION_REPORT,
    DocumentTypeEnum.INSURANCE_LETTER,
    DocumentTypeEnum.BANK_STATEMENT,
    DocumentTypeEnum.OTHER,
  ];

  const sellSideDocs = [
    DocumentTypeEnum.CERTIFICATE_OF_LOCATION,
    DocumentTypeEnum.ID_VERIFICATION,
    DocumentTypeEnum.PROMISE_TO_PURCHASE,
    DocumentTypeEnum.INSPECTION_REPORT,
    DocumentTypeEnum.OTHER,
  ];

  const availableDocs = transactionType === 'buy' ? buySideDocs : sellSideDocs;
  const docTypeOptions = availableDocs;

  // Initialize form
  const form = useForm<RequestDocumentFormValues>({
    resolver: zodResolver(requestDocumentSchema),
    defaultValues: {
      docType: undefined,
      customTitle: '',
      instructions: '',
      stage: currentStage,
    },
  });

  const { reset, control } = form; // Removed unused imports
  const selectedDocType = useWatch({ control, name: 'docType' });
  const selectedStage = useWatch({ control, name: 'stage' });

  // Reset form state when modal opens
  useEffect(() => {
    if (isOpen) {
      reset({
        docType: undefined,
        customTitle: '',
        instructions: '',
        stage: currentStage,
      });
    }
  }, [isOpen, currentStage, reset]);

  const handleOpenChange = (open: boolean) => {
    if (!open) {
      setSelectedConditionIds([]);
      onClose();
    }
  };

  // Focus custom title input when "Other" is selected
  useEffect(() => {
    if (selectedDocType === DocumentTypeEnum.OTHER) {
      setTimeout(() => {
        customTitleInputRef.current?.focus();
      }, 100);
    }
  }, [selectedDocType]);

  const onFormSubmit = (data: RequestDocumentFormValues) => {
    onSubmit(
      data.docType,
      data.docType === DocumentTypeEnum.OTHER ? (data.customTitle?.trim() || '') : '',
      data.instructions?.trim() || '',
      data.stage,
      selectedConditionIds
    );
    setSelectedConditionIds([]);
    onClose();
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-orange-50 dark:bg-orange-900/20">
              <FileText className="w-6 h-6 text-orange-600 dark:text-orange-400" />
            </div>
            <DialogTitle className="text-foreground">{t('title')}</DialogTitle>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onFormSubmit)} className="space-y-6 py-4">

            {/* Document Type Select */}
            <FormField
              control={form.control}
              name="docType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('documentType')}</FormLabel>
                  <Select
                    onValueChange={(value) => field.onChange(value as DocumentTypeEnum)}
                    defaultValue={field.value}
                    value={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={t('selectDocumentType')} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {docTypeOptions.map((type) => (
                        <SelectItem key={type} value={type}>
                          {t(`types.${type}`)}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage>{t(form.formState.errors.docType?.message || '')}</FormMessage>
                </FormItem>
              )}
            />

            {/* Custom Title Input (Conditional) */}
            {selectedDocType === DocumentTypeEnum.OTHER && (
              <FormField
                control={form.control}
                name="customTitle"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>{t('documentTitle')}</FormLabel>
                    <FormControl>
                      <Input
                        {...field}
                        ref={(e) => {
                          field.ref(e);
                          if (e) customTitleInputRef.current = e;
                        }}
                        placeholder={t('documentTitlePlaceholder')}
                      />
                    </FormControl>
                    <FormMessage>{t(form.formState.errors.customTitle?.message || '')}</FormMessage>
                  </FormItem>
                )}
              />
            )}

            {/* Instructions */}
            <FormField
              control={form.control}
              name="instructions"
              render={({ field }) => (
                <FormItem>
                  <div className="flex items-center justify-between">
                    <FormLabel>{t('instructions')}</FormLabel>
                    <span className="text-muted-foreground text-sm">{t('optional')}</span>
                  </div>
                  <FormControl>
                    <Textarea
                      {...field}
                      placeholder={t('instructionsPlaceholder')}
                      rows={4}
                    />
                  </FormControl>
                  <FormDescription className="text-right">
                    {field.value?.length || 0} {i18n.language === 'en' ? 'characters' : 'caractères'}
                  </FormDescription>
                  <FormMessage>{t(form.formState.errors.instructions?.message || '')}</FormMessage>
                </FormItem>
              )}
            />

            {/* Stage */}
            <FormField
              control={form.control}
              name="stage"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('associatedStage')}</FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    defaultValue={field.value}
                    value={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={t('selectStage')} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {isLoadingStages ? (
                        <div className="p-2 text-sm text-muted-foreground text-center">
                          {t('loading')}...
                        </div>
                      ) : (
                        stageOptions.map((option) => (
                          <SelectItem key={option.value} value={option.value}>
                            {option.label}
                          </SelectItem>
                        ))
                      )}
                    </SelectContent>
                  </Select>
                  <FormMessage>{t(form.formState.errors.stage?.message || '')}</FormMessage>
                </FormItem>
              )}
            />

            {/* Condition Selection */}
            <ConditionSelector
              transactionId={transactionId}
              selectedConditionIds={selectedConditionIds}
              onChange={setSelectedConditionIds}
              showCreateButton={checkPermission('EDIT_CONDITIONS')}
            />

            <div className="p-4 rounded-lg border-2 border-border bg-muted/50">
              <p
                className="text-muted-foreground text-sm"
                dangerouslySetInnerHTML={{
                  __html: DOMPurify.sanitize(t('infoText', {
                    stage: selectedStage ? getStageLabel(selectedStage, tTx) : (i18n.language === 'en' ? 'selected stage' : 'sélectionnée'),
                  })),
                }}
              />
            </div>

            <DialogFooter className="gap-4 sm:gap-4">
              <Button type="button" variant="outline" onClick={onClose}>
                {t('cancel')}
              </Button>
              <Button type="submit" disabled={!form.formState.isValid} className="gap-2">
                <Send className="w-4 h-4" />
                {t('sendRequest')}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
