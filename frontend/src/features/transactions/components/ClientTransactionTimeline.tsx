import { useClientTransactionTimeline } from '@/features/transactions/api/queries';
import { useTranslation } from 'react-i18next';
import { LoadingState } from '@/shared/components/branded/LoadingState';
import { ErrorState } from '@/shared/components/branded/ErrorState';
import { Section } from '@/shared/components/branded/Section';
import { formatDateTime } from '@/shared/utils/date';
import { getEventIcon } from './getEventIcon';
import { getEventTypeLabel } from './getEventTypeLabel';


interface ClientTransactionTimelineProps {
    transactionId: string;
}


export function ClientTransactionTimeline({ transactionId }: ClientTransactionTimelineProps) {
    const { t } = useTranslation('transactions');
    const { t: tDoc } = useTranslation('documents');
    const { data: timeline, isLoading, error } = useClientTransactionTimeline(transactionId);

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
                    {timeline.slice().reverse().map((entry, index) => (
                        <div key={entry.id || index} className="flex gap-4">
                            <div className="flex flex-col items-center">
                                <div className="flex items-center justify-center w-10 h-10 rounded-full bg-background border-2 border-border">
                                    {getEventIcon(entry.type)}
                                </div>
                                {index < timeline.length - 1 && (
                                    <div className="w-0.5 h-full min-h-[40px] bg-border mt-2" />
                                )}
                            </div>
                            <div className="flex-1 pb-6">
                                <div className="flex items-start justify-between gap-4">
                                    <div className="flex-1">
                                        <div className="flex items-center gap-2 mb-1">
                                            <h3 className="font-semibold">
                                                {entry.docType
                                                    ? `${getEventTypeLabel(entry.type, t)} : ${tDoc(`types.${entry.docType}`)}`
                                                    : getEventTypeLabel(entry.type, t)}
                                            </h3>
                                            {/* Subtitle for document events: 'Soumis par [nom]' or 'Demandé par [nom]' */}
                                            {entry.type.startsWith('DOCUMENT_') && entry.actorName && (
                                                <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground">
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
                                            )}
                                        </div>
                                        {/* Subtitle */}
                                        {entry.type.startsWith('DOCUMENT_') && entry.docType && (
                                            <p className="text-sm text-muted-foreground mt-1">
                                                {tDoc(`types.${entry.docType}`)}
                                                {entry.status ? ` — ${tDoc(`status.${entry.status}`)}` : ''}
                                            </p>
                                        )}
                                        {/* Affiche la note uniquement pour NOTE ou TRANSACTION_NOTE */}
                                        {(entry.type === 'NOTE' || entry.type === 'TRANSACTION_NOTE') && entry.note && (
                                            <p className="text-sm text-muted-foreground mt-1">
                                                {entry.note}
                                            </p>
                                        )}
                                        {/* Affiche le changement d'étape pour STAGE_CHANGE */}
                                        {entry.type === 'STAGE_CHANGE' && entry.transactionInfo && (() => {
                                            const translateStage = (stage: string | undefined) => {
                                                if (!stage) return '';
                                                const lowerStage = stage.toLowerCase();
                                                const sideKey = lowerStage.startsWith('seller') ? 'sell' : 'buy';
                                                return t(`stages.${sideKey}.${lowerStage}`, { defaultValue: stage });
                                            };
                                            return (
                                                <p className="text-sm text-muted-foreground mt-1">
                                                    {t('timeline.stageChangeNoteFull', {
                                                        previousStage: translateStage(entry.transactionInfo.previousStage),
                                                        newStage: translateStage(entry.transactionInfo.newStage)
                                                    })}
                                                </p>
                                            );
                                        })()}
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
                    ))}
                </div>
            )}
        </Section>
    );
}
