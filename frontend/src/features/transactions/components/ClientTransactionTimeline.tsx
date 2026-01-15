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
                                                        ? `${getEventTypeLabel(entry.type, t)} : ${tDoc(`types.${entry.docType}`)}`
                                                        : getEventTypeLabel(entry.type, t)}
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
                                                    } else if (entry.type.startsWith('PROPERTY_') && entry.transactionInfo?.actorName) {
                                                        return (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                                                                {t('timeline.by', { name: entry.transactionInfo.actorName })}
                                                            </span>
                                                        );
                                                    } else if (entry.type.startsWith('OFFER_') && entry.transactionInfo?.actorName) {
                                                        return (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                                                                {t('timeline.by', { name: entry.transactionInfo.actorName })}
                                                            </span>
                                                        );
                                                    } else if (entry.type.startsWith('CONDITION_') && entry.actorName) {
                                                        return (
                                                            <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                                                                {t('timeline.by', { name: entry.actorName })}
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
                                                    {entry.transactionInfo.address && entry.transactionInfo.address.trim() !== '' 
                                                        ? t('timeline.createdNote', {
                                                            clientName: entry.transactionInfo.clientName,
                                                            address: entry.transactionInfo.address
                                                        })
                                                        : t('timeline.createdNoteNoAddress', {
                                                            clientName: entry.transactionInfo.clientName
                                                        })
                                                    }
                                                </p>
                                            )}
                                            {/* Show info for STAGE_CHANGE using transactionInfo and i18n, always translate stage */}
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
                                            {/* Show info for PROPERTY events */}
                                            {entry.type.startsWith('PROPERTY_') && entry.transactionInfo && (
                                                <p className="text-sm text-muted-foreground mt-1">
                                                    {(() => {
                                                        const address = entry.transactionInfo.address || 'Unknown Address';
                                                        if (entry.type === 'PROPERTY_ADDED') return t('timeline.propertyAdded', { address });
                                                        if (entry.type === 'PROPERTY_UPDATED') return t('timeline.propertyUpdated', { address });
                                                        if (entry.type === 'PROPERTY_REMOVED') return t('timeline.propertyRemoved', { address });
                                                        return '';
                                                    })()}
                                                </p>
                                            )}
                                            {/* Show info for PROPERTY_OFFER events */}
                                            {entry.type.startsWith('PROPERTY_OFFER_') && entry.transactionInfo && (
                                                <p className="text-sm text-muted-foreground mt-1">
                                                    {(() => {
                                                        const { address, offerAmount } = entry.transactionInfo;
                                                        const formatCurrency = (amount?: number) => {
                                                            if (!amount) return '';
                                                            return new Intl.NumberFormat('en-CA', {
                                                                style: 'currency',
                                                                currency: 'CAD',
                                                                maximumFractionDigits: 0,
                                                            }).format(amount);
                                                        };
                                                        if (entry.type === 'PROPERTY_OFFER_MADE') {
                                                            return t('timeline.propertyOfferMade', {
                                                                address: address || t('unknownAddress'),
                                                                amount: formatCurrency(offerAmount),
                                                            });
                                                        }
                                                        if (entry.type === 'PROPERTY_OFFER_UPDATED') {
                                                            return t('timeline.propertyOfferUpdated', {
                                                                address: address || t('unknownAddress'),
                                                                amount: formatCurrency(offerAmount),
                                                            });
                                                        }
                                                        return '';
                                                    })()}
                                                </p>
                                            )}
                                            {/* Show note for OFFER events - use transactionInfo for translation */}
                                            {entry.type.startsWith('OFFER_') && entry.transactionInfo && (
                                                <p className="text-sm text-muted-foreground mt-1">
                                                    {(() => {
                                                        const { buyerName, offerAmount, offerStatus } = entry.transactionInfo;
                                                        const formatCurrency = (amount?: number) => {
                                                            if (!amount) return '';
                                                            return new Intl.NumberFormat('en-CA', {
                                                                style: 'currency',
                                                                currency: 'CAD',
                                                                maximumFractionDigits: 0,
                                                            }).format(amount);
                                                        };
                                                        if (entry.type === 'OFFER_RECEIVED') {
                                                            return t('timeline.offerReceivedDetail', {
                                                                buyerName: buyerName || '',
                                                                amount: formatCurrency(offerAmount),
                                                            });
                                                        }
                                                        if (entry.type === 'OFFER_UPDATED') {
                                                            const translatedStatus = offerStatus ? t(`receivedOfferStatuses.${offerStatus}`) : '';
                                                            return t('timeline.offerUpdatedDetail', {
                                                                buyerName: buyerName || '',
                                                                status: translatedStatus,
                                                            });
                                                        }
                                                        if (entry.type === 'OFFER_REMOVED') {
                                                            return t('timeline.offerRemovedDetail', { buyerName: buyerName || '' });
                                                        }
                                                        return '';
                                                    })()}
                                                </p>
                                            )}
                                            {/* Show translated info for CONDITION events */}
                                            {entry.type.startsWith('CONDITION_') && (
                                                <p className="text-sm text-muted-foreground mt-1">
                                                    {(() => {
                                                        // Get condition type from metadata, fallback to parsing from note for legacy entries
                                                        const getConditionType = () => {
                                                            // If type is OTHER and custom title exists, use custom title
                                                            const typeFromMetadata = entry.transactionInfo?.conditionType;
                                                            const customTitle = entry.transactionInfo?.conditionCustomTitle;
                                                            if (typeFromMetadata === 'OTHER' && customTitle) {
                                                                return customTitle;
                                                            }
                                                            if (typeFromMetadata) {
                                                                return t(`conditionTypes.${typeFromMetadata}`, { defaultValue: typeFromMetadata });
                                                            }
                                                            // Fallback: extract from note for legacy entries
                                                            const note = entry.note || '';
                                                            const match = note.match(/Condition\s+(?:added:|updated:|removed:)?\s*(\w+)/i);
                                                            if (match) {
                                                                const type = match[1].toUpperCase();
                                                                return t(`conditionTypes.${type}`, { defaultValue: type });
                                                            }
                                                            return t('conditions.unknown', { defaultValue: 'Unknown' });
                                                        };
                                                        const conditionType = getConditionType();
                                                        if (entry.type === 'CONDITION_ADDED') return t('timeline.conditionAdded', { conditionType });
                                                        if (entry.type === 'CONDITION_UPDATED') return t('timeline.conditionUpdated', { conditionType });
                                                        if (entry.type === 'CONDITION_REMOVED') return t('timeline.conditionRemoved', { conditionType });
                                                        if (entry.type === 'CONDITION_SATISFIED') return t('timeline.conditionSatisfied', { conditionType });
                                                        if (entry.type === 'CONDITION_FAILED') return t('timeline.conditionFailed', { conditionType });
                                                        return entry.note || '';
                                                    })()}
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
