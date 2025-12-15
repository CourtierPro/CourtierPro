// src/features/transactions/components/getEventIcon.tsx
import type { TimelineEntryDTO as TimelineEntry } from '@/shared/api/types';
import { Clock, FileText, CheckCircle, AlertCircle, AlertTriangle, Archive } from 'lucide-react';

/**
 * Returns the appropriate icon for a timeline event type.
 * Centralized to avoid duplication across timeline components.
 */
export function getEventIcon(type: TimelineEntry['type']) {
    switch (type) {
        case 'CREATED':
            return <Archive className="w-5 h-5 text-blue-500" />;
        case 'STAGE_CHANGE':
            return <AlertCircle className="w-5 h-5 text-blue-500" />;
        case 'NOTE':
        case 'TRANSACTION_NOTE':
            return <FileText className="w-5 h-5 text-gray-500" />;
        case 'DOCUMENT_REQUESTED':
            return <FileText className="w-5 h-5 text-amber-500" />;
        case 'DOCUMENT_SUBMITTED':
            return <FileText className="w-5 h-5 text-purple-500" />;
        case 'DOCUMENT_APPROVED':
            return <CheckCircle className="w-5 h-5 text-green-500" />;
        case 'DOCUMENT_NEEDS_REVISION':
            return <AlertTriangle className="w-5 h-5 text-yellow-500" />;
        default:
            return <Clock className="w-5 h-5 text-gray-400" />;
    }
}
