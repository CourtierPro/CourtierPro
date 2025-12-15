// src/features/transactions/components/getEventTypeLabel.ts
import type { TimelineEntryDTO as TimelineEntry } from '@/shared/api/types';

export function getEventTypeLabel(
  type: TimelineEntry['type'],
  t: (k: string, o?: Record<string, unknown>) => string
) {
  return t(`timelineType.${type}`);
}