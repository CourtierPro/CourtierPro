import { useTranslation } from 'react-i18next';
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '@/shared/components/ui/card';
import { Badge } from '@/shared/components/ui/badge';
import { formatDateTime } from '@/shared/utils/date';
import type { NotificationResponseDTO } from '../api/notificationsApi';
import { cn } from '@/shared/utils/utils';

interface NotificationItemProps {
    notification: NotificationResponseDTO;
    onMarkAsRead: (id: string) => void;
}

export function NotificationItem({ notification, onMarkAsRead }: NotificationItemProps) {
    const { t } = useTranslation('notifications');

    const handleClick = () => {
        if (!notification.read) {
            try {
                onMarkAsRead(notification.publicId);
            } catch (error) {
                console.error("Failed to mark notification as read", error);
            }
        }
    };


    return (
        <Card
            className={cn(
                "cursor-pointer transition-colors hover:bg-muted/50 mb-2 relative overflow-hidden",
                !notification.read && "bg-primary/5 border-primary/20",
                notification.read && "opacity-80",
                notification.type === 'BROADCAST' && "border-l-4 border-l-orange-500 bg-orange-50/50 dark:bg-orange-950/20"
            )}
            onClick={handleClick}
            role="button"
            tabIndex={0}
            aria-pressed={notification.read}
        >
            {!notification.read && (
                <div className={cn("absolute top-0 right-0 w-2 h-full bg-primary", notification.type === 'BROADCAST' && "bg-orange-500")} />
            )}
            <CardHeader className="p-3 pb-1">
                <div className="flex justify-between items-center gap-2">
                    <CardTitle className={cn("text-sm font-semibold flex items-center gap-2", !notification.read && "text-primary", notification.type === 'BROADCAST' && "text-orange-600 dark:text-orange-400")}>
                        {notification.type === 'BROADCAST' && (
                            <span className="text-xs bg-orange-100 dark:bg-orange-900 text-orange-600 dark:text-orange-300 px-1.5 py-0.5 rounded-full uppercase tracking-wider font-bold">
                                {t('broadcast', 'Broadcast')}
                            </span>
                        )}
                        {notification.title}
                    </CardTitle>
                    {!notification.read && (
                        <Badge variant="default" className="h-4 px-1 text-[10px]">
                            {t('new')}
                        </Badge>
                    )}
                </div>
                <CardDescription className="text-xs mt-0">
                    {formatDateTime(notification.createdAt)}
                </CardDescription>
            </CardHeader>
            <CardContent className="p-3 pt-1">
                <p className="text-sm text-foreground/80 line-clamp-2">
                    {notification.message}
                </p>
            </CardContent>
        </Card>
    );
}
