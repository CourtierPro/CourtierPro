import { Lock } from 'lucide-react';
import { cn } from '@/shared/utils/utils';

interface PermissionDeniedProps {
    message: string;
    className?: string;
}

export function PermissionDenied({ message, className }: PermissionDeniedProps) {
    return (
        <div className={cn("flex flex-col items-center justify-center p-8 text-center bg-muted/30 rounded-lg border border-dashed border-muted-foreground/25", className)}>
            <div className="bg-muted p-3 rounded-full mb-3">
                <Lock className="h-6 w-6 text-muted-foreground" />
            </div>
            <p className="text-muted-foreground font-medium">{message}</p>
        </div>
    );
}
