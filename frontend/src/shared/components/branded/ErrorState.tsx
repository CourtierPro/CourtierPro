import { AlertTriangle, RefreshCw } from "lucide-react";
import { Button } from "@/shared/components/ui/button";
import { cn } from "@/shared/utils/utils";
import { useTranslation } from "react-i18next";

interface ErrorStateProps {
    title?: string;
    message: string;
    onRetry?: () => void;
    action?: React.ReactNode;
    className?: string;
}

export function ErrorState({ title, message, onRetry, action, className }: ErrorStateProps) {
    const { t } = useTranslation('common');
    const displayTitle = title ?? t('error.somethingWentWrong');

    return (
        <div className={cn("flex flex-col items-center justify-center p-8 rounded-xl bg-card border shadow-sm text-center", className)}>
            <div className="w-12 h-12 rounded-full bg-red-50 flex items-center justify-center mb-4">
                <AlertTriangle className="w-6 h-6 text-red-500" />
            </div>
            <h3 className="text-lg font-semibold text-foreground mb-2">{displayTitle}</h3>
            <p className="text-muted-foreground max-w-md mb-6">{message}</p>
            {onRetry && (
                <Button variant="outline" onClick={onRetry} className="gap-2">
                    <RefreshCw className="w-4 h-4" />
                    {t('actions.retry')}
                </Button>
            )}
            {action && <div className="mt-4">{action}</div>}
        </div>
    );
}
