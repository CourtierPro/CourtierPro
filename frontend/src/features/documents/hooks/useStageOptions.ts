
import { useTranslation } from 'react-i18next';
import { BUYER_STAGES, SELLER_STAGES, getStageLabel } from '@/shared/utils/stages';

export function useStageOptions(side: 'BUY_SIDE' | 'SELL_SIDE') {
  const { t } = useTranslation('transactions');
  const stages = side === 'SELL_SIDE' ? SELLER_STAGES : BUYER_STAGES;
  return stages.map(stage => ({
    value: stage,
    label: getStageLabel(stage, t, side)
  }));
}
