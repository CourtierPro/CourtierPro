import { useState } from 'react';
import { Bell } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from '@/shared/components/ui/button';
import { Popover, PopoverContent, PopoverTrigger } from '@/shared/components/ui/popover';
import { ScrollArea } from '@/shared/components/ui/scroll-area';
import { Badge } from '@/shared/components/ui/badge';
import { useNotifications, useMarkNotificationAsRead } from '../api/notificationsApi';
import { NotificationItem } from './NotificationItem';

export function NotificationPopover() {
    const { t } = useTranslation('notifications');
    const navigate = useNavigate();
    const [open, setOpen] = useState(false);
    const { data: notifications = [] } = useNotifications();
    const { mutate: markAsRead } = useMarkNotificationAsRead();

    const unreadCount = notifications.filter(n => !n.read).length;

    const handleMarkAsRead = (id: string) => {
        markAsRead(id);
    };

    const handleViewAll = () => {
        setOpen(false);
        navigate('/notifications');
    };

    return (
        <Popover open={open} onOpenChange={setOpen}>
            <PopoverTrigger asChild>
                <Button variant="ghost" size="icon" className="relative text-foreground" aria-label={t('title')}>
                    <Bell className="w-5 h-5" />
                    {unreadCount > 0 && (
                        <span className="absolute top-1 right-1 flex h-4 w-4 items-center justify-center rounded-full bg-destructive text-destructive-foreground text-[10px] font-semibold">
                            {unreadCount}
                        </span>
                    )}
                </Button>
            </PopoverTrigger>
            <PopoverContent className="w-80 p-0" align="end">
                <div className="flex items-center justify-between border-b px-4 py-3">
                    <h4 className="text-sm font-semibold">{t('title')}</h4>
                    {unreadCount > 0 && (
                        <Badge variant="secondary" className="text-xs">
                            {unreadCount} {t('new')}
                        </Badge>
                    )}
                </div>
                <ScrollArea className="h-[350px]">
                    {notifications.length === 0 ? (
                        <div className="p-8 text-center text-sm text-muted-foreground">
                            {t('empty')}
                        </div>
                    ) : (
                        <div className="flex flex-col p-2">
                            {/* Backend already sorts by CreatedAtDesc */}
                            {notifications.map((notification) => (
                                <NotificationItem
                                    key={notification.publicId}
                                    notification={notification}
                                    onMarkAsRead={handleMarkAsRead}
                                />
                            ))}
                        </div>
                    )}
                </ScrollArea>
                <div className="border-t p-2">
                    <Button variant="ghost" className="w-full text-xs" onClick={handleViewAll}>
                        {t('viewAll') || "View all notifications"}
                    </Button>
                </div>
            </PopoverContent>
        </Popover>
    );
}
