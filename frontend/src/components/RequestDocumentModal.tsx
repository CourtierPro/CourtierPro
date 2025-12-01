import React, { useState, useEffect, useRef } from 'react';
import { X, Send, FileText, ChevronDown } from 'lucide-react';

interface RequestDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (documentTitle: string, instructions: string, stage: string) => void;
  transactionType: 'buy' | 'sell';
  currentStage: string;
  language: 'en' | 'fr';
}

const translations = {
  en: {
    title: 'Request Additional Document',
    documentTitle: 'Document Title',
    documentTitlePlaceholder: 'e.g., Additional Proof of Income',
    instructions: 'Instructions or Description',
    instructionsPlaceholder: 'Provide details about what the client needs to submit...',
    associatedStage: 'Associated Stage',
    selectStage: 'Select a stage',
    cancel: 'Cancel',
    sendRequest: 'Send Request',
    required: 'Required',
    optional: 'Optional',
    documentTitleRequired: 'Document title is required',
    stageRequired: 'Please select a stage',
    stages: {
      buy: {
        offer: 'Offer',
        inspection: 'Inspection',
        financing: 'Financing',
        legalReview: 'Legal Review',
        closing: 'Closing',
      },
      sell: {
        listing: 'Listing',
        marketing: 'Marketing',
        offer: 'Offer',
        inspection: 'Inspection',
        closing: 'Closing',
      },
    },
  },
  fr: {
    title: 'Demander un document supplémentaire',
    documentTitle: 'Titre du document',
    documentTitlePlaceholder: 'ex. : Preuve de revenu supplémentaire',
    instructions: 'Instructions ou description',
    instructionsPlaceholder: 'Fournissez des détails sur ce que le client doit soumettre...',
    associatedStage: 'Étape associée',
    selectStage: 'Sélectionner une étape',
    cancel: 'Annuler',
    sendRequest: 'Envoyer la demande',
    required: 'Requis',
    optional: 'Optionnel',
    documentTitleRequired: 'Le titre du document est requis',
    stageRequired: 'Veuillez sélectionner une étape',
    stages: {
      buy: {
        offer: 'Offre',
        inspection: 'Inspection',
        financing: 'Financement',
        legalReview: 'Révision légale',
        closing: 'Clôture',
      },
      sell: {
        listing: 'Inscription',
        marketing: 'Marketing',
        offer: 'Offre',
        inspection: 'Inspection',
        closing: 'Clôture',
      },
    },
  },
};

export function RequestDocumentModal({
  isOpen,
  onClose,
  onSubmit,
  transactionType,
  currentStage,
  language,
}: RequestDocumentModalProps) {
  const [documentTitle, setDocumentTitle] = useState('');
  const [instructions, setInstructions] = useState('');
  const [selectedStage, setSelectedStage] = useState('');
  const [errors, setErrors] = useState<{ title?: string; stage?: string }>({});

  const modalRef = useRef<HTMLDivElement>(null);
  const firstInputRef = useRef<HTMLInputElement>(null);

  const t = translations[language];
  const stageOptions = Object.entries(t.stages[transactionType]);

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
  }, [isOpen, currentStage]);

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
      newErrors.title = t.documentTitleRequired;
    }

    if (!selectedStage) {
      newErrors.stage = t.stageRequired;
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
                {t.title}
              </h2>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="p-1 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] transition-colors"
              aria-label="Close modal"
            >
              <X className="w-5 h-5" style={{ color: '#353535' }} />
            </button>
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
                <span>{t.documentTitle}</span>
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  {t.required}
                </span>
              </label>
              <input
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
                placeholder={t.documentTitlePlaceholder}
                className={`w-full p-3 rounded-lg border-2 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent transition-all ${
                  errors.title ? 'border-red-500' : 'border-gray-200'
                }`}
                style={{ color: '#353535' }}
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
                <span>{t.instructions}</span>
                <span
                  style={{ color: '#6b7280', fontSize: '0.875rem' }}
                  aria-label="optional"
                >
                  {t.optional}
                </span>
              </label>
              <textarea
                id="instructions"
                value={instructions}
                onChange={(e) => setInstructions(e.target.value)}
                placeholder={t.instructionsPlaceholder}
                rows={4}
                className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent resize-none transition-all"
                style={{ color: '#353535' }}
                aria-required="false"
              />
              <p
                style={{ color: '#6b7280', fontSize: '0.875rem' }}
                className="mt-2"
              >
                {instructions.length} {language === 'en' ? 'characters' : 'caractères'}
              </p>
            </div>

            {/* Associated Stage Field */}
            <div>
              <label
                htmlFor="associated-stage"
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center justify-between"
              >
                <span>{t.associatedStage}</span>
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  {t.required}
                </span>
              </label>
              <div className="relative">
                <select
                  id="associated-stage"
                  value={selectedStage}
                  onChange={(e) => {
                    setSelectedStage(e.target.value);
                    if (errors.stage) {
                      setErrors({ ...errors, stage: undefined });
                    }
                  }}
                  className={`w-full p-3 pr-10 rounded-lg border-2 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent appearance-none transition-all ${
                    errors.stage ? 'border-red-500' : 'border-gray-200'
                  }`}
                  style={{ color: '#353535' }}
                  aria-required="true"
                  aria-invalid={!!errors.stage}
                  aria-describedby={errors.stage ? 'stage-error' : undefined}
                >
                  <option value="">{t.selectStage}</option>
                  {stageOptions.map(([key, value]) => (
                    <option key={key} value={value}>
                      {value}
                    </option>
                  ))}
                </select>
                <ChevronDown
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none"
                  style={{ color: '#353535', opacity: 0.5 }}
                />
              </div>
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
              <p style={{ color: '#353535', fontSize: '0.875rem' }}>
                {language === 'en' ? (
                  <>
                    This document will be added to the client's checklist under the{' '}
                    <strong>{selectedStage || 'selected stage'}</strong> and marked as
                    pending.
                  </>
                ) : (
                  <>
                    Ce document sera ajouté à la liste de contrôle du client sous l'étape{' '}
                    <strong>{selectedStage || 'sélectionnée'}</strong> et marqué comme
                    en attente.
                  </>
                )}
              </p>
            </div>
          </div>

          {/* Modal Footer */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-3 p-6 border-t border-gray-200 bg-gray-50">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-3 rounded-lg hover:bg-gray-200 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors border-2 border-gray-200"
              style={{ color: '#353535', backgroundColor: '#FFFFFF' }}
            >
              {t.cancel}
            </button>
            <button
              type="submit"
              disabled={!isFormValid}
              className="flex-1 px-4 py-3 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              style={{
                backgroundColor: isFormValid ? '#FF6B01' : '#e5e7eb',
                color: isFormValid ? '#FFFFFF' : '#9ca3af',
              }}
              aria-disabled={!isFormValid}
            >
              <Send className="w-5 h-5" />
              {t.sendRequest}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}