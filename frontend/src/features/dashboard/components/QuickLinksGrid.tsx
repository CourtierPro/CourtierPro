import { type ReactNode } from "react";
import { useNavigate } from "react-router-dom";
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
    const navigate = useNavigate();

    const handleClick = (link: QuickLink) => {
        if (link.disabled) return;
        if (link.onClick) {
            link.onClick();
        } else if (link.href) {
            navigate(link.href);
        }
    };

    return (
        <div
            className={cn(
                "grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-4 gap-4",
                className
            )}
        >
            {links.map((link) => {
                const isDefault = link.variant === "default";
                return (
                    <Button
                        key={link.id}
                        variant={link.variant || "outline"}
                        disabled={link.disabled}
                        className={cn(
                            "h-auto flex-col gap-3 py-5 px-4",
                            "transition-all duration-200",
                            link.disabled && "opacity-50 cursor-not-allowed",
                            !link.disabled && !isDefault && "hover:bg-primary/10 hover:border-primary/50",
                            isDefault && "hover:bg-primary/90"
                        )}
                        onClick={() => handleClick(link)}
                    >
                        <span className={cn(isDefault ? "text-primary-foreground" : "text-primary")}>{link.icon}</span>
                        <span className={cn("text-sm font-medium text-center leading-tight", isDefault && "text-primary-foreground")}>
                            {link.label}
                        </span>
                    </Button>
                );
            })}
        </div>
    );
}
