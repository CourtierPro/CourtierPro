import { CheckCircle } from "lucide-react";
import { cn } from "@/shared/utils/utils";

interface StageBadgeProps {
    stageNumber: number;
    label: string;
    status: "completed" | "current" | "upcoming" | "terminated";
    className?: string;
}

export function StageBadge({ stageNumber, label, status, className }: StageBadgeProps) {
    const isCompleted = status === "completed";
    const isCurrent = status === "current";
    const isTerminated = status === "terminated";

    return (
        <div className={cn("flex flex-col items-center gap-2", className)}>
            <div
                className={cn(
                    "w-8 h-8 rounded-full flex items-center justify-center transition-all flex-shrink-0",
                    isCurrent && "ring-4 ring-primary/30",
                    (isCompleted || isCurrent) && !isTerminated ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground",
                    isTerminated && "bg-muted text-muted-foreground"
                )}
            >
                {isCompleted ? (
                    <CheckCircle className="w-5 h-5" />
                ) : (
                    <span className="text-xs font-bold">{stageNumber}</span>
                )}
            </div>
            <span
                className={cn(
                    "text-xs text-center max-w-[100px]",
                    isCurrent ? "text-primary font-medium" : "text-muted-foreground",
                    isTerminated && "text-muted-foreground"
                )}
            >
                {label}
            </span>
        </div>
    );
}
