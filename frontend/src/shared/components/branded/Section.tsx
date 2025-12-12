import { Card, CardHeader, CardContent, CardTitle, CardDescription } from "@/shared/components/ui/card";
import { cn } from "@/shared/utils/utils";
import { type ReactNode, type HTMLAttributes, forwardRef } from "react";

interface SectionProps extends HTMLAttributes<HTMLDivElement> {
    title?: string;
    description?: string;
    children: ReactNode;
    action?: ReactNode;
}

export const Section = forwardRef<HTMLDivElement, SectionProps>(({
    title,
    description,
    children,
    className,
    action,
    ...props
}, ref) => {
    const hasHeader = title || description || action;

    return (
        <Card ref={ref} className={cn("overflow-hidden", className)} {...props}>
            {hasHeader && (
                <CardHeader className="flex flex-row items-start justify-between space-y-0 pb-4">
                    <div className="space-y-1">
                        {title && <CardTitle>{title}</CardTitle>}
                        {description && <CardDescription>{description}</CardDescription>}
                    </div>
                    {action && <div>{action}</div>}
                </CardHeader>
            )}
            <CardContent>
                {children}
            </CardContent>
        </Card>
    );
});

Section.displayName = "Section";
