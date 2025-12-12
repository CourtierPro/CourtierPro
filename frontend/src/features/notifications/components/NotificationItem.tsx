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
        if (!notification.isRead) {
            onMarkAsRead(notification.publicId);
        }
    };

    return (
        <Card
            className={cn(
                "cursor-pointer transition-colors hover:bg-muted/50 mb-2 relative overflow-hidden",
                !notification.isRead && "bg-primary/5 border-primary/20",
                notification.isRead && "opacity-80"
            )}
            onClick={handleClick}
        >
            {!notification.isRead && (
                <div className="absolute top-0 right-0 w-2 h-full bg-primary" />
            )}
            <CardHeader className="p-3 pb-1">
                <div className="flex justify-between items-center gap-2">
                    <CardTitle className={cn("text-sm font-semibold", !notification.isRead && "text-primary")}>
                        {notification.title}
                    </CardTitle>
                    {!notification.isRead && (
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
