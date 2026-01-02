import React from 'react';
import { useTranslation } from 'react-i18next';
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuTrigger } from '@/shared/components/ui/dropdown-menu';
import { Button } from '@/shared/components/ui/button';

interface StageDropdownSelectorProps {
  stages: { value: string; label: string }[];
  selectedStage: string;
  onSelectStage: (stage: string) => void;
}

export const StageDropdownSelector: React.FC<StageDropdownSelectorProps> = ({ stages, selectedStage, onSelectStage }) => {
  const { t } = useTranslation('transactions');
  const selected = stages.find(s => s.value === selectedStage);
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          className="capitalize min-w-[220px] text-left border-2 border-orange-500 focus:ring-orange-500 focus:border-orange-600"
        >
          {selected ? selected.label : t('selectStage', 'Sélectionner une étape')}
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start">
        {stages.map(stage => (
          <DropdownMenuItem key={stage.value} onClick={() => onSelectStage(stage.value)}>
            {stage.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};
