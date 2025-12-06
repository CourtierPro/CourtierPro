import type { ReactNode } from "react";
import { cn } from "@/shared/utils/utils";

interface SectionHeaderProps {
    title: string;
    description?: string;
    action?: ReactNode;
    className?: string;
}

export function SectionHeader({ title, description, action, className }: SectionHeaderProps) {
    return (
        <div className={cn("flex items-center justify-between mb-4", className)}>
            <div>
                <h3 className="text-lg font-medium text-foreground">{title}</h3>
                {description && <p className="text-sm text-muted-foreground">{description}</p>}
            </div>
            {action && <div>{action}</div>}
        </div>
    );
}
