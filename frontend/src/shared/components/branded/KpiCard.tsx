import type { ReactNode } from "react";
import { Card, CardContent } from "@/shared/components/ui/card";
import { cn } from "@/shared/utils/utils";

interface KpiCardProps {
    title: string;
    value: string | number;
    icon: ReactNode;
    trend?: {
        value: number;
        label: string;
        direction: "up" | "down" | "neutral";
    };
    className?: string;
}

export function KpiCard({ title, value, icon, trend, className }: KpiCardProps) {
    return (
        <Card className={cn("overflow-hidden", className)}>
            <CardContent className="p-6">
                <div className="flex items-center justify-between space-y-0 pb-2">
                    <p className="text-sm font-medium text-muted-foreground">{title}</p>
                    <div className="text-muted-foreground">{icon}</div>
                </div>
                <div className="flex items-baseline justify-between">
                    <div className="text-2xl font-bold">{value}</div>
                    {trend && (
                        <div
                            className={cn(
                                "text-xs font-medium flex items-center",
                                trend.direction === "up" && "text-green-600",
                                trend.direction === "down" && "text-red-600",
                                trend.direction === "neutral" && "text-gray-500"
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
