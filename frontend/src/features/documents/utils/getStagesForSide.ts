import { BUYER_STAGES, SELLER_STAGES } from '@/shared/utils/stages';

export function getStagesForSide(side: string): string[] {
  if (side === 'BUY_SIDE') return BUYER_STAGES;
  if (side === 'SELL_SIDE') return SELLER_STAGES;
  return [];
}
