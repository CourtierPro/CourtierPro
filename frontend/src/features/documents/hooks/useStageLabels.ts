import { BUYER_STAGES, SELLER_STAGES, STAGE_DESCRIPTIONS } from '@/shared/utils/stages';
import { useTranslation } from 'react-i18next';

export function useStageLabels(side: string) {
  const { t, i18n } = useTranslation('transactions');
  const stages = side === 'BUY_SIDE' ? BUYER_STAGES : SELLER_STAGES;
  return stages.map((stage) => ({
    value: stage,
    label: t(`stages.${stage}`, STAGE_DESCRIPTIONS[stage] || stage),
  }));
}
