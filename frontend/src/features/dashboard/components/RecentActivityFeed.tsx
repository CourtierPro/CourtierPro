import { useState } from "react";
import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { Badge } from "@/shared/components/ui/badge";
import { Checkbox } from "@/shared/components/ui/checkbox";
import {
    Activity,
    ChevronLeft,
    ChevronRight,
    Eye,
    CheckCheck,
} from "lucide-react";
import { useRecentActivity, type RecentActivity } from "@/features/dashboard/api/queries";
import { useMarkActivitiesAsSeen } from "@/features/dashboard/api/mutations";
import { formatDateTime } from "@/shared/utils/date";
import { getEventIcon } from "@/features/transactions/components/getEventIcon";
import { getEventTypeLabel } from "@/features/transactions/components/getEventTypeLabel";

interface RecentActivityFeedProps {
    className?: string;
}

export function RecentActivityFeed({ className }: RecentActivityFeedProps) {
    const { t } = useTranslation("dashboard");
    const { t: tTx } = useTranslation("transactions");
    const { t: tDoc } = useTranslation("documents");
    const navigate = useNavigate();
    const [page, setPage] = useState(0);
    const pageSize = 5;
    const [selectedIds, setSelectedIds] = useState<Set<string>>(new Set());

    const { data, isLoading, error } = useRecentActivity(page, pageSize);
    const markAsSeenMutation = useMarkActivitiesAsSeen();

    const handleActivityClick = async (activity: RecentActivity) => {
        // Mark as seen before navigating
        if (!activity.seen) {
            await markAsSeenMutation.mutateAsync([activity.activityId]);
        }
        if (activity.transactionId) {
            navigate(`/transactions/${activity.transactionId}`);
        }
    };

    const handleCheckboxChange = (activityId: string, checked: boolean) => {
        setSelectedIds(prev => {
            const next = new Set(prev);
            if (checked) {
                next.add(activityId);
            } else {
                next.delete(activityId);
            }
            return next;
        });
    };

    const handleMarkSelectedAsSeen = async () => {
        if (selectedIds.size === 0) return;
        await markAsSeenMutation.mutateAsync(Array.from(selectedIds));
        setSelectedIds(new Set());
    };

    const handleMarkAllAsSeen = async () => {
        const activities = data?.content ?? [];
        const unseenIds = activities.filter(a => !a.seen).map(a => a.activityId);
        if (unseenIds.length === 0) return;
        await markAsSeenMutation.mutateAsync(unseenIds);
        setSelectedIds(new Set());
    };

    const handleSelectAll = () => {
        const activities = data?.content ?? [];
        const unseenIds = activities.filter(a => !a.seen).map(a => a.activityId);
        setSelectedIds(new Set(unseenIds));
    };

    const handleDeselectAll = () => {
        setSelectedIds(new Set());
    };

    const translateStage = (stage: string | undefined, side: string) => {
        if (!stage) return '';
        const lowerStage = stage.toLowerCase();
        const sideKey = side === 'BUY_SIDE' ? 'buy' : 'sell';
        return tTx(`stages.${sideKey}.${lowerStage}.name`, { defaultValue: stage });
    };

    const translateSide = (side: string) => {
        if (side === 'BUY_SIDE') return tTx('buy');
        if (side === 'SELL_SIDE') return tTx('sell');
        return '';
    };

    const formatCurrency = (amount?: number) => {
        if (!amount) return '';
        return new Intl.NumberFormat('en-CA', {
            style: 'currency',
            currency: 'CAD',
            maximumFractionDigits: 0,
        }).format(amount);
    };

    const renderEventDetails = (activity: RecentActivity) => {
        const isDocumentEvent = activity.type.startsWith('DOCUMENT_');
        const isPropertyEvent = activity.type.startsWith('PROPERTY_') && !activity.type.startsWith('PROPERTY_OFFER_');
        const isPropertyOfferEvent = activity.type.startsWith('PROPERTY_OFFER_');
        const isOfferEvent = activity.type.startsWith('OFFER_') && !activity.type.startsWith('PROPERTY_OFFER_');
        const isConditionEvent = activity.type.startsWith('CONDITION_');

        // Document events
        if (isDocumentEvent && activity.docType) {
            if (String(activity.type) === 'DOCUMENT_REQUEST_UPDATED' && activity.note?.startsWith('document.details.updated.note|')) {
                const parts = activity.note.split('|');
                const brokerName = parts[2] || '';
                return (
                    <p className="text-sm text-muted-foreground mt-1">
                        {tTx('timeline.documentDetailsUpdatedNote', { brokerName })}
                    </p>
                );
            }
            return (
                <p className="text-sm text-muted-foreground mt-1">
                    {tDoc(`types.${activity.docType}`)}
                </p>
            );
        }

        // Note events
        if ((activity.type === 'NOTE' || activity.type === 'TRANSACTION_NOTE') && activity.note) {
            return (
                <p className="text-sm text-muted-foreground mt-1">
                    {activity.note}
                </p>
            );
        }

        // Created event
        if (activity.type === 'CREATED' && activity.transactionInfo) {
            const address = activity.transactionInfo.address;
            return (
                <p className="text-sm text-muted-foreground mt-1">
                    {address && address.trim() !== ''
                        ? tTx('timeline.createdNote', {
                            clientName: activity.transactionInfo.clientName,
                            address
                        })
                        : tTx('timeline.createdNoteNoAddress', {
                            clientName: activity.transactionInfo.clientName
                        })
                    }
                </p>
            );
        }

        // Stage change
        if (activity.type === 'STAGE_CHANGE' && activity.transactionInfo) {
            return (
                <p className="text-sm text-muted-foreground mt-1">
                    {tTx('timeline.stageChangeNoteFull', {
                        previousStage: translateStage(activity.transactionInfo.previousStage, activity.side),
                        newStage: translateStage(activity.transactionInfo.newStage, activity.side)
                    })}
                </p>
            );
        }

        // Property events
        if (isPropertyEvent && activity.transactionInfo) {
            const address = activity.transactionInfo.address || tTx('unknownAddress');
            if (activity.type === 'PROPERTY_ADDED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.propertyAdded', { address })}</p>;
            if (activity.type === 'PROPERTY_UPDATED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.propertyUpdated', { address })}</p>;
            if (activity.type === 'PROPERTY_REMOVED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.propertyRemoved', { address })}</p>;
        }

        // Property offer events
        if (isPropertyOfferEvent && activity.transactionInfo) {
            const { address, offerAmount, previousOfferStatus, offerStatus } = activity.transactionInfo;
            if (activity.type === 'PROPERTY_OFFER_MADE') {
                return (
                    <p className="text-sm text-muted-foreground mt-1">
                        {tTx('timeline.propertyOfferMade', {
                            address: address || tTx('unknownAddress'),
                            amount: formatCurrency(offerAmount),
                        })}
                    </p>
                );
            }
            if (activity.type === 'PROPERTY_OFFER_UPDATED') {
                if (previousOfferStatus && offerStatus) {
                    const fromStatus = tTx(`buyerOfferStatuses.${previousOfferStatus}`, { defaultValue: previousOfferStatus });
                    const toStatus = tTx(`buyerOfferStatuses.${offerStatus}`, { defaultValue: offerStatus });
                    return (
                        <p className="text-sm text-muted-foreground mt-1">
                            {tTx('timeline.propertyOfferStatusChanged', {
                                address: address || tTx('unknownAddress'),
                                fromStatus,
                                toStatus,
                            })}
                        </p>
                    );
                }
                return (
                    <p className="text-sm text-muted-foreground mt-1">
                        {tTx('timeline.propertyOfferUpdated', {
                            address: address || tTx('unknownAddress'),
                            amount: formatCurrency(offerAmount),
                        })}
                    </p>
                );
            }
        }

        // Offer events (sell-side received offers)
        if (isOfferEvent && activity.transactionInfo) {
            const { buyerName, offerAmount, offerStatus } = activity.transactionInfo;
            if (activity.type === 'OFFER_RECEIVED') {
                return (
                    <p className="text-sm text-muted-foreground mt-1">
                        {tTx('timeline.offerReceivedDetail', {
                            buyerName: buyerName || '',
                            amount: formatCurrency(offerAmount),
                        })}
                    </p>
                );
            }
            if (activity.type === 'OFFER_UPDATED') {
                const translatedStatus = offerStatus ? tTx(`receivedOfferStatuses.${offerStatus}`) : '';
                return (
                    <p className="text-sm text-muted-foreground mt-1">
                        {tTx('timeline.offerUpdatedDetail', {
                            buyerName: buyerName || '',
                            status: translatedStatus,
                        })}
                    </p>
                );
            }
            if (activity.type === 'OFFER_REMOVED') {
                return (
                    <p className="text-sm text-muted-foreground mt-1">
                        {tTx('timeline.offerRemovedDetail', { buyerName: buyerName || '' })}
                    </p>
                );
            }
        }

        // Condition events
        if (isConditionEvent) {
            const getConditionType = () => {
                const typeFromMetadata = activity.transactionInfo?.conditionType;
                const customTitle = activity.transactionInfo?.conditionCustomTitle;
                if (typeFromMetadata === 'OTHER' && customTitle) {
                    return customTitle;
                }
                if (typeFromMetadata) {
                    return tTx(`conditionTypes.${typeFromMetadata}`, { defaultValue: typeFromMetadata });
                }
                return tTx('conditions.unknown', { defaultValue: 'Unknown' });
            };
            const conditionType = getConditionType();

            if (activity.type === 'CONDITION_ADDED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.conditionAdded', { conditionType })}</p>;
            if (activity.type === 'CONDITION_UPDATED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.conditionUpdated', { conditionType })}</p>;
            if (activity.type === 'CONDITION_REMOVED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.conditionRemoved', { conditionType })}</p>;
            if (activity.type === 'CONDITION_SATISFIED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.conditionSatisfied', { conditionType })}</p>;
            if (activity.type === 'CONDITION_FAILED') return <p className="text-sm text-muted-foreground mt-1">{tTx('timeline.conditionFailed', { conditionType })}</p>;
        }

        return null;
    };

    const renderActorBadge = (activity: RecentActivity) => {
        const isDocumentEvent = activity.type.startsWith('DOCUMENT_');

        if (isDocumentEvent && activity.actorName) {
            let label = '';
            if (activity.type === 'DOCUMENT_SUBMITTED') label = tTx('timeline.submittedBy', { name: activity.actorName });
            else if (activity.type === 'DOCUMENT_REQUESTED') label = tTx('timeline.requestedBy', { name: activity.actorName });
            else if (activity.type === 'DOCUMENT_APPROVED') label = tTx('timeline.approvedBy', { name: activity.actorName });
            else if (activity.type === 'DOCUMENT_NEEDS_REVISION') label = tTx('timeline.revisionBy', { name: activity.actorName });

            if (label) {
                return (
                    <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                        {label}
                    </span>
                );
            }
        }

        // For other event types with actorName
        if (['CREATED', 'STAGE_CHANGE'].includes(activity.type) && activity.transactionInfo?.actorName) {
            return (
                <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                    {tTx('timeline.by', { name: activity.transactionInfo.actorName })}
                </span>
            );
        }

        if (activity.type.startsWith('PROPERTY_') && activity.transactionInfo?.actorName) {
            return (
                <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                    {tTx('timeline.by', { name: activity.transactionInfo.actorName })}
                </span>
            );
        }

        if (activity.type.startsWith('CONDITION_') && activity.actorName) {
            return (
                <span className="text-xs px-2 py-0.5 rounded-full bg-muted text-muted-foreground ml-2">
                    {tTx('timeline.by', { name: activity.actorName })}
                </span>
            );
        }

        return null;
    };

    if (isLoading) {
        return (
            <Section
                title={t("broker.activityFeed.title")}
                className={className}
            >
                <div className="space-y-4">
                    {[1, 2, 3, 4].map((i) => (
                        <div key={i} className="flex gap-4">
                            <Skeleton className="h-10 w-10 rounded-full" />
                            <div className="flex-1 space-y-2">
                                <Skeleton className="h-4 w-3/4" />
                                <Skeleton className="h-3 w-1/2" />
                            </div>
                        </div>
                    ))}
                </div>
            </Section>
        );
    }

    if (error) {
        return (
            <Section
                title={t("broker.activityFeed.title")}
                className={className}
            >
                <div className="text-sm text-destructive">
                    {t("broker.priorityCards.error")}
                </div>
            </Section>
        );
    }

    const activities = data?.content ?? [];
    const totalPages = data?.totalPages ?? 0;
    const isFirst = data?.first ?? true;
    const isLast = data?.last ?? true;
    const unseenCount = activities.filter(a => !a.seen).length;
    const hasSelection = selectedIds.size > 0;

    return (
        <Section
            title={t("broker.activityFeed.title")}
            description={t("broker.activityFeed.description")}
            className={className}
        >
            {activities.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                    <Activity className="h-10 w-10 text-muted-foreground/50 mb-2" />
                    <p className="text-sm text-muted-foreground">
                        {t("broker.activityFeed.empty")}
                    </p>
                </div>
            ) : (
                <>
                    {/* Action toolbar */}
                    {unseenCount > 0 && (
                        <div className="flex items-center gap-2 mb-4 pb-4 border-b">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={hasSelection ? handleDeselectAll : handleSelectAll}
                            >
                                {hasSelection ? t("broker.activityFeed.deselectAll") : t("broker.activityFeed.selectAll")}
                            </Button>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={handleMarkSelectedAsSeen}
                                disabled={!hasSelection || markAsSeenMutation.isPending}
                            >
                                <Eye className="h-4 w-4 mr-1" />
                                {t("broker.activityFeed.markAsSeen")}
                                {hasSelection && ` (${selectedIds.size})`}
                            </Button>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={handleMarkAllAsSeen}
                                disabled={markAsSeenMutation.isPending}
                            >
                                <CheckCheck className="h-4 w-4 mr-1" />
                                {t("broker.activityFeed.markAllAsSeen")}
                            </Button>
                        </div>
                    )}

                    <div className="space-y-6">
                        {activities.map((activity, index) => {
                            const isDocumentEvent = activity.type.startsWith('DOCUMENT_');
                            const isUnseen = !activity.seen;
                            const isSelected = selectedIds.has(activity.activityId);

                            return (
                                <div
                                    key={activity.activityId}
                                    className={`flex gap-4 rounded-lg transition-colors ${isUnseen
                                        ? 'bg-primary/5 border-l-4 border-l-primary'
                                        : ''
                                        }`}
                                >
                                    {/* Checkbox for unseen items */}
                                    {isUnseen && (
                                        <div
                                            className="flex items-start pt-3 pl-2"
                                            onClick={(e) => e.stopPropagation()}
                                        >
                                            <Checkbox
                                                checked={isSelected}
                                                onCheckedChange={(checked) =>
                                                    handleCheckboxChange(activity.activityId, checked as boolean)
                                                }
                                            />
                                        </div>
                                    )}

                                    <button
                                        onClick={() => handleActivityClick(activity)}
                                        className={`flex-1 flex gap-4 text-left focus:outline-none focus:ring-2 focus:ring-primary rounded-lg hover:bg-muted/30 transition-colors p-2 ${isUnseen ? '-ml-2' : ''}`}
                                    >
                                        {/* Icon and line */}
                                        <div className="flex flex-col items-center">
                                            <div className={`flex items-center justify-center w-10 h-10 rounded-full bg-background border-2 ${isUnseen ? 'border-primary' : 'border-border'
                                                }`}>
                                                {getEventIcon(activity.type as Parameters<typeof getEventIcon>[0])}
                                            </div>
                                            {index < activities.length - 1 && (
                                                <div className="w-0.5 h-full min-h-[60px] bg-border mt-2" />
                                            )}
                                        </div>

                                        {/* Content */}
                                        <div className="flex-1 pb-8">
                                            <div className="flex items-start justify-between gap-4">
                                                <div className="flex-1">
                                                    {/* Event type title with actor badge */}
                                                    <div className="flex items-center gap-2 mb-3 flex-wrap">
                                                        <h3 className={`font-semibold ${isUnseen ? 'text-primary' : ''}`}>
                                                            {isDocumentEvent && activity.docType && String(activity.type) === 'DOCUMENT_REQUEST_UPDATED'
                                                                ? `${tDoc('requestDocument')} : ${tDoc('edited')}`
                                                                : isDocumentEvent && activity.docType
                                                                    ? `${getEventTypeLabel(activity.type as Parameters<typeof getEventTypeLabel>[0], tTx)} : ${tDoc(`types.${activity.docType}`)}`
                                                                    : getEventTypeLabel(activity.type as Parameters<typeof getEventTypeLabel>[0], tTx)}
                                                        </h3>
                                                        {renderActorBadge(activity)}
                                                        {isUnseen && (
                                                            <Badge variant="default" className="text-xs py-0 h-5">
                                                                {t("broker.activityFeed.new")}
                                                            </Badge>
                                                        )}
                                                    </div>

                                                    {/* Transaction context row */}
                                                    <div className="flex items-center gap-2 flex-wrap text-xs text-muted-foreground mb-3">
                                                        {activity.clientName && (
                                                            <span className="font-medium">{activity.clientName}</span>
                                                        )}
                                                        {activity.clientName && activity.propertyAddress && (
                                                            <span>•</span>
                                                        )}
                                                        {activity.propertyAddress && (
                                                            <span>
                                                                {activity.propertyAddress === "No property selected"
                                                                    ? tTx('noPropertySelected')
                                                                    : activity.propertyAddress}
                                                            </span>
                                                        )}
                                                        {(activity.clientName || activity.propertyAddress) && activity.side && (
                                                            <span>•</span>
                                                        )}
                                                        {activity.side && (
                                                            <Badge variant="outline" className="text-xs py-0 h-5">
                                                                {translateSide(activity.side)}
                                                            </Badge>
                                                        )}
                                                        {activity.currentStage && (
                                                            <>
                                                                <span>•</span>
                                                                <Badge variant="secondary" className="text-xs py-0 h-5">
                                                                    {translateStage(activity.currentStage, activity.side)}
                                                                </Badge>
                                                            </>
                                                        )}
                                                    </div>

                                                    {/* Event-specific details */}
                                                    {renderEventDetails(activity)}
                                                </div>
                                                {activity.occurredAt && (
                                                    <div className="text-right">
                                                        <p className="text-xs text-muted-foreground whitespace-nowrap">
                                                            {formatDateTime(activity.occurredAt)}
                                                        </p>
                                                    </div>
                                                )}
                                            </div>
                                        </div>
                                    </button>
                                </div>
                            );
                        })}
                    </div>

                    {/* Pagination controls */}
                    {totalPages > 1 && (
                        <div className="flex items-center justify-between mt-6 pt-4 border-t">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setPage(p => Math.max(0, p - 1))}
                                disabled={isFirst}
                            >
                                <ChevronLeft className="h-4 w-4 mr-1" />
                                {t("broker.activityFeed.previous")}
                            </Button>
                            <span className="text-sm text-muted-foreground">
                                {t("broker.activityFeed.pageOf", { current: page + 1, total: totalPages })}
                            </span>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setPage(p => p + 1)}
                                disabled={isLast}
                            >
                                {t("broker.activityFeed.next")}
                                <ChevronRight className="h-4 w-4 ml-1" />
                            </Button>
                        </div>
                    )}
                </>
            )}
        </Section>
    );
}
