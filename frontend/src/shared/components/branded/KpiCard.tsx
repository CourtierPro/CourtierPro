import type { ReactNode, KeyboardEvent } from "react";
import { Card, CardContent } from "@/shared/components/ui/card";
import { cn } from "@/shared/utils/utils";

interface KpiCardProps {
    title: string;
    value: string | number;
    icon: ReactNode;
    infoButton?: ReactNode;
    trend?: {
        value: number;
        label: string;
        direction: "up" | "down" | "neutral";
    };
    className?: string;
    onClick?: () => void;
    changed?: boolean;
}

export function KpiCard({ title, value, icon, infoButton, trend, className, onClick, changed }: KpiCardProps) {
    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        if (!onClick) return;
        if (event.key === "Enter" || event.key === " ") {
            event.preventDefault();
            onClick();
        }
    };

    return (
        <Card
            className={cn(
                "overflow-hidden relative",
                onClick && "cursor-pointer transition hover:shadow-md",
                className
            )}
            onClick={onClick}
            role={onClick ? "button" : undefined}
            tabIndex={onClick ? 0 : undefined}
            onKeyDown={handleKeyDown}
        >
                {changed && (
                    <span
                        aria-label="Updated"
                        title="Updated"
                        className="absolute top-2 right-2 w-2 h-2 rounded-full bg-red-500 ring-2 ring-white dark:ring-slate-900 kpi-dot-pulse"
                    />
                )}
                <CardContent className="p-6">
                    <div className="flex items-center justify-between space-y-0 pb-2">
                        <div className="flex items-center gap-2">
                            <p className="text-sm font-medium text-muted-foreground">{title}</p>
                            {infoButton}
                        </div>
                        <div className="text-muted-foreground">{icon}</div>
                    </div>
                    <div className="flex items-baseline justify-between">
                        <div className="text-2xl font-bold">{value}</div>
                        {trend && (
                            <div
                                className={cn(
                                    "text-xs font-medium flex items-center",
                                    trend.direction === "up" && "text-green-600 dark:text-green-400",
                                    trend.direction === "down" && "text-red-600 dark:text-red-400",
                                    trend.direction === "neutral" && "text-muted-foreground"
                                )}
                            >
                                {trend.direction === "up" && "+"}
                                {trend.value}% {trend.label}
                            </div>
                        )}
                    </div>
                </CardContent>
            </Card>
    );
}
