import { useEffect, useRef } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { useTranslation } from 'react-i18next';
import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
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
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/shared/components/ui/select';
import { DocumentTypeEnum } from '@/features/documents/types';
import { requestDocumentSchema, type RequestDocumentFormValues } from '@/shared/schemas';
import { getStageLabel } from '@/shared/utils/stages';
import { useTransactionStages } from '@/features/transactions/hooks/useTransactionStages';

interface EditDocumentRequestModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (data: RequestDocumentFormValues) => void;
  transactionType: 'buy' | 'sell';
  // currentStage prop is unused, so remove it
  initialValues: RequestDocumentFormValues;
}

export function EditDocumentRequestModal({
  isOpen,
  onClose,
  onSubmit,
  transactionType,
  initialValues,
}: EditDocumentRequestModalProps) {
  const { t, i18n } = useTranslation('documents');
  const { t: tTx } = useTranslation('transactions');
  const customTitleInputRef = useRef<HTMLInputElement>(null);
  const side = transactionType === 'buy' ? 'BUY_SIDE' : 'SELL_SIDE';
  const { stages } = useTransactionStages(side);
  const stageOptions = stages.map(stage => ({
    value: stage,
    label: getStageLabel(stage, tTx, side)
  }));

  const form = useForm<RequestDocumentFormValues>({
    resolver: zodResolver(requestDocumentSchema),
    defaultValues: initialValues,
    values: initialValues,
  });

  const { reset, control } = form;
  const selectedDocType = useWatch({ control, name: 'docType' });
  // Remove unused selectedStage

  useEffect(() => {
    if (isOpen) {
      reset(initialValues);
    }
  }, [isOpen, initialValues, reset]);

  useEffect(() => {
    if (selectedDocType === DocumentTypeEnum.OTHER) {
      setTimeout(() => {
        customTitleInputRef.current?.focus();
      }, 100);
    }
  }, [selectedDocType]);

  const onFormSubmit = (data: RequestDocumentFormValues) => {
    onSubmit(data);
    onClose();
  };

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

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <DialogTitle>{t('editTitle', 'Edit Document Request')}</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onFormSubmit)} className="space-y-6 py-4">
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
            <FormField
              control={form.control}
              name="instructions"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('instructions')}</FormLabel>
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
            <FormField
              control={form.control}
              name="stage"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>{t('stage')}</FormLabel>
                  <Select
                    onValueChange={field.onChange}
                    value={field.value}
                  >
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder={t('selectStage')} />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {stageOptions.map((option) => (
                        <SelectItem key={option.value} value={option.value}>
                          {option.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage>{t(form.formState.errors.stage?.message || '')}</FormMessage>
                </FormItem>
              )}
            />
            <DialogFooter className="gap-4 sm:gap-4">
              <Button type="button" variant="outline" onClick={onClose}>
                {t('cancel')}
              </Button>
              <Button type="submit" disabled={!form.formState.isValid} className="gap-2">
                {t('save', 'Save')}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
