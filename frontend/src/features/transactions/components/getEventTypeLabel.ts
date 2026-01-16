// src/features/transactions/components/getEventTypeLabel.ts
import type { TimelineEntryDTO as TimelineEntry } from '@/shared/api/types';

export function getEventTypeLabel(
  type: TimelineEntry['type'],
  t: (k: string, o?: Record<string, unknown>) => string
) {
  if (String(type) === 'DOCUMENT_REQUEST_UPDATED') {
    // This label is handled in TransactionTimeline, not here, so return empty string to avoid double label
    return '';
  }
  return t(`timelineType.${type}`);
}