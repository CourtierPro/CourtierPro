import { useTranslation } from 'react-i18next';
import { Section } from "@/shared/components/branded/Section";
import { StageBadge } from "@/shared/components/branded/StageBadge";
import { Button } from "@/shared/components/ui/button";
import { getStagesForSide, resolveStageIndex } from '@/shared/utils/stages';
import { type Transaction } from '@/features/transactions/api/queries';

interface TransactionStageTrackerProps {
    transaction: Transaction;
    onUpdateStage: () => void;
}

export function TransactionStageTracker({ transaction, onUpdateStage }: TransactionStageTrackerProps) {
    const { t } = useTranslation('transactions');
    const stages = getStagesForSide(transaction.side);
    const currentStageIndex = resolveStageIndex(transaction.currentStage, stages);

    return (
        <Section title={t('progress')} className="p-6">
            <div className="relative">
                <div className="absolute top-4 left-0 w-full h-0.5 bg-muted -z-10" />
                <div className="flex justify-between overflow-x-auto pb-4">
                    {stages.map((stage, index) => {
                        let status: "completed" | "current" | "upcoming" | "terminated" = "upcoming";
                        if (transaction.status === 'terminated') status = "terminated";
                        else if (index < currentStageIndex) status = "completed";
                        else if (index === currentStageIndex) status = "current";

                        return (
                            <StageBadge
                                key={stage}
                                stageNumber={index + 1}
                                label={t(`stages.${transaction.side === 'BUY_SIDE' ? 'buy' : 'sell'}.${stage.toLowerCase()}`)}
                                status={status}
                                className="min-w-[100px]"
                            />
                        );
                    })}
                </div>
                <div className="mt-6 flex justify-end">
                    <Button onClick={onUpdateStage}>
                        {t('updateStage')}
                    </Button>
                </div>
            </div>
        </Section>
    );
}
