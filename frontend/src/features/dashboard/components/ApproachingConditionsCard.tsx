import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { Section } from "@/shared/components/branded/Section";
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { Skeleton } from "@/shared/components/ui/skeleton";
import { AlertTriangle, ArrowRight, FileWarning } from "lucide-react";
import { useApproachingConditions, type ApproachingCondition } from "@/features/dashboard/api/queries";
import { cn } from "@/shared/utils/utils";

interface ApproachingConditionsCardProps {
    className?: string;
    maxItems?: number;
}

export function ApproachingConditionsCard({ className, maxItems = 5 }: ApproachingConditionsCardProps) {
    const { t } = useTranslation("dashboard");
    const { t: tTx } = useTranslation("transactions");
    const navigate = useNavigate();
    const { data: conditions, isLoading, error } = useApproachingConditions();

    const displayConditions = conditions?.slice(0, maxItems) ?? [];

    const getUrgencyColor = (days: number) => {
        if (days <= 1) return "destructive";
        if (days <= 3) return "warning";
        return "secondary";
    };

    const getConditionTitle = (condition: ApproachingCondition) => {
        if (condition.customTitle) {
            return condition.customTitle;
        }
        return tTx(`conditionTypes.${condition.conditionType}`, { defaultValue: condition.conditionType });
    };

    const handleConditionClick = (condition: ApproachingCondition) => {
        navigate(`/transactions/${condition.transactionId}?tab=conditions`);
    };

    if (isLoading) {
        return (
            <Section
                title={t("broker.priorityCards.approachingConditions.title")}
                className={className}
            >
                <div className="space-y-3">
                    {[1, 2, 3].map((i) => (
                        <Skeleton key={i} className="h-16 w-full" />
                    ))}
                </div>
            </Section>
        );
    }

    if (error) {
        return (
            <Section
                title={t("broker.priorityCards.approachingConditions.title")}
                className={className}
            >
                <div className="text-sm text-destructive">
                    {t("broker.priorityCards.error")}
                </div>
            </Section>
        );
    }

    return (
        <Section
            title={t("broker.priorityCards.approachingConditions.title")}
            description={t("broker.priorityCards.approachingConditions.description")}
            className={className}
            action={
                conditions && conditions.length > maxItems && (
                    <Button variant="ghost" size="sm" onClick={() => navigate("/transactions")}>
                        {t("broker.priorityCards.viewAll")}
                        <ArrowRight className="ml-1 h-4 w-4" />
                    </Button>
                )
            }
        >
            {displayConditions.length === 0 ? (
                <div className="flex flex-col items-center justify-center py-8 text-center">
                    <FileWarning className="h-10 w-10 text-muted-foreground/50 mb-2" />
                    <p className="text-sm text-muted-foreground">
                        {t("broker.priorityCards.approachingConditions.empty")}
                    </p>
                </div>
            ) : (
                <div className="space-y-2">
                    {displayConditions.map((condition) => (
                        <button
                            key={condition.conditionId}
                            onClick={() => handleConditionClick(condition)}
                            className={cn(
                                "w-full flex items-center justify-between p-3 rounded-lg",
                                "bg-muted/50 hover:bg-muted transition-colors",
                                "text-left focus:outline-none focus:ring-2 focus:ring-primary"
                            )}
                        >
                            <div className="flex-1 min-w-0">
                                <div className="flex items-center gap-2 mb-1">
                                    {condition.daysUntilDeadline <= 1 && (
                                        <AlertTriangle className="h-4 w-4 text-destructive flex-shrink-0" />
                                    )}
                                    <span className="font-medium text-sm truncate">
                                        {getConditionTitle(condition)}
                                    </span>
                                </div>
                                <div className="flex items-center gap-2 text-xs text-muted-foreground">
                                    <span>{condition.clientName}</span>
                                    <span>•</span>
                                    <span className="truncate">
                                        {condition.propertyAddress || t("broker.priorityCards.noAddress")}
                                    </span>
                                </div>
                            </div>
                            <Badge
                                variant={getUrgencyColor(condition.daysUntilDeadline)}
                                className="ml-2 flex-shrink-0"
                            >
                                {condition.daysUntilDeadline === 0
                                    ? t("broker.priorityCards.approachingConditions.today")
                                    : condition.daysUntilDeadline === 1
                                        ? t("broker.priorityCards.approachingConditions.tomorrow")
                                        : t("broker.priorityCards.approachingConditions.daysLeft", { days: condition.daysUntilDeadline })
                                }
                            </Badge>
                        </button>
                    ))}
                </div>
            )}
        </Section>
    );
}
