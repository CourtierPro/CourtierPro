import { useState, useEffect, useRef } from 'react';
import { getStagesForSide, enumToLabel } from '@/utils/stages';
import { X, ChevronDown, MessageSquare } from 'lucide-react';

interface StageUpdateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (stageIndex: number, note: string, visibleToClient: boolean) => void;
  transactionSide: 'buy' | 'sell';
  language: 'en' | 'fr';
}

const translations = {
  en: {
    title: 'Update Transaction Stage',
    selectStage: 'Select New Stage',
    selectStagePlaceholder: 'Choose the next stage...',
    stageDescription: 'Stage Description',
    progressNote: 'Progress Note',
    progressNotePlaceholder: 'Add a note about this stage change (optional)...',
    visibleToClient: 'Visible to Client',
    visibleToClientHelp: 'When checked, this note will be visible to the client in their timeline',
    cancel: 'Cancel',
    updateStage: 'Update Stage',
    selectStageFirst: 'Please select a stage to continue',
    // stage lists removed — UI reads enums from src/utils/stages.ts
    buyStageDescriptions: [
      'The buyer\'s offer has been submitted to the seller and is awaiting review and response.',
      'The seller has accepted the buyer\'s offer. Moving forward with due diligence and conditions.',
      'Professional property inspection is scheduled or completed. Review findings with client.',
      'Buyer is securing mortgage pre-approval or final approval from their financial institution.',
      'Legal documents including purchase agreement are being reviewed by the buyer\'s lawyer.',
      'Final property walkthrough scheduled to verify condition before closing.',
      'All conditions met. Finalizing documents and preparing for ownership transfer.',
      'Transaction successfully completed. Keys have been transferred to the new owner.',
    ],
    sellStageDescriptions: [
      'Property is officially listed on the market and available for showings.',
      'Active marketing campaign underway. Property is being promoted to potential buyers.',
      'An offer has been received from a potential buyer and is under review.',
      'Negotiating terms, conditions, and price with the potential buyer.',
      'Seller has accepted an offer. Moving forward with buyer\'s due diligence period.',
      'Legal documents are being reviewed and prepared by the seller\'s lawyer.',
      'All conditions met. Finalizing sale documents and preparing for ownership transfer.',
      'Sale successfully completed. Property ownership has been transferred to the buyer.',
    ],
  },
  fr: {
    title: 'Mettre à jour l\'étape de la transaction',
    selectStage: 'Sélectionner une nouvelle étape',
    selectStagePlaceholder: 'Choisir la prochaine étape...',
    stageDescription: 'Description de l\'étape',
    progressNote: 'Note de progression',
    progressNotePlaceholder: 'Ajouter une note sur ce changement d\'étape (optionnel)...',
    visibleToClient: 'Visible pour le client',
    visibleToClientHelp: 'Si coché, cette note sera visible pour le client dans sa chronologie',
    cancel: 'Annuler',
    updateStage: 'Mettre à jour l\'étape',
    selectStageFirst: 'Veuillez sélectionner une étape pour continuer',
    // stage lists removed — UI reads enums from src/utils/stages.ts
    buyStageDescriptions: [
      'L\'offre de l\'acheteur a été soumise au vendeur et est en attente d\'examen et de réponse.',
      'Le vendeur a accepté l\'offre de l\'acheteur. Nous passons à la vérification diligente et aux conditions.',
      'L\'inspection professionnelle de la propriété est prévue ou terminée. Examinez les résultats avec le client.',
      'L\'acheteur obtient une pré-approbation ou approbation finale de son institution financière.',
      'Les documents juridiques incluant le contrat d\'achat sont examinés par l\'avocat de l\'acheteur.',
      'Visite finale de la propriété prévue pour vérifier l\'état avant la clôture.',
      'Toutes les conditions sont remplies. Finalisation des documents et préparation du transfert de propriété.',
      'Transaction terminée avec succès. Les clés ont été transférées au nouveau propriétaire.',
    ],
    sellStageDescriptions: [
      'La propriété est officiellement inscrite sur le marché et disponible pour les visites.',
      'Campagne de marketing active en cours. La propriété est promue auprès des acheteurs potentiels.',
      'Une offre a été reçue d\'un acheteur potentiel et est en cours d\'examen.',
      'Négociation des termes, conditions et prix avec l\'acheteur potentiel.',
      'Le vendeur a accepté une offre. Nous passons à la période de vérification diligente de l\'acheteur.',
      'Les documents juridiques sont examinés et préparés par l\'avocat du vendeur.',
      'Toutes les conditions sont remplies. Finalisation des documents de vente et préparation du transfert.',
      'Vente terminée avec succès. La propriété a été transférée à l\'acheteur.',
    ],
  },
};

