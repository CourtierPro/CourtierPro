import { cn } from "@/shared/utils/utils";
import type { ReactNode } from "react";

interface StatLineProps {
    label: string;
    value: string | number;
    icon?: ReactNode;
    variant?: "default" | "warning" | "danger" | "success";
    className?: string;
}

export function StatLine({ label, value, icon, variant = "default", className }: StatLineProps) {
    return (
        <div
            className={cn(
                "flex items-center justify-between py-2.5 px-3 rounded-lg transition-colors",
                variant === "default" && "bg-muted/40",
                variant === "warning" && "bg-amber-50 dark:bg-amber-950/30",
                variant === "danger" && "bg-red-50 dark:bg-red-950/30",
                variant === "success" && "bg-emerald-50 dark:bg-emerald-950/30",
                className
            )}
        >
            <div className="flex items-center gap-2">
                {icon && (
                    <span
                        className={cn(
                            "shrink-0",
                            variant === "default" && "text-muted-foreground",
                            variant === "warning" && "text-amber-600 dark:text-amber-400",
                            variant === "danger" && "text-red-600 dark:text-red-400",
                            variant === "success" && "text-emerald-600 dark:text-emerald-400"
                        )}
                    >
                        {icon}
                    </span>
                )}
                <span className="text-sm text-muted-foreground">{label}</span>
            </div>
            <span
                className={cn(
                    "text-sm font-semibold tabular-nums",
                    variant === "default" && "text-foreground",
                    variant === "warning" && "text-amber-700 dark:text-amber-300",
                    variant === "danger" && "text-red-700 dark:text-red-300",
                    variant === "success" && "text-emerald-700 dark:text-emerald-300"
                )}
            >
                {value}
            </span>
        </div>
    );
}
