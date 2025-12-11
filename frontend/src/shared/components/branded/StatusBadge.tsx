import { Badge } from "@/shared/components/ui/badge";
import { cn } from "@/shared/utils/utils";

export type StatusType = "active" | "closed" | "terminated" | "pending" | "approved" | "needs_revision" | "confirmed" | "completed" | "default";

interface StatusBadgeProps {
    status: string;
    type?: StatusType; // Optional mapping if status string doesn't match types directly
    className?: string;
}

export function StatusBadge({ status, type, className }: StatusBadgeProps) {
    // Map status string to a visual variant if type is not provided
    const variant = type || (status.toLowerCase() as StatusType);

    const getVariant = (v: string): "default" | "secondary" | "destructive" | "outline" | "success" | "warning" | "info" => {
        switch (v) {
            case "active":
            case "approved":
            case "completed":
                return "success";
            case "closed":
                return "secondary";
            case "terminated":
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

    return (
        <Badge
            variant={getVariant(variant)}
            className={cn("font-normal capitalize", className)}
        >
            {status.replace(/_/g, " ")}
        </Badge>
    );
}
