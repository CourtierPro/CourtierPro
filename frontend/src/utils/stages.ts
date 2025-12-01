// utils/stages.ts
// Centralized stage enums and helpers for transactions — clean + deduped + consistent

// -----------------------------
// BUYER STAGES (backend-order)
// -----------------------------
export const BUYER_STAGES = [
  'BUYER_PREQUALIFY_FINANCIALLY',
  'BUYER_SHOP_FOR_PROPERTY',
  'BUYER_SUBMIT_OFFER',
  'BUYER_OFFER_ACCEPTED',
  'BUYER_HOME_INSPECTION',
  'BUYER_FINANCING_FINALIZED',
  'BUYER_FIRST_NOTARY_APPOINTMENT',
  'BUYER_SECOND_NOTARY_APPOINTMENT',
  'BUYER_OCCUPANCY',
  'BUYER_TERMINATED',
];

// -----------------------------
// SELLER STAGES (backend-order)
// -----------------------------
export const SELLER_STAGES = [
  'SELLER_INITIAL_CONSULTATION',
  'SELLER_LISTING_PUBLISHED',
  'SELLER_REVIEW_OFFERS',
  'SELLER_ACCEPT_BEST_OFFER',
  'SELLER_CONDITIONS_MET',
  'SELLER_NOTARY_COORDINATION',
  'SELLER_NOTARY_APPOINTMENT',
  'SELLER_HANDOVER_KEYS',
  'SELLER_TERMINATED',
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
// BUYER_HOME_INSPECTION → "Home Inspection"
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

// -----------------------------
// Termination check
// -----------------------------
export function isTerminatedStage(
  currentStage: string | number | undefined,
  stages: string[],
): boolean {
  if (typeof currentStage === 'string') {
    return currentStage.endsWith('_TERMINATED');
  }
  const idx = resolveStageIndex(currentStage, stages);
  return stages[idx]?.endsWith('_TERMINATED') ?? false;
}
