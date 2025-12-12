import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { useNotifications, useMarkNotificationAsRead } from "@/features/notifications/api/notificationsApi";
import { NotificationItem } from "@/features/notifications/components/NotificationItem";
import { Card, CardContent } from "@/shared/components/ui/card";
import { Loader2 } from "lucide-react";

export function NotificationsPage() {
  const { t } = useTranslation("notifications");
  const { data: notifications, isLoading } = useNotifications();
  const { mutate: markAsRead } = useMarkNotificationAsRead();

  return (
    <div className="space-y-6 container max-w-3xl mx-auto">
      <PageHeader
        title={t("title")}
        subtitle={t("subtitle") || "Manage your alerts and updates"}
      />

      {isLoading ? (
        <div className="flex justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : notifications && notifications.length > 0 ? (
        <div className="grid gap-2">
          {[...notifications]
            .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
            .map((notification) => (
              <NotificationItem
                key={notification.publicId}
                notification={notification}
                onMarkAsRead={(id) => markAsRead(id)}
              />
            ))}
        </div>
      ) : (
        <Card className="bg-muted/10 border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <p>{t("empty")}</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
