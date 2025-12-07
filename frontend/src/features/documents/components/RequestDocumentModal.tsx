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

interface RequestDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (documentTitle: string, instructions: string, stage: string) => void;
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
  const [documentTitle, setDocumentTitle] = useState('');
  const [instructions, setInstructions] = useState('');
  const [selectedStage, setSelectedStage] = useState('');
  const [errors, setErrors] = useState<{ title?: string; stage?: string }>({});

  const firstInputRef = useRef<HTMLInputElement>(null);

  const stages = t('stages', { returnObjects: true }) as Record<string, Record<string, string>>;
  const stageOptions = stages[transactionType] ? Object.entries(stages[transactionType]) : [];

  useEffect(() => {
    if (isOpen) {
      setDocumentTitle('');
      setInstructions('');
      setSelectedStage(currentStage);
      setErrors({});

      setTimeout(() => {
        firstInputRef.current?.focus();
      }, 100);
    }
  }, [isOpen, currentStage]);

  const validateForm = (): boolean => {
    const newErrors: { title?: string; stage?: string } = {};

    if (!documentTitle.trim()) {
      newErrors.title = t('documentTitleRequired');
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
      onSubmit(documentTitle.trim(), instructions.trim(), selectedStage);
      onClose();
    }
  };

  const isFormValid = documentTitle.trim().length > 0 && selectedStage.length > 0;

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
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <Label htmlFor="document-title" className="text-gray-700">
                {t('documentTitle')}
              </Label>
              <span className="text-red-500 text-sm">{t('required')}</span>
            </div>
            <Input
              ref={firstInputRef}
              type="text"
              id="document-title"
              value={documentTitle}
              onChange={(e) => {
                setDocumentTitle(e.target.value);
                if (errors.title) {
                  setErrors({ ...errors, title: undefined });
                }
              }}
              placeholder={t('documentTitlePlaceholder')}
              className={errors.title ? 'border-red-500' : ''}
              aria-invalid={!!errors.title}
            />
            {errors.title && (
              <p className="text-red-500 text-sm">{errors.title}</p>
            )}
          </div>

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