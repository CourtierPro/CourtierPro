import type { ReactNode } from "react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { cn } from "@/shared/utils/utils";

import type { HTMLAttributes } from "react";

interface SectionProps extends HTMLAttributes<HTMLDivElement> {
    title?: string;
    description?: string;
    children: ReactNode;
    className?: string;
    action?: ReactNode;
    // Allow ref for compatibility with libraries like react-dropzone
    ref?: React.Ref<HTMLDivElement>;
}

export function Section({ title, description, children, className, action, ...props }: SectionProps) {
    const hasHeader = title || description || action;

    return (
        <Card className={cn("overflow-hidden", className)} {...props}>
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
