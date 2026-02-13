import { type ReactNode } from "react";
import { Link } from "react-router-dom";
import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/utils/utils";

export interface QuickLink {
    id: string;
    label: string;
    icon: ReactNode;
    href?: string;
    onClick?: () => void;
    variant?: "default" | "outline" | "ghost" | "secondary";
    disabled?: boolean;
}

interface QuickLinksGridProps {
    links: QuickLink[];
    className?: string;
}

export function QuickLinksGrid({ links, className }: QuickLinksGridProps) {
    // navigate is still used if button logic is complex but we prefer Link for standard navigations

    return (
        <div
            className={cn(
                "grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-4",
                className
            )}
        >
            {links.map((link) => {
                const isDefault = link.variant === "default";
                const commonClasses = cn(
                    "h-auto flex-col gap-3 py-5 px-4",
                    "transition-all duration-200",
                    link.disabled && "opacity-50 cursor-not-allowed",
                    !link.disabled && !isDefault && "hover:bg-primary/10 hover:border-primary/50",
                    isDefault && "hover:bg-primary/90"
                );

                const content = (
                    <>
                        <span className={cn(isDefault ? "text-primary-foreground" : "text-primary")}>{link.icon}</span>
                        <span className={cn("text-sm font-medium text-center leading-tight", isDefault && "text-primary-foreground")}>
                            {link.label}
                        </span>
                    </>
                );

                if (link.href && !link.disabled) {
                    return (
                        <Button
                            key={link.id}
                            variant={link.variant || "outline"}
                            className={commonClasses}
                            asChild
                        >
                            <Link to={link.href}>
                                {content}
                            </Link>
                        </Button>
                    );
                }

                return (
                    <Button
                        key={link.id}
                        variant={link.variant || "outline"}
                        disabled={link.disabled}
                        className={commonClasses}
                        onClick={link.onClick}
                    >
                        {content}
                    </Button>
                );
            })}
        </div>
    );
}
