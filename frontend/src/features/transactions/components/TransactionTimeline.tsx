import { useTransactionTimeline } from '@/features/transactions/api/queries';
import type { TimelineEntryDTO as TimelineEntry } from '@/shared/api/types';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { Section } from '@/shared/components/branded/Section';
import { useTranslation } from 'react-i18next';
import { Clock, FileText, CheckCircle, AlertCircle, AlertTriangle, Archive } from 'lucide-react';
import { formatDateTime } from '@/shared/utils/date';

interface TransactionTimelineProps {
    transactionId: string;
}

function getEventIcon(type: TimelineEntry['type']) {
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



function getEventTypeLabel(type: TimelineEntry['type'], t: (k: string, o?: any) => string, tDoc: (k: string, o?: any) => string) {
    // Utilise la clé timelineType.* pour tous les types
    return t(`timelineType.${type}`);
}

export function TransactionTimeline({ transactionId }: TransactionTimelineProps) {
    const { t } = useTranslation('transactions');
    const { t: tDoc } = useTranslation('documents');
    const { data: timeline, isLoading, error } = useTransactionTimeline(transactionId);

    if (isLoading) {
        return <LoadingState />;
    }

    if (error) {
        return (
            <ErrorState
                title={t('errorLoadingTimeline')}
                message={t('couldNotLoadTimeline')}
            />
        );
    }

    return (
        <Section>
            <h2 className="text-xl font-semibold mb-6">{t('timeline')}</h2>

            {(!timeline || timeline.length === 0) && (
                <p className="text-muted-foreground text-center py-8">
                    {t('noTimelineEvents')}
                </p>
            )}

            {timeline && timeline.length > 0 && (
                <div className="space-y-4">
                    {timeline.slice().reverse().map((entry, index) => {
                        const isDocumentEvent = entry.type.startsWith('DOCUMENT_');
                        return (
                            <div key={entry.id || index} className="flex gap-4">
                                {/* Icon and line */}
                                <div className="flex flex-col items-center">
                                    <div className="flex items-center justify-center w-10 h-10 rounded-full bg-background border-2 border-border">
                                        {getEventIcon(entry.type)}
                                    </div>
                                    {index < timeline.length - 1 && (
                                        <div className="w-0.5 h-full min-h-[40px] bg-border mt-2" />
                                    )}
                                </div>

                                {/* Content */}
                                <div className="flex-1 pb-6">
                                    <div className="flex items-start justify-between gap-4">
                                        <div className="flex-1">
                                            <div className="flex items-center gap-2 mb-1 flex-wrap">
                                                <h3 className="font-semibold">
                                                    {isDocumentEvent && entry.docType
                                                        ? `${getEventTypeLabel(entry.type, t, tDoc)} : ${tDoc(`types.${entry.docType}`)}`
                                                        : getEventTypeLabel(entry.type, t, tDoc)}
                                                </h3>
                                                {/* Badge always next to title for all types */}
                                                {(() => {
                                                    if (isDocumentEvent && entry.actorName) {
                                                        return (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                                                                {entry.type === 'DOCUMENT_SUBMITTED'
                                                                    ? t('timeline.submittedBy', { name: entry.actorName })
                                                                    : entry.type === 'DOCUMENT_REQUESTED'
                                                                    ? t('timeline.requestedBy', { name: entry.actorName })
                                                                    : entry.type === 'DOCUMENT_APPROVED'
                                                                    ? t('timeline.approvedBy', { name: entry.actorName })
                                                                    : entry.type === 'DOCUMENT_NEEDS_REVISION'
                                                                    ? t('timeline.revisionBy', { name: entry.actorName })
                                                                    : null}
                                                            </span>
                                                        );
                                                    } else if (entry.type === 'CREATED' && entry.transactionInfo?.actorName) {
                                                        return (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                                                                {t('timeline.by', { name: entry.transactionInfo.actorName })}
                                                            </span>
                                                        );
                                                    } else if (entry.type === 'STAGE_CHANGE' && entry.transactionInfo?.actorName) {
                                                        return (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                                                                {t('timeline.by', { name: entry.transactionInfo.actorName })}
                                                            </span>
                                                        );
                                                    }
                                                    return null;
                                                })()}
                                            </div>
                                            {/* Show document name and status for document events */}
                                            {isDocumentEvent && entry.docType && (
                                                <p className="text-sm text-muted-foreground mt-1">
                                                    {tDoc(`types.${entry.docType}`)}
                                                    {entry.status ? ` — ${tDoc(`status.${entry.status}`)}` : ''}
                                                </p>
                                            )}
                                                {/* Show note for NOTE or TRANSACTION_NOTE */}
                                                {(entry.type === 'NOTE' || entry.type === 'TRANSACTION_NOTE') && entry.note && (
                                                    <p className="text-sm text-muted-foreground mt-1">
                                                        {entry.note}
                                                    </p>
                                                )}
                                                {/* Show info for CREATED using transactionInfo and i18n */}
                                                {entry.type === 'CREATED' && entry.transactionInfo && (
                                                    <p className="text-sm text-muted-foreground mt-1">
                                                        {t('timeline.createdNote', {
                                                            clientName: entry.transactionInfo.clientName,
                                                            address: entry.transactionInfo.address
                                                        })}
                                                    </p>
                                                )}
                                                {/* Show info for STAGE_CHANGE using transactionInfo and i18n, always translate stage */}
                                                {entry.type === 'STAGE_CHANGE' && entry.transactionInfo && (
                                                    <p className="text-sm text-muted-foreground mt-1">
                                                        {t('timeline.stageChangeNoteFull', {
                                                            previousStage: entry.transactionInfo.previousStage ? t(`stages.${entry.transactionInfo.previousStage.toLowerCase()}`) : '',
                                                            newStage: entry.transactionInfo.newStage ? t(`stages.${entry.transactionInfo.newStage.toLowerCase()}`) : ''
                                                        })}
                                                    </p>
                                                )}
                                        </div>
                                        {entry.occurredAt && (
                                            <div className="text-right">
                                                <p className="text-xs text-muted-foreground whitespace-nowrap">
                                                    {formatDateTime(entry.occurredAt)}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>
                        );
                    })}
                </div>
            )}
        </Section>
    );
}
