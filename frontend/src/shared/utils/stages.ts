// utils/stages.ts
// Centralized stage enums and helpers for transactions — clean + deduped + consistent
import type { TFunction } from 'i18next';

// -----------------------------
// BUYER STAGES (backend-order)
// -----------------------------
export const BUYER_STAGES = [
  'BUYER_FINANCIAL_PREPARATION',
  'BUYER_PROPERTY_SEARCH',
  'BUYER_OFFER_AND_NEGOTIATION',
  'BUYER_FINANCING_AND_CONDITIONS',
  'BUYER_NOTARY_AND_SIGNING',
  'BUYER_POSSESSION',
];

// -----------------------------
// SELLER STAGES (backend-order)
// -----------------------------
export const SELLER_STAGES = [
  'SELLER_INITIAL_CONSULTATION',
  'SELLER_PUBLISH_LISTING',
  'SELLER_OFFER_AND_NEGOTIATION',
  'SELLER_FINANCING_AND_CONDITIONS',
  'SELLER_NOTARY_AND_SIGNING',
  'SELLER_HANDOVER',
];

// -----------------------------
// Transaction Side Enum
// -----------------------------
export type TransactionSideEnum = 'BUY_SIDE' | 'SELL_SIDE';

// -----------------------------
// Stage selector by side
// -----------------------------
export function getStagesForSide(side: string | undefined): string[] {
  if (side === 'BUY_SIDE') return BUYER_STAGES;
  if (side === 'SELL_SIDE') return SELLER_STAGES;
  return [];
}

// -----------------------------
// Convert enum string → UI label
// BUYER_FINANCIAL_PREPARATION → "Financial Preparation"
// -----------------------------
export function enumToLabel(value?: string): string {
  if (!value || typeof value !== 'string') return 'Unknown';

  const val = value.replace(/^BUYER_|^SELLER_/, '');
  return val
    .split(/[_\s]+/)
    .filter(Boolean)
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

// -----------------------------
// Get translated stage label using i18n
// Returns translated stage name from transactions.json
// -----------------------------
export function getStageLabel(
  value?: string,
  t?: TFunction<'transactions'>,
  side?: 'BUY_SIDE' | 'SELL_SIDE',
): string {
  if (!value || typeof value !== 'string' || !t) {
    return enumToLabel(value);
  }

  const lowerValue = value.toLowerCase();
  const sideKey = side === 'SELL_SIDE' || lowerValue.includes('seller') ? 'sell' : 'buy';
  const translationKey = `stages.${sideKey}.${lowerValue}.name`;

  const translation = t(translationKey, { defaultValue: '' });
  return translation || enumToLabel(value);
}

// -----------------------------
// Get translated stage description using i18n
// -----------------------------
export function getStageDescription(
  value: string,
  t: TFunction<'transactions'>,
  side?: 'BUY_SIDE' | 'SELL_SIDE',
): string {
  const lowerValue = value.toLowerCase();
  const sideKey = side === 'SELL_SIDE' || lowerValue.includes('seller') ? 'sell' : 'buy';
  return t(`stages.${sideKey}.${lowerValue}.description`, { defaultValue: '' });
}

// -----------------------------
// Get translated stage goal using i18n
// -----------------------------
export function getStageGoal(
  value: string,
  t: TFunction<'transactions'>,
  side?: 'BUY_SIDE' | 'SELL_SIDE',
): string {
  const lowerValue = value.toLowerCase();
  const sideKey = side === 'SELL_SIDE' || lowerValue.includes('seller') ? 'sell' : 'buy';
  return t(`stages.${sideKey}.${lowerValue}.goal`, { defaultValue: '' });
}

// -----------------------------
// Resolve stage index (0-based)
// Supports:
//  - backend numeric stages (1-based)
//  - enum strings
//  - fallback to 0
// -----------------------------
export function resolveStageIndex(
  currentStage: string | number | undefined | null,
  stages: string[],
): number {
  if (!stages || stages.length === 0) return 0;

  // numeric (backend legacy)
  if (typeof currentStage === 'number') {
    return Math.max(0, Math.min(stages.length - 1, currentStage - 1));
  }

  // enum string exact match
  if (typeof currentStage === 'string') {
    const exact = stages.indexOf(currentStage);
    if (exact >= 0) return exact;

    // relaxed match: remove prefix and match by suffix
    const short = currentStage.replace(/^BUYER_|^SELLER_/, '');
    const idx = stages.findIndex((s) => s.endsWith(short));
    if (idx >= 0) return idx;

    // normalized comparison
    const normalized = currentStage.toUpperCase();
    const idxNorm = stages.findIndex((s) => s.toUpperCase() === normalized);
    if (idxNorm >= 0) return idxNorm;
  }

  return 0;
}
