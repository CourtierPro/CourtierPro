import type { ReactNode } from "react";
import { cn } from "@/shared/utils/utils";

interface EmptyStateProps {
    icon: ReactNode;
    title: string;
    description: string;
    action?: ReactNode;
    className?: string;
}

export function EmptyState({ icon, title, description, action, className }: EmptyStateProps) {
    return (
        <div className={cn("flex flex-col items-center justify-center text-center p-8 min-h-[300px] rounded-lg border-2 border-dashed border-muted-foreground/25", className)}>
            <div className="text-muted-foreground/50 mb-4 [&>svg]:w-12 [&>svg]:h-12">{icon}</div>
            <h3 className="text-lg font-semibold text-foreground mb-2">{title}</h3>
            <p className="text-muted-foreground max-w-sm mb-6">{description}</p>
            {action && <div>{action}</div>}
        </div>
    );
}
