import type { ReactNode } from "react";
import { cn } from "@/shared/utils/utils";

interface AttributeRowProps {
    label: string;
    value: ReactNode;
    className?: string;
}

export function AttributeRow({ label, value, className }: AttributeRowProps) {
    return (
        <div className={cn("flex flex-col sm:flex-row sm:justify-between py-2 border-b border-border last:border-0", className)}>
            <span className="text-sm font-medium text-muted-foreground">{label}</span>
            <span className="text-sm text-foreground mt-1 sm:mt-0">{value}</span>
        </div>
    );
}
