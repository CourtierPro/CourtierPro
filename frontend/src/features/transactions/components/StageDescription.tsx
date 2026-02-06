import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { ChevronLeft, ChevronRight } from 'lucide-react';
import { Button } from '@/shared/components/ui/button';
import { Section } from '@/shared/components/branded/Section';
import { getStagesForSide, getStageLabel, getStageDescription, getStageGoal, resolveStageIndex } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';

interface StageDescriptionProps {
  transaction: Transaction;
}

export function StageDescription({ transaction }: StageDescriptionProps) {
  const { t } = useTranslation('transactions');
  const stages = getStagesForSide(transaction.side);
  const currentStageIndex = resolveStageIndex(transaction.currentStage, stages);
  const [viewIndex, setViewIndex] = useState(currentStageIndex);

  useEffect(() => {
    setViewIndex(currentStageIndex);
  }, [currentStageIndex]);

  if (stages.length === 0) return null;

  const stage = stages[viewIndex];
  const side = transaction.side as 'BUY_SIDE' | 'SELL_SIDE';
  const label = getStageLabel(stage, t, side);
  const description = getStageDescription(stage, t, side);
  const goal = getStageGoal(stage, t, side);

  return (
    <Section className="p-3 md:p-6 h-full">
      <div className="flex flex-col gap-3 h-full">
        <div className="flex items-center justify-between gap-2">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setViewIndex((i) => Math.max(0, i - 1))}
            disabled={viewIndex === 0}
            aria-label={t('stageDescription.previousStage')}
            className="shrink-0"
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <h3 className="text-base md:text-lg font-semibold text-foreground text-center">
            {viewIndex + 1}. {label}
          </h3>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setViewIndex((i) => Math.min(stages.length - 1, i + 1))}
            disabled={viewIndex === stages.length - 1}
            aria-label={t('stageDescription.nextStage')}
            className="shrink-0"
          >
            <ChevronRight className="h-4 w-4" />
          </Button>
        </div>
        <p className="text-sm text-muted-foreground leading-relaxed flex-1">
          {description}
        </p>
        {goal && (
          <p className="text-sm font-medium text-foreground">
            <span className="text-primary">{t('stageDescription.goalLabel')}</span> {goal}
          </p>
        )}
      </div>
    </Section>
  );
}
