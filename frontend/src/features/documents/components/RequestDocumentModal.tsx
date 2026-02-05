import { useRef, useEffect, useState, useCallback } from 'react';
import DOMPurify from 'dompurify';
import { Send, FileText, Calendar as CalendarIcon, ChevronDown, ChevronRight, Save, PenLine, Upload, X, Loader2 } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useForm, useWatch } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { format } from 'date-fns';
import { useDropzone, type FileRejection } from 'react-dropzone';

import { Button } from '@/shared/components/ui/button';
import { Input } from '@/shared/components/ui/input';
import { Textarea } from '@/shared/components/ui/textarea';
import { Calendar } from '@/shared/components/ui/calendar';
import { Progress } from '@/shared/components/ui/progress';
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
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from '@/shared/components/ui/popover';
import { Checkbox } from '@/shared/components/ui/checkbox';
import { cn } from '@/shared/utils/utils';

import { DocumentTypeEnum, DocumentFlowEnum } from '@/features/documents/types';
import { useTransactionStages } from '@/features/transactions/hooks/useTransactionStages';
import { getStageLabel } from '@/shared/utils/stages';
import { requestDocumentSchema, type RequestDocumentFormValues } from '@/shared/schemas';
import { ConditionSelector } from '@/features/transactions/components/ConditionSelector';
import { useParticipantPermissions } from '@/features/transactions/hooks/useParticipantPermissions';

interface RequestDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (...args: [docType: DocumentTypeEnum, customTitle: string, instructions: string, stage: string, conditionIds: string[], dueDate?: Date, status?: 'DRAFT' | 'REQUESTED', flow?: DocumentFlowEnum, requiresSignature?: boolean]) => Promise<string | undefined> | void;
  onUploadFile?: (documentId: string, file: File) => Promise<void>;
  onSendDocumentRequest?: (documentId: string) => Promise<void>;
  transactionType: 'buy' | 'sell';
  currentStage: string;
  transactionId: string;
}

