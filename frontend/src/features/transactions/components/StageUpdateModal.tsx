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

interface StageUpdateModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (stageIndex: number, note: string, visibleToClient: boolean) => void;
  transactionSide: 'buy' | 'sell';
}

export function StageUpdateModal(props: StageUpdateModalProps) {
  if (!props.isOpen) return null;
  return <StageUpdateForm {...props} />;
}

function StageUpdateForm({
  onClose,
  onSubmit,
  transactionSide,
}: StageUpdateModalProps) {
  const { t, i18n } = useTranslation('transactions');
  const [selectedStage, setSelectedStage] = useState<number | ''>('');
  const [progressNote, setProgressNote] = useState('');
  const [visibleToClient, setVisibleToClient] = useState(true);

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

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();

    if (selectedStage === '') return;

    onSubmit(selectedStage as number, progressNote, visibleToClient);

    // Reset form
    setSelectedStage('');
    setProgressNote('');
    setVisibleToClient(true);
  };

  const isFormValid = selectedStage !== '';

  return (
    <div
      className="fixed inset-0 bg-gray-900 bg-opacity-20 flex items-center justify-center z-50 p-4"
      onClick={onClose}
      aria-hidden={true}
    >
      <div
        ref={modalRef}
        className="rounded-xl shadow-xl w-full max-w-md mx-auto bg-white"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="stage-modal-title"
        aria-describedby="stage-modal-description"
      >
        <form onSubmit={handleSubmit}>
          <div
            className="flex items-center justify-between p-6 border-b border-gray-200"
          >
            <h2 id="stage-modal-title" style={{ color: '#353535' }}>
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
                value={selectedStage === '' ? undefined : String(selectedStage)}
                onValueChange={(value) => setSelectedStage(Number(value))}
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
                      <SelectItem key={stageEnum} value={String(stageNumber)}>
                        {i18n.language === 'en' ? 'Stage' : 'Étape'} {stageNumber}: {enumToLabel(stageEnum)}
                      </SelectItem>
                    );
                  })}
                </SelectContent>
              </Select>
            </div>

            {selectedStage !== '' && (
              <div
                className="p-4 rounded-lg border-l-4 animate-fade-in bg-orange-50 border-l-primary"
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
                  {stageDescriptions[(selectedStage as number) - 1] ?? ''}
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
              />

              <div className="mt-3">
                <div className="flex items-start gap-3">
                  <Checkbox
                    id="visible-to-client"
                    checked={visibleToClient}
                    onCheckedChange={(checked) => setVisibleToClient(checked === true)}
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
              <div
                className="p-3 rounded-lg border border-gray-200 bg-muted"
                role="status"
              >
                <p className="text-muted-foreground text-sm">
                  {t('selectStageFirst')}
                </p>
              </div>
            )}
          </div>

          <div className="flex items-center gap-3 p-6 border-t border-gray-200">
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
              className="flex-1"
            >
              {t('updateTransactionStage')}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}