import { AlertTriangle, RefreshCw } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/utils/utils";

interface ErrorStateProps {
    title?: string;
    message: string;
    onRetry?: () => void;
    className?: string;
}

export function ErrorState({ title = "Something went wrong", message, onRetry, className }: ErrorStateProps) {
    return (
        <div className={cn("flex flex-col items-center justify-center p-6 rounded-lg bg-destructive/10 border border-destructive/20 text-center", className)}>
            <AlertTriangle className="w-8 h-8 text-destructive mb-4" />
            <h3 className="text-lg font-semibold text-destructive mb-2">{title}</h3>
            <p className="text-destructive/80 max-w-md mb-6">{message}</p>
            {onRetry && (
                <Button variant="destructive" onClick={onRetry} className="gap-2">
                    <RefreshCw className="w-4 h-4" />
                    Retry
                </Button>
            )}
        </div>
    );
}