export function StageUpdateModal({
  isOpen,
  onClose,
  onSubmit,
  transactionSide,
  language,
}: StageUpdateModalProps) {
  const [selectedStage, setSelectedStage] = useState<number | ''>('');
  const [progressNote, setProgressNote] = useState('');
  const [visibleToClient, setVisibleToClient] = useState(true);

  const modalRef = useRef<HTMLDivElement>(null);
  const firstFocusableRef = useRef<HTMLSelectElement>(null);

  const t = translations[language];
  const stageEnums = transactionSide === 'buy' ? getStagesForSide('BUY_SIDE') : getStagesForSide('SELL_SIDE');
  const stageDescriptions =
    transactionSide === 'buy' ? t.buyStageDescriptions : t.sellStageDescriptions;

  // Reset form when modal opens/closes
  useEffect(() => {
    if (isOpen) {
      setSelectedStage('');
      setProgressNote('');
      setVisibleToClient(true);
      // Focus the first input when modal opens
      setTimeout(() => {
        firstFocusableRef.current?.focus();
      }, 100);
    }
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
        // Shift + Tab
        if (document.activeElement === firstElement) {
          e.preventDefault();
          lastElement.focus();
        }
      } else {
        // Tab
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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (selectedStage === '') return;

    onSubmit(selectedStage as number, progressNote, visibleToClient);

    // Reset form
    setSelectedStage('');
    setProgressNote('');
    setVisibleToClient(true);
  };

  if (!isOpen) return null;

  const isFormValid = selectedStage !== '';

  return (
    <div
      className="fixed inset-0 bg-gray-900 bg-opacity-20 flex items-center justify-center z-50 p-4"
      onClick={onClose}
      aria-hidden={!isOpen}
    >
      <div
        ref={modalRef}
        className="rounded-xl shadow-xl w-full max-w-md mx-auto"
        style={{ backgroundColor: '#FFFFFF' }}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="stage-modal-title"
        aria-describedby="stage-modal-description"
      >
        <form onSubmit={handleSubmit}>
          {/* Modal Header */}
          <div
            className="flex items-center justify-between p-6 border-b border-gray-200"
          >
            <h2 id="stage-modal-title" style={{ color: '#353535' }}>
              {t.title}
            </h2>
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
            {/* Stage Selection */}
            <div>
              <label
                htmlFor="stage-select"
                style={{ color: '#353535' }}
                className="block mb-2 flex items-center gap-2"
              >
                {t.selectStage}
                <span
                  style={{ color: '#ef4444', fontSize: '0.875rem' }}
                  aria-label="required"
                >
                  *
                </span>
              </label>

              <div className="relative">
                <select
                  ref={firstFocusableRef}
                  id="stage-select"
                  value={selectedStage}
                  onChange={(e) => setSelectedStage(Number(e.target.value))}
                  className="w-full p-3 pr-10 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent appearance-none"
                  style={{ color: '#353535' }}
                  aria-required="true"
                  aria-describedby="stage-description-text"
                >
                  <option value="">{t.selectStagePlaceholder}</option>
                  {stageEnums.map((stageEnum, index) => {
                    const stageNumber = index + 1;
                    return (
                      <option key={stageEnum} value={stageNumber}>
                        {language === 'en' ? 'Stage' : 'Étape'} {stageNumber}: {enumToLabel(stageEnum)}
                      </option>
                    );
                  })}
                </select>
                <ChevronDown
                  className="absolute right-3 top-1/2 transform -translate-y-1/2 w-5 h-5 pointer-events-none"
                  style={{ color: '#353535', opacity: 0.5 }}
                />
              </div>
            </div>

            {/* Stage Description */}
            {selectedStage !== '' && (
              <div
                className="p-4 rounded-lg border-l-4 animate-fade-in"
                style={{
                  backgroundColor: '#FFF5F0',
                  borderLeftColor: '#FF6B01',
                }}
                id="stage-description-text"
                role="status"
                aria-live="polite"
              >
                <div className="flex items-start gap-2 mb-2">
                  <MessageSquare className="w-5 h-5 flex-shrink-0 mt-0.5" style={{ color: '#FF6B01' }} />
                  <p style={{ color: '#353535', fontSize: '0.875rem' }}>
                    <span className="opacity-70">{t.stageDescription}:</span>
                  </p>
                </div>
                <p style={{ color: '#353535', fontSize: '0.875rem' }}>
                  {stageDescriptions[(selectedStage as number) - 1] ?? ''}
                </p>
              </div>
            )}

            {/* Progress Note */}
            <div>
              <label
                htmlFor="progress-note"
                style={{ color: '#353535' }}
                className="block mb-2"
              >
                {t.progressNote}
              </label>

              <textarea
                id="progress-note"
                value={progressNote}
                onChange={(e) => setProgressNote(e.target.value)}
                rows={4}
                className="w-full p-3 rounded-lg border-2 border-gray-200 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:border-transparent resize-none"
                style={{ color: '#353535' }}
                placeholder={t.progressNotePlaceholder}
                aria-describedby="note-help-text"
              />

              {/* Visible to Client Checkbox */}
              <div className="mt-3">
                <label className="flex items-start gap-3 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={visibleToClient}
                    onChange={(e) => setVisibleToClient(e.target.checked)}
                    className="mt-1 w-4 h-4 rounded focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2"
                    style={{ accentColor: '#FF6B01' }}
                    aria-describedby="visibility-help-text"
                  />
                  <div className="flex-1">
                    <p style={{ color: '#353535', fontSize: '0.875rem' }}>
                      {t.visibleToClient}
                    </p>
                    <p
                      id="visibility-help-text"
                      style={{ color: '#353535', opacity: 0.7, fontSize: '0.75rem' }}
                      className="mt-1"
                    >
                      {t.visibleToClientHelp}
                    </p>
                  </div>
                </label>
              </div>
            </div>

            {/* Validation Message */}
            {!isFormValid && (
              <div
                className="p-3 rounded-lg border border-gray-200"
                style={{ backgroundColor: '#f9fafb' }}
                role="status"
              >
                <p style={{ color: '#353535', opacity: 0.7, fontSize: '0.875rem' }}>
                  {t.selectStageFirst}
                </p>
              </div>
            )}
          </div>

          {/* Modal Footer */}
          <div className="flex items-center gap-3 p-6 border-t border-gray-200">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-4 py-3 rounded-lg hover:bg-gray-100 focus:outline-none focus:ring-2 focus:ring-gray-300 focus:ring-offset-2 transition-colors border-2 border-gray-200"
              style={{ color: '#353535' }}
            >
              {t.cancel}
            </button>
            <button
              type="submit"
              disabled={!isFormValid}
              className="flex-1 px-4 py-3 rounded-lg hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-[#FF6B01] focus:ring-offset-2 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
              style={{
                backgroundColor: isFormValid ? '#FF6B01' : '#e5e7eb',
                color: isFormValid ? '#FFFFFF' : '#9ca3af',
              }}
              aria-disabled={!isFormValid}
            >
              {t.updateStage}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}