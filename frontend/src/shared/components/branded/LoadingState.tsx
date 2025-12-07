import { Loader2 } from "lucide-react";
import { cn } from "@/shared/utils/utils";

interface LoadingStateProps {
    message?: string;
    className?: string;
}

export function LoadingState({ message = "Loading...", className }: LoadingStateProps) {
    return (
        <div className={cn("flex flex-col items-center justify-center p-8 min-h-[200px]", className)}>
            <Loader2 className="w-8 h-8 animate-spin text-primary mb-4" />
            <p className="text-muted-foreground text-sm">{message}</p>
        </div>
    );
}
