import React from 'react';
import { Button } from '@/shared/components/ui/button';
import { useTranslation } from 'react-i18next';

interface StageSelectorProps {
  stages: string[];
  selectedStage: string;
  onSelectStage: (stage: string) => void;
  getStageLabel?: (stage: string) => string;
}

export const StageSelector: React.FC<StageSelectorProps> = ({ stages, selectedStage, onSelectStage, getStageLabel }) => {
  const { t } = useTranslation('transactions');
  return (
    <div className="flex flex-wrap gap-2 mb-4">
      {stages.map((stage) => (
        <Button
          key={stage}
          variant={selectedStage === stage ? 'default' : 'outline'}
          onClick={() => onSelectStage(stage)}
          className="capitalize"
        >
          {getStageLabel ? getStageLabel(stage) : (() => {
            const sideKey = stage.toLowerCase().startsWith('seller') ? 'sell' : 'buy';
            return t(`stages.${sideKey}.${stage.toLowerCase()}.name`, stage);
          })()}
        </Button>
      ))}
    </div>
  );
};
