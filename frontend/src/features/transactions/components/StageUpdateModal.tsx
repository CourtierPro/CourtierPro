import { useState, useEffect, useRef } from 'react';
import { getStagesForSide, enumToLabel } from '@/shared/utils/stages';
import { X, MessageSquare } from 'lucide-react';
import { useTranslation } from 'react-i18next';
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
// Note: no DTO import required here; parent handles mutation and types

interface StageUpdateModalProps {
  isOpen: boolean;
  onClose: () => void;
  transactionSide: 'buy' | 'sell';
  transactionId: string;
  // new: indicates whether an update is in progress
  isLoading?: boolean;
  // new: submit passes selected stage string and note
  onSubmit?: (stage: string, note: string) => Promise<void> | void;
}

export function StageUpdateModal(props: StageUpdateModalProps) {
  if (!props.isOpen) return null;
  return <StageUpdateForm {...props} />;
}

function StageUpdateForm({
  onClose,
  transactionSide,
  isLoading = false,
  onSubmit,
}: StageUpdateModalProps) {
  const { t, i18n } = useTranslation('transactions');
  const [selectedStage, setSelectedStage] = useState<string>('');
  const [progressNote, setProgressNote] = useState('');
  const [visibleToClient, setVisibleToClient] = useState(true);
  // mutation handled by parent via onSubmit; no local mutation here

  const modalRef = useRef<HTMLDivElement>(null);
  const firstFocusableRef = useRef<HTMLSelectElement>(null);

  const stageEnums = transactionSide === 'buy' ? getStagesForSide('BUY_SIDE') : getStagesForSide('SELL_SIDE');
  const stageDescriptions =
    transactionSide === 'buy'
      ? (t('buyStageDescriptions', { returnObjects: true }) as string[])
      : (t('sellStageDescriptions', { returnObjects: true }) as string[]);

  useEffect(() => {
    setTimeout(() => {
      firstFocusableRef.current?.focus();
    }, 100);
  }, []);

  useEffect(() => {
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
  }, [onClose]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!selectedStage) return;

    try {
      if (onSubmit) {
        await onSubmit(selectedStage, progressNote || '');
      }
      // If onSubmit succeeded, close the modal
      onClose();
    } catch (err) {
      console.error('Failed to update stage', err);
      // let caller handle toast/error display
    }

    // Reset form
    setSelectedStage('');
    setProgressNote('');
    setVisibleToClient(true);
  };

  const isFormValid = selectedStage !== '';

  return (
    <div
      className="fixed inset-0 bg-background/80 backdrop-blur-sm flex items-center justify-center z-50 p-4"
      onClick={onClose}
      aria-hidden={true}
    >
      <div
        ref={modalRef}
        className="rounded-xl shadow-xl w-full max-w-md mx-auto bg-card border border-border"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="stage-modal-title"
        aria-describedby="stage-modal-description"
      >
        <form onSubmit={handleSubmit}>
          <div
            className="flex items-center justify-between p-6 border-b border-border"
          >
            <h2 id="stage-modal-title" className="text-lg font-semibold text-foreground">
              {t('updateTransactionStage')}
            </h2>
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
            <div>
              <label
                htmlFor="stage-select"
                className="block mb-2 flex items-center gap-2 text-foreground"
              >
                {t('selectNewStage')}
                <span
                  className="text-destructive text-sm"
                  aria-label="required"
                >
                  *
                </span>
              </label>

              <Select
                value={selectedStage === '' ? undefined : selectedStage}
                onValueChange={(value) => setSelectedStage(value)}
                disabled={isLoading}
              >
                <SelectTrigger
                  id="stage-select"
                  className="w-full"
                  aria-required="true"
                  aria-describedby="stage-description-text"
                >
                  <SelectValue placeholder={t('selectStagePlaceholder')} />
                </SelectTrigger>
                <SelectContent>
                  {stageEnums.map((stageEnum, index) => {
                    const stageNumber = index + 1;
                    return (
                      <SelectItem key={stageEnum} value={stageEnum}>
                        {i18n.language === 'en' ? 'Stage' : 'Étape'} {stageNumber}: {enumToLabel(stageEnum)}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>

            {selectedStage !== '' && (
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

            <div>
              <label
                htmlFor="progress-note"
                className="block mb-2 text-foreground"
              >
                {t('progressNote')}
              </label>

              <Textarea
                id="progress-note"
                value={progressNote}
                onChange={(e) => setProgressNote(e.target.value)}
                rows={4}
                placeholder={t('progressNotePlaceholder')}
                aria-describedby="note-help-text"
                disabled={isLoading}
              />

              <div className="mt-3">
                <div className="flex items-start gap-3">
                  <Checkbox
                    id="visible-to-client"
                    checked={visibleToClient}
                    onCheckedChange={(checked) => setVisibleToClient(checked === true)}
                    disabled={isLoading}
                    aria-describedby="visibility-help-text"
                  />
                  <div className="flex-1">
                    <label htmlFor="visible-to-client" className="text-sm cursor-pointer">
                      {t('visibleToClient')}
                    </label>
                    <p
                      id="visibility-help-text"
                      className="text-xs text-muted-foreground mt-1"
                    >
                      {t('visibleToClientHelp')}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {!isFormValid && (
              <div className="mt-2" role="status">
                <p className="text-muted-foreground text-sm italic">
                  {t('selectStageFirst')}
                </p>
              </div>
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
              disabled={!isFormValid || isLoading}
              className="flex-1"
            >
              {isLoading ? t('updating') ?? 'Updating...' : t('updateTransactionStage')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}