export function RequestDocumentModal({
  isOpen,
  onClose,
  onSubmit,
  onUploadFile,
  onSendDocumentRequest,
  transactionType,
  currentStage,
  transactionId,
}: RequestDocumentModalProps) {
  const { t, i18n } = useTranslation('documents');
  const { t: tTx } = useTranslation('transactions');
  const { checkPermission } = useParticipantPermissions(transactionId);

  const customTitleInputRef = useRef<HTMLInputElement>(null);
  const [selectedConditionIds, setSelectedConditionIds] = useState<string[]>([]);
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);

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
      dueDate: undefined,
      requiresSignature: false,
    },
  });

  const { reset, control } = form; // Removed unused imports
  const selectedDocType = useWatch({ control, name: 'docType' });
  const selectedStage = useWatch({ control, name: 'stage' });
  const requiresSignature = useWatch({ control, name: 'requiresSignature' });

  // Dropzone for signature document attachment
  const onDrop = useCallback((acceptedFiles: File[]) => {
    const selectedFile = acceptedFiles[0];
    if (selectedFile) {
      if (selectedFile.size > 25 * 1024 * 1024) {
        setFileError(t('errors.fileTooLarge'));
        setFile(null);
      } else {
        setFileError(null);
        setFile(selectedFile);
      }
    }
  }, [t]);

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop,
    onDropRejected: (fileRejections: FileRejection[]) => {
      const rejection = fileRejections[0];
      if (rejection.errors[0].code === 'file-invalid-type') {
        setFileError(t('errors.invalidFileType', 'Invalid file type. Please upload PDF, JPG, or PNG.'));
      } else {
        setFileError(rejection.errors[0].message);
      }
    },
    accept: {
      'application/pdf': ['.pdf'],
      'image/jpeg': ['.jpg', '.jpeg'],
      'image/png': ['.png']
    },
    maxFiles: 1,
    multiple: false,
    disabled: isUploading
  });

  const removeFile = () => {
    setFile(null);
    setFileError(null);
  };

  // Simulate progress when uploading
  useEffect(() => {
    let interval: ReturnType<typeof setInterval>;
    if (isUploading) {
      interval = setInterval(() => {
        setUploadProgress((prev) => (prev >= 90 ? 90 : prev + 10));
      }, 500);
    }
    return () => clearInterval(interval);
  }, [isUploading]);

  // Reset form state when modal opens
  useEffect(() => {
    if (isOpen) {
      reset({
        docType: undefined,
        customTitle: '',
        instructions: '',
        stage: currentStage,
        dueDate: undefined,
        requiresSignature: false,
      });
    }
  }, [isOpen, currentStage, reset]);

  const handleOpenChange = (open: boolean) => {
    if (!open && !isUploading) {
      setSelectedConditionIds([]);
      setShowAdvanced(false);
      setFile(null);
      setFileError(null);
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

  const onFormSubmit = async (data: RequestDocumentFormValues, status: 'DRAFT' | 'REQUESTED' = 'REQUESTED') => {
    const hasFileToUpload = data.requiresSignature && file && onUploadFile;

    // If signature request with file, always create as DRAFT first, then upload, then optionally send
    const createStatus = hasFileToUpload ? 'DRAFT' : status;

    if (hasFileToUpload) {
      setIsUploading(true);
      setUploadProgress(0);
    }

    try {
      const documentId = await onSubmit(
        data.docType,
        data.docType === DocumentTypeEnum.OTHER ? (data.customTitle?.trim() || '') : '',
        data.instructions?.trim() || '',
        data.stage,
        selectedConditionIds,
        data.dueDate,
        createStatus,
        undefined,
        data.requiresSignature
      );

      // Upload file if signature request with file attached
      if (hasFileToUpload && documentId) {
        await onUploadFile(documentId, file);

        // If user clicked "Send Request", transition DRAFT → REQUESTED
        if (status === 'REQUESTED' && onSendDocumentRequest) {
          await onSendDocumentRequest(documentId);
        }

        setUploadProgress(100);
        setTimeout(() => {
          setSelectedConditionIds([]);
          setFile(null);
          setIsUploading(false);
          onClose();
        }, 500);
        return;
      }

      setSelectedConditionIds([]);
      setFile(null);
      onClose();
    } catch {
      setIsUploading(false);
      setUploadProgress(0);
    }
  };

  const handleSaveAsDraft = () => {
    const data = form.getValues();
    if (form.formState.isValid) {
      onFormSubmit(data, 'DRAFT');
    } else {
      form.trigger();
    }
  };

  const handleSendRequest = () => {
    const data = form.getValues();
    if (form.formState.isValid) {
      onFormSubmit(data, 'REQUESTED');
    } else {
      form.trigger();
    }
  };

  return (
    <Dialog open={isOpen} onOpenChange={handleOpenChange}>
      <DialogContent className="sm:max-w-2xl">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-orange-50 dark:bg-orange-900/20">
              <FileText className="w-6 h-6 text-orange-600 dark:text-orange-400" />
            </div>
            <DialogTitle className="text-foreground">{t('title')}</DialogTitle>
          </div>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={(e) => { e.preventDefault(); handleSendRequest(); }} className="space-y-6 py-4">

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

            {/* Requires Signature Checkbox */}
            <FormField
              control={form.control}
              name="requiresSignature"
              render={({ field }) => (
                <FormItem className="flex flex-row items-start space-x-3 space-y-0 rounded-md border p-4">
                  <FormControl>
                    <Checkbox
                      checked={field.value}
                      onCheckedChange={field.onChange}
                    />
                  </FormControl>
                  <div className="space-y-1 leading-none">
                    <FormLabel className="flex items-center gap-2">
                      <PenLine className="w-4 h-4" />
                      {t('requiresSignature')}
                    </FormLabel>
                    <FormDescription>
                      {t('requiresSignatureDescription')}
                    </FormDescription>
                  </div>
                </FormItem>
              )}
            />

            {/* File Dropzone (shown when requiresSignature is checked) */}
            {requiresSignature && (
              <div className="space-y-2">
                <FormLabel>{t('documentToSign')}</FormLabel>
                {!file ? (
                  <div
                    {...getRootProps()}
                    className={`border-2 border-dashed rounded-lg p-6 text-center transition-colors cursor-pointer flex flex-col items-center gap-2
                      ${isDragReject ? 'border-destructive bg-destructive/10' : isDragActive ? 'border-primary bg-primary/10' : 'border-border hover:bg-muted/50'}`}
                  >
                    <input {...getInputProps()} />
                    <div className={`p-2 rounded-full ${isDragReject ? 'bg-destructive/10' : 'bg-muted'}`}>
                      {isDragReject ? (
                        <X className="w-5 h-5 text-destructive" />
                      ) : (
                        <Upload className="w-5 h-5 text-muted-foreground" />
                      )}
                    </div>
                    <div>
                      <span className={`text-sm font-medium block ${isDragReject ? 'text-destructive' : 'text-foreground'}`}>
                        {isDragReject ? t('errors.invalidFileType', 'Invalid file type') : (isDragActive ? t('dropFileHere', 'Drop file here') : t('dragAndDropOrClick'))}
                      </span>
                      <span className="text-xs text-muted-foreground">
                        {t('supportedFormats')} (PDF, JPG, PNG) • {t('maxSize', { size: '25MB' })}
                      </span>
                    </div>
                  </div>
                ) : (
                  <div className="border border-border rounded-lg p-3 relative">
                    {!isUploading && (
                      <button
                        type="button"
                        onClick={removeFile}
                        className="absolute top-2 right-2 p-1 hover:bg-muted rounded-full text-muted-foreground"
                      >
                        <X className="w-4 h-4" />
                      </button>
                    )}
                    <div className="flex items-center gap-3">
                      <div className="p-2 bg-primary/10 rounded-lg">
                        <FileText className="w-6 h-6 text-primary" />
                      </div>
                      <div className="flex-1 overflow-hidden">
                        <p className="text-sm font-medium text-foreground truncate">{file.name}</p>
                        <p className="text-xs text-muted-foreground">{(file.size / 1024 / 1024).toFixed(2)} MB</p>
                      </div>
                    </div>
                    {isUploading && (
                      <div className="mt-3 space-y-1">
                        <div className="flex justify-between text-xs text-muted-foreground">
                          <span>{t('actions.uploading', 'Uploading...')}...</span>
                          <span>{uploadProgress}%</span>
                        </div>
                        <Progress value={uploadProgress} className="h-2" />
                      </div>
                    )}
                  </div>
                )}
                {fileError && (
                  <div className="text-sm text-destructive bg-destructive/10 p-2 rounded flex items-center gap-2">
                    <span className="block w-1.5 h-1.5 rounded-full bg-destructive" />
                    {fileError}
                  </div>
                )}
              </div>
            )}

            <div className="grid grid-cols-2 gap-4">
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

              {/* Due Date */}
              <FormField
                control={form.control}
                name="dueDate"
                render={({ field }) => (
                  <FormItem className="flex flex-col">
                    <FormLabel>{t('dueDate')}</FormLabel>
                    <Popover>
                      <PopoverTrigger asChild>
                        <FormControl>
                          <Button
                            variant={"outline"}
                            className={cn(
                              "w-full pl-3 text-left font-normal",
                              !field.value && "text-muted-foreground"
                            )}
                          >
                            {field.value ? (
                              format(field.value, "PPP")
                            ) : (
                              <span>{t('pickDate')}</span>
                            )}
                            <CalendarIcon className="ml-auto h-4 w-4 opacity-50" />
                          </Button>
                        </FormControl>
                      </PopoverTrigger>
                      <PopoverContent className="w-auto p-0" align="start">
                        <Calendar
                          mode="single"
                          selected={field.value}
                          onSelect={field.onChange}
                          disabled={(date) =>
                            date < new Date(new Date().setHours(0, 0, 0, 0))
                          }
                          initialFocus
                        />
                      </PopoverContent>
                    </Popover>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            {/* Condition Selection */}
            <div className="pt-2">
              <Button
                type="button"
                variant="ghost"
                className="p-0 h-auto font-medium text-sm flex items-center gap-1 text-muted-foreground hover:text-foreground"
                onClick={() => setShowAdvanced(!showAdvanced)}
              >
                {showAdvanced ? <ChevronDown className="h-4 w-4" /> : <ChevronRight className="h-4 w-4" />}
                {t('advancedOptions')}
              </Button>

              {showAdvanced && (
                <div className="pt-3 pl-2 border-l-2 border-muted mt-2">
                  <ConditionSelector
                    transactionId={transactionId}
                    selectedConditionIds={selectedConditionIds}
                    onChange={setSelectedConditionIds}
                    showCreateButton={checkPermission('EDIT_CONDITIONS')}
                  />
                </div>
              )}
            </div>

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
              <Button type="button" variant="outline" onClick={onClose} disabled={isUploading}>
                {t('cancel')}
              </Button>
              <Button
                type="button"
                variant="secondary"
                onClick={handleSaveAsDraft}
                disabled={!form.formState.isValid || isUploading}
                className="gap-2"
              >
                <Save className="w-4 h-4" />
                {t('actions.saveAsDraft', 'Save as Draft')}
              </Button>
              <Button
                type="submit"
                disabled={!form.formState.isValid || isUploading || (requiresSignature && !file)}
                className="gap-2"
              >
                {isUploading ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    {t('actions.uploading', 'Uploading...')}
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    {t('sendRequest')}
                  </>
                )}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
