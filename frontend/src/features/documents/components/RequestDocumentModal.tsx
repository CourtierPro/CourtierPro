import React, { useState, useEffect, useRef } from 'react';
import { X, Send, FileText } from 'lucide-react';
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

  const modalRef = useRef<HTMLDivElement>(null);
  const firstInputRef = useRef<HTMLInputElement>(null);

  // Get stages from translation
  const stages = t('stages', { returnObjects: true }) as Record<string, Record<string, string>>;
  const stageOptions = stages[transactionType] ? Object.entries(stages[transactionType]) : [];

  // Reset form when modal opens
  useEffect(() => {
    if (isOpen) {
      setDocumentTitle('');
      setInstructions('');
      setSelectedStage(currentStage);
      setErrors({});

      // Focus first input
      setTimeout(() => {
        firstInputRef.current?.focus();
      }, 100);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen]);

  // Handle ESC key and focus trap
  useEffect(() => {
    if (!isOpen) return;

    const handleEscape = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    const handleTabKey = (e: KeyboardEvent) => {
      if (e.key !== 'Tab') return;

      const focusableElements = modalRef.current?.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      );

      if (!focusableElements || focusableElements.length === 0) return;

      const firstElement = focusableElements[0] as HTMLElement;
      const lastElement = focusableElements[focusableElements.length - 1] as HTMLElement;

      if (e.shiftKey) {
        if (document.activeElement === firstElement) {
          e.preventDefault();
          lastElement.focus();
        }
      } else {
        if (document.activeElement === lastElement) {
          e.preventDefault();
          firstElement.focus();
        }
      }
    };

    document.addEventListener('keydown', handleEscape);
    document.addEventListener('keydown', handleTabKey);

    return () => {
      document.removeEventListener('keydown', handleEscape);
      document.removeEventListener('keydown', handleTabKey);
    };
  }, [isOpen, onClose]);

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

  if (!isOpen) return null;

  return (
    <div
      className="fixed inset-0 bg-gray-900 bg-opacity-20 flex items-center justify-center z-50 p-4"
      onClick={onClose}
      aria-hidden={!isOpen}
    >
      <div
        ref={modalRef}
        className="rounded-xl shadow-xl w-full max-w-lg mx-auto"
        style={{ backgroundColor: '#FFFFFF' }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="request-document-modal-title"
      >
        <form onSubmit={handleSubmit}>
          {/* Modal Header */}
          <div className="flex items-center justify-between p-6 border-b border-gray-200">
            <div className="flex items-center gap-3">
              <div
                className="p-2 rounded-lg"
                style={{ backgroundColor: '#FFF5F0' }}
              >
                <FileText className="w-6 h-6" style={{ color: '#FF6B01' }} />
              </div>
              <h2 id="request-document-modal-title" style={{ color: '#353535' }}>
                {t('title')}
              </h2>
            </div>
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

          {/* Modal Body */}
          <div className="p-6 space-y-6">
            {/* Document Title Field */}
            <div>
              <label
                htmlFor="document-title"
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center justify-between"
              >
                <span>{t('documentTitle')}</span>
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  {t('required')}
                </span>
              </label>
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
                aria-required="true"
                aria-invalid={!!errors.title}
                aria-describedby={errors.title ? 'title-error' : undefined}
              />
              {errors.title && (
                <p
                  id="title-error"
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  className="mt-2"
                  role="alert"
                >
                  {errors.title}
                </p>
              )}
            </div>

            {/* Instructions Field */}
            <div>
              <label
                htmlFor="instructions"
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center justify-between"
              >
                <span>{t('instructions')}</span>
                <span
                  style={{ color: '#6b7280', fontSize: '0.875rem' }}
                  aria-label="optional"
                >
                  {t('optional')}
                </span>
              </label>
              <Textarea
                id="instructions"
                value={instructions}
                onChange={(e) => setInstructions(e.target.value)}
                placeholder={t('instructionsPlaceholder')}
                rows={4}
                aria-required="false"
              />
              <p
                style={{ color: '#6b7280', fontSize: '0.875rem' }}
                className="mt-2"
              >
                {instructions.length} {i18n.language === 'en' ? 'characters' : 'caractères'}
              </p>
            </div>

            {/* Associated Stage Field */}
            <div>
              <label
                htmlFor="associated-stage"
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center justify-between"
              >
                <span>{t('associatedStage')}</span>
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  {t('required')}
                </span>
              </label>
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
                  className={`w-full ${errors.stage ? 'border-red-500' : ''}`}
                  aria-required="true"
                  aria-invalid={!!errors.stage}
                  aria-describedby={errors.stage ? 'stage-error' : undefined}
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
                <p
                  id="stage-error"
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  className="mt-2"
                  role="alert"
                >
                  {errors.stage}
                </p>
              )}
            </div>

            {/* Info Box */}
            <div
              className="p-4 rounded-lg border-2"
              style={{ borderColor: '#e5e7eb', backgroundColor: '#f9fafb' }}
            >
              <p style={{ color: '#353535', fontSize: '0.875rem' }}
                dangerouslySetInnerHTML={{
                  __html: t('infoText', { stage: selectedStage || (i18n.language === 'en' ? 'selected stage' : 'sélectionnée') })
                }}
              />
            </div>
          </div>

          {/* Modal Footer */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 p-6 border-t border-gray-200 bg-gray-50">
            <Button
              type="button"
              variant="outline"
              onClick={onClose}
              className="flex-1"
            >
              {t('cancel')}
            </Button>
            <Button
              type="submit"
              disabled={!isFormValid}
              className="flex-1 gap-2"
            >
              <Send className="w-5 h-5" />
              {t('sendRequest')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}