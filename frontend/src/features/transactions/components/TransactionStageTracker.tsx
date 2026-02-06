import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { PermissionDenied } from "@/shared/components/branded/PermissionDenied";
import { Section } from "@/shared/components/branded/Section";
import { StageBadge } from "@/shared/components/branded/StageBadge";
import { Button } from "@/shared/components/ui/button";
import { getStagesForSide, resolveStageIndex, getStageGoal } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';
import { ChevronDown, ChevronUp, Lock } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipTrigger } from "@/shared/components/ui/tooltip";
import { Badge } from "@/shared/components/ui/badge";
import { useParticipantPermissions } from '@/features/transactions/hooks/useParticipantPermissions';
import { cn } from '@/shared/utils/utils';

interface TransactionStageTrackerProps {
    transaction: Transaction;
    onUpdateStage: () => void;
    onTerminate: () => void;
    isReadOnly?: boolean;
}

export function TransactionStageTracker({ transaction, onUpdateStage, onTerminate, isReadOnly = false }: TransactionStageTrackerProps) {
    const { t } = useTranslation('transactions');
    const [isExpanded, setIsExpanded] = useState(false);
    const stages = getStagesForSide(transaction.side);
    const currentStageIndex = resolveStageIndex(transaction.currentStage, stages);
    const isTerminated = transaction.status === 'TERMINATED_EARLY';
    const isClosed = transaction.status === 'CLOSED_SUCCESSFULLY';

    const getConnectorClass = (index: number) => {
        if (isTerminated) return "bg-muted";
        if (isClosed || index < currentStageIndex) return "bg-primary";
        return "bg-muted";
    };

    const { checkPermission } = useParticipantPermissions(transaction.transactionId);
    const canViewStage = checkPermission('VIEW_STAGE');

    if (!canViewStage) {
        return (
            <Section title={t('progress')} className="p-4 md:p-6">
                <PermissionDenied message={t('noPermissionViewStage')} />
            </Section>
        );
    }

    return (
        <Section title={t('progress')} className="p-4 md:p-6"
            action={
                <Button
                    variant="ghost"
                    size="sm"
                    className="md:hidden h-6 w-6 p-0"
                    onClick={() => setIsExpanded(!isExpanded)}
                >
                    {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                </Button>
            }
        >
            {/* Desktop View (Horizontal) */}
            <div className={cn("hidden md:block relative", isTerminated && "opacity-60")}>
                {isTerminated && (
                    <div className="absolute top-4 left-0 w-full h-0.5 bg-destructive z-10" />
                )}
                <div className="flex items-start">
                    {stages.map((stage, index) => {
                        let status: "completed" | "current" | "upcoming" | "terminated" = "upcoming";
                        if (isClosed) status = "completed"; // When closed successfully, all stages are treated as completed.
                        else if (index < currentStageIndex) status = "completed";
                        else if (index === currentStageIndex) status = "current";

                        return (
                            <div
                                key={stage}
                                className={cn(
                                    "flex items-start min-w-0",
                                    index < stages.length - 1 ? "flex-1" : "shrink-0"
                                )}
                            >
                                <Tooltip>
                                    <TooltipTrigger asChild>
                                        <div className="cursor-help"> {/* Wrapper to ensure trigger works if Badge doesn't forward ref or has issues */}
                                            <StageBadge
                                                stageNumber={index + 1}
                                                label={t(`stages.${transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}.${stage.toLowerCase()}.name`)}
                                                status={status}
                                                className="min-w-[100px]"
                                            />
                                        </div>
                                    </TooltipTrigger>
                                    <TooltipContent className="border-none max-w-[200px] text-center">
                                        <p>{getStageGoal(stage, t, transaction.side)}</p>
                                    </TooltipContent>
                                </Tooltip>

                                {index < stages.length - 1 && (
                                    <div className={cn("h-0.5 flex-1 mt-4 mx-2 rounded", getConnectorClass(index))} />
                                )}
                            </div>
                        );
                    })}
                </div>
            </div>
            {/* Mobile View (Vertical Collapsible) */}
            <div className={cn("md:hidden space-y-0", isTerminated && "opacity-60")}>
                {stages.map((stage, index) => {
                    // Filter Logic: If collapsed, only show current stage.
                    if (!isExpanded && index !== currentStageIndex) return null;

                    let status: "completed" | "current" | "upcoming" | "terminated" = "upcoming";
                    if (isClosed) status = "completed";
                    else if (index < currentStageIndex) status = "completed";
                    else if (index === currentStageIndex) status = "current";

                    const isLast = index === stages.length - 1;
                    // In collapsed mode, we don't show the connecting line ever
                    const showLine = isExpanded && !isLast;

                    return (
                        <div key={stage} className="flex gap-4">
                            <div className="flex flex-col items-center">
                                <StageBadge
                                    stageNumber={index + 1}
                                    label=""
                                    status={status}
                                    className="z-10 bg-background"
                                />
                                {showLine && (
                                    <div className={cn("w-0.5 min-h-[40px] flex-1 my-1", getConnectorClass(index))} />
                                )}
                            </div>
                            <div className="pt-1.5 pb-6">
                                <span className={`text-sm font-medium ${status === 'current' ? 'text-primary' : 'text-muted-foreground'}`}>
                                    {t(`stages.${transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}.${stage.toLowerCase()}.name`)}
                                </span>
                            </div>
                        </div>
                    );
                })}
            </div>

            {isTerminated && (
                <div className="mt-2 flex justify-center">
                    <Badge variant="destructive" className="px-3 py-1">
                        {t('transactionTerminated') || "Transaction Terminated"}
                    </Badge>
                </div>
            )}

            {!isReadOnly && (
                <div className="mt-2 md:mt-6 flex justify-end">
                    {isClosed ? (
                        <Badge variant="success" className="px-3 py-1.5 min-h-[36px] flex gap-2">
                            <Lock className="w-4 h-4" />
                            {t('transactionClosed') || "Transaction Closed"}
                        </Badge>
                    ) : !isTerminated && checkPermission('EDIT_STAGE') && (
                        <div className="flex gap-2 w-full sm:w-auto">
                            <Button variant="destructive" onClick={onTerminate} className="flex-1 sm:flex-none">
                                {t('terminateTransaction')}
                            </Button>
                            <Button onClick={onUpdateStage} className="flex-1 sm:flex-none">
                                {t('updateStage')}
                            </Button>
                        </div>
                    )}
                </div>
            )}
        </Section>
    );
}
