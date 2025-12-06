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

    const getVariantStyles = (v: string) => {
        switch (v) {
            case "active":
            case "approved":
            case "completed":
                return "bg-green-100 text-green-700 hover:bg-green-100/80 border-green-200";
            case "closed":
                return "bg-gray-100 text-gray-700 hover:bg-gray-100/80 border-gray-200";
            case "terminated":
            case "needs_revision":
                return "bg-red-100 text-red-700 hover:bg-red-100/80 border-red-200";
            case "pending":
            case "upcoming":
                return "bg-yellow-100 text-yellow-700 hover:bg-yellow-100/80 border-yellow-200";
            case "confirmed":
                return "bg-blue-100 text-blue-700 hover:bg-blue-100/80 border-blue-200";
            default:
                return "bg-gray-100 text-gray-700 hover:bg-gray-100/80 border-gray-200";
        }
    };

    return (
        <Badge
            variant="outline"
            className={cn("font-normal capitalize", getVariantStyles(variant), className)}
        >
            {status.replace(/_/g, " ")}
        </Badge>
    );
}
