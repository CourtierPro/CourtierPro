import type { ReactNode } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { cn } from "@/shared/utils/utils";

interface SectionProps {
    title?: string;
    description?: string;
    children: ReactNode;
    className?: string;
    action?: ReactNode;
}

export function Section({ title, description, children, className, action }: SectionProps) {
    const hasHeader = title || description || action;

    return (
        <Card className={cn("overflow-hidden", className)}>
            {hasHeader && (
                <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-4">
                    <div className="space-y-1">
                        {title && <CardTitle className="text-lg font-semibold">{title}</CardTitle>}
                        {description && <CardDescription>{description}</CardDescription>}
                    </div>
                    {action && <div>{action}</div>}
                </CardHeader>
            )}
            <CardContent className={cn(!hasHeader && "pt-6")}>{children}</CardContent>
        </Card>
    );
}
