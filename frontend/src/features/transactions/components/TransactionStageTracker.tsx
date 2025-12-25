import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { StageBadge } from "@/shared/components/branded/StageBadge";
import { Button } from "@/shared/components/ui/button";
import { getStagesForSide, resolveStageIndex } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';
import { ChevronDown, ChevronUp, Lock } from 'lucide-react';
import { Tooltip, TooltipContent, TooltipTrigger } from "@/shared/components/ui/tooltip";
import { Badge } from "@/shared/components/ui/badge";

interface TransactionStageTrackerProps {
    transaction: Transaction;
    onUpdateStage: () => void;
    isReadOnly?: boolean;
}

export function TransactionStageTracker({ transaction, onUpdateStage, isReadOnly = false }: TransactionStageTrackerProps) {
    const { t } = useTranslation('transactions');
    const [isExpanded, setIsExpanded] = useState(false);
    const stages = getStagesForSide(transaction.side);
    const currentStageIndex = resolveStageIndex(transaction.currentStage, stages);

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
            <div className="hidden md:block relative">
                <div className="absolute top-4 left-0 w-full h-0.5 bg-muted -z-10" />
                <div className="flex justify-between">
                    {stages.map((stage, index) => {
                        let status: "completed" | "current" | "upcoming" | "terminated" = "upcoming";
                        if (transaction.status === 'TERMINATED_EARLY') status = "terminated";
                        else if (transaction.status === 'CLOSED_SUCCESSFULLY') status = "completed"; // When closed successfully, all stages are treated as completed.
                        else if (index < currentStageIndex) status = "completed";
                        else if (index === currentStageIndex) status = "current";
                        return (
                            <Tooltip key={stage}>
                                <TooltipTrigger asChild>
                                    <div className="cursor-help"> {/* Wrapper to ensure trigger works if Badge doesn't forward ref or has issues */}
                                        <StageBadge
                                            stageNumber={index + 1}
                                            label={t(`stages.${transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}.${stage.toLowerCase()}`)}
                                            status={status}
                                            className="min-w-[100px]"
                                        />
                                    </div>
                                </TooltipTrigger>
                                <TooltipContent className="border-none max-w-[200px] text-center">
                                    <p>{
                                        transaction.side === 'BUY_SIDE'
                                            ? (t('buyStageDescriptions', { returnObjects: true }) as string[])?.[index]
                                            : (t('sellStageDescriptions', { returnObjects: true }) as string[])?.[index]
                                    }</p>
                                </TooltipContent>
                            </Tooltip>
                        );
                    })}}
                </div>
            </div>
            {/* Mobile View (Vertical Collapsible) */}
            <div className="md:hidden space-y-0">
                {stages.map((stage, index) => {
                    // Filter Logic: If collapsed, only show current stage.
                    if (!isExpanded && index !== currentStageIndex) return null;

                    let status: "completed" | "current" | "upcoming" | "terminated" = "upcoming";
                    if (transaction.status === 'TERMINATED_EARLY') status = "terminated";
                    else if (transaction.status === 'CLOSED_SUCCESSFULLY') status = "completed";
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
                                    <div className="w-0.5 min-h-[40px] flex-1 bg-muted my-1" />
                                )}
                            </div>
                            <div className="pt-1.5 pb-6">
                                <span className={`text-sm font-medium ${status === 'current' ? 'text-primary' : 'text-muted-foreground'}`}>
                                    {t(`stages.${transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}.${stage.toLowerCase()}`)}
                                </span>
                            </div>
                        </div>
                    );
                })}
            </div>

            {!isReadOnly && (
                <div className="mt-2 md:mt-6 flex justify-end">
                    {transaction.status === 'CLOSED_SUCCESSFULLY' ? (
                        <Badge variant="success" className="px-3 py-1.5 min-h-[36px] flex gap-2">
                            <Lock className="w-4 h-4" />
                            {t('transactionClosed') || "Transaction Closed"}
                        </Badge>
                    ) : transaction.status === 'TERMINATED_EARLY' ? (
                        <Badge variant="destructive" className="px-3 py-1.5 min-h-[36px] flex gap-2">
                            <Lock className="w-4 h-4" />
                            {t('transactionTerminated') || "Transaction Terminated"}
                        </Badge>
                    ) : (
                        <Button onClick={onUpdateStage} className="w-full sm:w-auto">
                            {t('updateStage')}
                        </Button>
                    )}
                </div>
            )}
        </Section>
    );
}
