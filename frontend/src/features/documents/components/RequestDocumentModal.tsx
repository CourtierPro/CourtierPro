import React, { useState, useEffect, useRef } from 'react';
import { Send, FileText } from 'lucide-react';
import { useTranslation } from 'react-i18next';
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
import { Label } from '@/shared/components/ui/label';
import { DocumentTypeEnum } from '@/features/documents/types';

interface RequestDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (docType: DocumentTypeEnum, customTitle: string, instructions: string, stage: string) => void;
  transactionType: 'buy' | 'sell';
  currentStage: string;
}

export function RequestDocumentModal({
  isOpen,
  onClose,
  onSubmit,
  transactionType,
  currentStage,
}: RequestDocumentModalProps) {
  const { t, i18n } = useTranslation('documents');
  const [selectedDocType, setSelectedDocType] = useState<DocumentTypeEnum | ''>('');
  const [customTitle, setCustomTitle] = useState('');
  const [instructions, setInstructions] = useState('');
  const [selectedStage, setSelectedStage] = useState('');
  const [errors, setErrors] = useState<{ docType?: string; customTitle?: string; stage?: string }>({});

  const customTitleInputRef = useRef<HTMLInputElement>(null);

  const stages = t('stages', { returnObjects: true }) as Record<string, Record<string, string>>;
  const stageOptions = stages[transactionType] ? Object.entries(stages[transactionType]) : [];

  const docTypeOptions = Object.values(DocumentTypeEnum);

  useEffect(() => {
    if (isOpen) {
      setSelectedDocType('');
      setCustomTitle('');
      setInstructions('');
      setSelectedStage(currentStage);
      setErrors({});
    }
  }, [isOpen, currentStage]);

  useEffect(() => {
    if (selectedDocType === DocumentTypeEnum.OTHER) {
      setTimeout(() => {
        customTitleInputRef.current?.focus();
      }, 100);
    }
  }, [selectedDocType]);

  const validateForm = (): boolean => {
    const newErrors: { docType?: string; customTitle?: string; stage?: string } = {};

    if (!selectedDocType) {
      newErrors.docType = t('documentTypeRequired');
    }

    if (selectedDocType === DocumentTypeEnum.OTHER && !customTitle.trim()) {
      newErrors.customTitle = t('documentTitleRequired');
    }

    if (!selectedStage) {
      newErrors.stage = t('stageRequired');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (validateForm()) {
      onSubmit(
        selectedDocType as DocumentTypeEnum,
        selectedDocType === DocumentTypeEnum.OTHER ? customTitle.trim() : '',
        instructions.trim(),
        selectedStage
      );
      onClose();
    }
  };

  const isFormValid =
    selectedDocType &&
    (selectedDocType !== DocumentTypeEnum.OTHER || customTitle.trim().length > 0) &&
    selectedStage.length > 0;

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="sm:max-w-lg">
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-lg bg-orange-50">
              <FileText className="w-6 h-6 text-orange-500" />
            </div>
            <DialogTitle className="text-gray-800">{t('title')}</DialogTitle>
          </div>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-6 py-4">

          {/* Document Type Select */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="document-type" className="text-gray-700">
                {t('documentType')}
              </Label>
              <span className="text-red-500 text-sm">{t('required')}</span>
            </div>
            <Select
              value={selectedDocType}
              onValueChange={(value) => {
                setSelectedDocType(value as DocumentTypeEnum);
                if (errors.docType) {
                  setErrors({ ...errors, docType: undefined });
                }
              }}
            >
              <SelectTrigger
                id="document-type"
                className={errors.docType ? 'border-red-500' : ''}
              >
                <SelectValue placeholder={t('selectDocumentType')} />
              </SelectTrigger>
              <SelectContent>
                {docTypeOptions.map((type) => (
                  <SelectItem key={type} value={type}>
                    {t(`types.${type}`)}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.docType && (
              <p className="text-red-500 text-sm">{errors.docType}</p>
            )}
          </div>

          {/* Custom Title Input (Conditional) */}
          {selectedDocType === DocumentTypeEnum.OTHER && (
            <div className="space-y-2">
              <div className="flex items-center justify-between">
                <Label htmlFor="custom-title" className="text-gray-700">
                  {t('documentTitle')}
                </Label>
                <span className="text-red-500 text-sm">{t('required')}</span>
              </div>
              <Input
                ref={customTitleInputRef}
                type="text"
                id="custom-title"
                value={customTitle}
                onChange={(e) => {
                  setCustomTitle(e.target.value);
                  if (errors.customTitle) {
                    setErrors({ ...errors, customTitle: undefined });
                  }
                }}
                placeholder={t('documentTitlePlaceholder')}
                className={errors.customTitle ? 'border-red-500' : ''}
                aria-invalid={!!errors.customTitle}
              />
              {errors.customTitle && (
                <p className="text-red-500 text-sm">{errors.customTitle}</p>
              )}
            </div>
          )}

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="instructions" className="text-gray-700">
                {t('instructions')}
              </Label>
              <span className="text-gray-500 text-sm">{t('optional')}</span>
            </div>
            <Textarea
              id="instructions"
              value={instructions}
              onChange={(e) => setInstructions(e.target.value)}
              placeholder={t('instructionsPlaceholder')}
              rows={4}
            />
            <p className="text-gray-500 text-sm text-right">
              {instructions.length} {i18n.language === 'en' ? 'characters' : 'caractères'}
            </p>
          </div>

          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="associated-stage" className="text-gray-700">
                {t('associatedStage')}
              </Label>
              <span className="text-red-500 text-sm">{t('required')}</span>
            </div>
            <Select
              value={selectedStage}
              onValueChange={(value) => {
                setSelectedStage(value);
                if (errors.stage) {
                  setErrors({ ...errors, stage: undefined });
                }
              }}
            >
              <SelectTrigger
                id="associated-stage"
                className={errors.stage ? 'border-red-500' : ''}
              >
                <SelectValue placeholder={t('selectStage')} />
              </SelectTrigger>
              <SelectContent>
                {stageOptions.map(([key, value]) => (
                  <SelectItem key={key} value={value}>
                    {value}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {errors.stage && (
              <p className="text-red-500 text-sm">{errors.stage}</p>
            )}
          </div>

          <div className="p-4 rounded-lg border-2 border-gray-200 bg-gray-50">
            <p
              className="text-gray-700 text-sm"
              dangerouslySetInnerHTML={{
                __html: t('infoText', {
                  stage: selectedStage || (i18n.language === 'en' ? 'selected stage' : 'sélectionnée'),
                }),
              }}
            />
          </div>

          <DialogFooter className="gap-2 sm:gap-0">
            <Button type="button" variant="outline" onClick={onClose}>
              {t('cancel')}
            </Button>
            <Button type="submit" disabled={!isFormValid} className="gap-2">
              <Send className="w-4 h-4" />
              {t('sendRequest')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}