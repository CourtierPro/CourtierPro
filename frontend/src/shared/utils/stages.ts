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

type StageSideKey = 'buy' | 'sell';

function normalizeStageValue(value: string): string {
  return value
    .trim()
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace(/[^A-Za-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .replace(/_+/g, '_')
    .toLowerCase();
}

function inferSideKeys(
  normalizedValue: string,
  side?: 'BUY_SIDE' | 'SELL_SIDE',
): StageSideKey[] {
  if (side === 'BUY_SIDE') return ['buy'];
  if (side === 'SELL_SIDE') return ['sell'];
  if (normalizedValue.startsWith('buyer_')) return ['buy'];
  if (normalizedValue.startsWith('seller_')) return ['sell'];
  return ['buy', 'sell'];
}

function buildStageCandidates(normalizedValue: string): string[] {
  if (!normalizedValue) return [];
  if (normalizedValue.startsWith('buyer_') || normalizedValue.startsWith('seller_')) {
    return [normalizedValue];
  }
  return [normalizedValue, `buyer_${normalizedValue}`, `seller_${normalizedValue}`];
}

function findStageTranslation(
  value: string,
  t: TFunction<'transactions'>,
  field: 'name' | 'description' | 'goal',
  side?: 'BUY_SIDE' | 'SELL_SIDE',
): string {
  const normalizedValue = normalizeStageValue(value);
  if (!normalizedValue) return '';

  const candidates = buildStageCandidates(normalizedValue);
  for (const candidate of candidates) {
    const sideKeys = inferSideKeys(candidate, side);
    for (const sideKey of sideKeys) {
      const translation = t(`stages.${sideKey}.${candidate}.${field}`, { defaultValue: '' });
      if (translation) {
        return translation;
      }
    }
  }

  return '';
}

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

  const val = value
    .trim()
    .replace(/([a-z])([A-Z])/g, '$1_$2')
    .replace(/[^A-Za-z0-9]+/g, '_')
    .replace(/^_+|_+$/g, '')
    .replace(/_+/g, '_')
    .replace(/^(BUYER|SELLER)_/i, '');

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

  const translation = findStageTranslation(value, t, 'name', side);
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
  return findStageTranslation(value, t, 'description', side);
}

// -----------------------------
// Get translated stage goal using i18n
// -----------------------------
export function getStageGoal(
  value: string,
  t: TFunction<'transactions'>,
  side?: 'BUY_SIDE' | 'SELL_SIDE',
): string {
  return findStageTranslation(value, t, 'goal', side);
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
