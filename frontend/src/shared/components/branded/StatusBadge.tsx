import { Badge } from "@/shared/components/ui/badge";
import { cn } from "@/shared/utils/utils";
import { useTranslation } from "react-i18next";

export type StatusType = "active" | "closed" | "terminated" | "pending" | "approved" | "needs_revision" | "confirmed" | "completed" | "default";

interface StatusBadgeProps {
    status: string;
    type?: StatusType; // Optional mapping if status string doesn't match types directly
    className?: string;
}

export function StatusBadge({ status, type, className }: StatusBadgeProps) {
    const { t } = useTranslation('transactions');

    // Map status string to a visual variant if type is not provided
    const variant = type || (status.toLowerCase() as StatusType);

    const getVariant = (v: string): "default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "info" => {
        switch (v) {
            case "active":
            case "approved":
            case "completed":
                return "success";
            case "closed":
            case "closed_successfully":
                return "secondary";
            case "terminated":
            case "terminated_early":
            case "needs_revision":
                return "destructive";
            case "pending":
            case "upcoming":
                return "warning";
            case "confirmed":
                return "info";
            default:
                return "secondary";
        }
    };

    // Get translated label for transaction status
    const getLabel = (s: string): string => {
        // Try direct key first (e.g., "ACTIVE" -> "Active")
        const directTranslated = t(s, { defaultValue: '' });
        if (directTranslated && directTranslated !== s) {
            return directTranslated;
        }
        // Fallback: replace underscores and capitalize
        return s.replace(/_/g, " ");
    };

    return (
        <Badge
            variant={getVariant(variant)}
            className={cn("font-normal capitalize", className)}
        >
            {getLabel(status)}
        </Badge>
    );
}
