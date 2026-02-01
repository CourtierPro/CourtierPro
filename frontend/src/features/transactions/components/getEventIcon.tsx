// src/features/transactions/components/getEventIcon.tsx
import type { TimelineEntryDTO as TimelineEntry } from '@/shared/api/types';
import { Clock, FileText, CheckCircle, AlertCircle, AlertTriangle, Archive, Home, RefreshCcw, Trash2, FileCheck, XCircle, CalendarCheck, CalendarX, CalendarClock } from 'lucide-react';

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
        case 'PROPERTY_ADDED':
            return <Home className="w-5 h-5 text-green-500" />;
        case 'PROPERTY_UPDATED':
            return <RefreshCcw className="w-5 h-5 text-blue-500" />;
        case 'PROPERTY_REMOVED':
            return <Trash2 className="w-5 h-5 text-red-500" />;
        case 'CONDITION_ADDED':
            return <FileCheck className="w-5 h-5 text-purple-500" />;
        case 'CONDITION_UPDATED':
            return <RefreshCcw className="w-5 h-5 text-blue-500" />;
        case 'CONDITION_SATISFIED':
            return <CheckCircle className="w-5 h-5 text-green-500" />;
        case 'CONDITION_FAILED':
            return <XCircle className="w-5 h-5 text-red-500" />;
        case 'CONDITION_REMOVED':
            return <Trash2 className="w-5 h-5 text-red-500" />;
        case 'APPOINTMENT_CONFIRMED':
            return <CalendarCheck className="w-5 h-5 text-green-600" />;
        case 'APPOINTMENT_CANCELLED':
        case 'APPOINTMENT_DECLINED':
            return <CalendarX className="w-5 h-5 text-red-500" />;
        case 'APPOINTMENT_RESCHEDULED':
            return <CalendarClock className="w-5 h-5 text-orange-500" />;
        case 'APPOINTMENT_REQUESTED':
            return <CalendarClock className="w-5 h-5 text-blue-500" />;
        default:
            return <Clock className="w-5 h-5 text-gray-400" />;
    }
}
