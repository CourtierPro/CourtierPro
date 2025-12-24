
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Megaphone } from "lucide-react";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { useNotifications, useMarkNotificationAsRead } from "@/features/notifications/api/notificationsApi";
import { NotificationItem } from "@/features/notifications/components/NotificationItem";
import { BroadcastMessageModal } from "@/features/notifications/components/BroadcastMessageModal";
import { useCurrentUser } from "@/features/auth/api/useCurrentUser";
import { Card, CardContent } from "@/shared/components/ui/card";
import { Button } from "@/shared/components/ui/button";
import { Loader2 } from "lucide-react";

export function NotificationsPage() {
  const { t } = useTranslation("notifications");
  const { data: notifications, isLoading } = useNotifications();
  const { mutate: markAsRead } = useMarkNotificationAsRead();
  const { data: currentUser } = useCurrentUser();
  const [isBroadcastModalOpen, setIsBroadcastModalOpen] = useState(false);

  const isAdmin = currentUser?.role === "ADMIN";

  return (
    <div className="space-y-6 container max-w-3xl mx-auto">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <PageHeader
          title={t("title")}
          subtitle={t("subtitle") || "Manage your alerts and updates"}
        />
        {isAdmin && (
          <Button
            onClick={() => setIsBroadcastModalOpen(true)}
            className="w-full sm:w-auto"
          >
            <Megaphone className="h-4 w-4 mr-2" />
            {t("broadcast.create", "Create Broadcast")}
          </Button>
        )}
      </div>

      <BroadcastMessageModal
        open={isBroadcastModalOpen}
        onOpenChange={setIsBroadcastModalOpen}
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
