import { useTranslation } from "react-i18next";
import { useNavigate } from "react-router-dom";
import { formatDistanceToNow } from "date-fns";
import { enUS, fr } from "date-fns/locale";
import { FileText, Bell, AlertCircle, CheckCircle, Info, Sparkles, XCircle } from "lucide-react";
import { toast } from "sonner";
import { NotificationCategory, NotificationType, type NotificationResponseDTO } from "@/features/notifications/api/notificationsApi";
import { cn } from "@/shared/utils/utils";

interface NotificationsWidgetProps {
  notifications: NotificationResponseDTO[];
  onMarkAsRead?: (id: string) => void;
}

const getIcon = (notification: NotificationResponseDTO) => {
  if (notification.type === NotificationType.BROADCAST) {
    return <AlertCircle className="w-3.5 h-3.5 text-orange-600 flex-shrink-0" />;
  }

  switch (notification.category) {
    case NotificationCategory.BROADCAST:
      return <AlertCircle className="w-3.5 h-3.5 text-orange-600 flex-shrink-0" />;
    case NotificationCategory.WELCOME:
      return <Sparkles className="w-3.5 h-3.5 text-amber-500 flex-shrink-0" />;
    case NotificationCategory.DOCUMENT_REQUEST:
    case NotificationCategory.DOCUMENT_SUBMITTED:
    case NotificationCategory.DOCUMENT_REVISION:
      return <FileText className="w-3.5 h-3.5 text-primary flex-shrink-0" />;
    case NotificationCategory.DOCUMENT_APPROVED:
      return <CheckCircle className="w-3.5 h-3.5 text-green-600 flex-shrink-0" />;
    case NotificationCategory.DOCUMENT_REJECTED:
      return <XCircle className="w-3.5 h-3.5 text-destructive flex-shrink-0" />;
    case NotificationCategory.STAGE_UPDATE:
      return <Info className="w-3.5 h-3.5 text-blue-500 flex-shrink-0" />;
    case NotificationCategory.TRANSACTION_CANCELLED:
      return <XCircle className="w-3.5 h-3.5 text-destructive flex-shrink-0" />;
    default:
      return <Bell className="w-3.5 h-3.5 text-foreground/60 flex-shrink-0" />;
  }
};

const getTimeString = (createdAt: string, locale: string) => {
  const dateLocale = locale === "fr" ? fr : enUS;
  return formatDistanceToNow(new Date(createdAt), { addSuffix: true, locale: dateLocale });
};

export function NotificationsWidget({
  notifications,
  onMarkAsRead = () => {},
}: NotificationsWidgetProps) {
  const { t, i18n } = useTranslation("dashboard");
  const navigate = useNavigate();

  // Limit to 5 notifications
  const limitedNotifications = notifications.slice(0, 5);

  const handleNotificationClick = (notification: NotificationResponseDTO) => {
    if (!notification.read) {
      try {
        onMarkAsRead(notification.publicId);
      } catch (error) {
        console.error("Failed to mark notification as read", error);
      }
    }

    if (notification.relatedTransactionId) {
      if (notification.category === NotificationCategory.TRANSACTION_CANCELLED) {
        toast.info(t("dashboard:transactionDeletedMessage", "This transaction has been cancelled or deleted."));
        return;
      }

      const isDocumentRelated = (
        [
          NotificationCategory.DOCUMENT_REQUEST,
          NotificationCategory.DOCUMENT_SUBMITTED,
          NotificationCategory.DOCUMENT_APPROVED,
          NotificationCategory.DOCUMENT_REJECTED,
          NotificationCategory.DOCUMENT_REVISION,
        ] as NotificationCategory[]
      ).includes(notification.category);

      const targetUrl = isDocumentRelated
        ? `/transactions/${notification.relatedTransactionId}?tab=documents`
        : `/transactions/${notification.relatedTransactionId}`;

      navigate(targetUrl);
    }
  };

  return (
    <div className="space-y-2">
      {limitedNotifications.map((notification) => (
        <div
          key={notification.publicId}
          onClick={() => handleNotificationClick(notification)}
          role="button"
          tabIndex={0}
          className={cn(
            "p-3 rounded-lg border transition-colors cursor-pointer hover:bg-muted/50",
            !notification.read && "bg-primary/5 border-primary/20",
            notification.read && "border-border/50 bg-background"
          )}
        >
          <div className="flex items-start gap-2">
            {getIcon(notification)}
            <div className="flex-1 min-w-0">
              <p className={cn("text-sm font-medium line-clamp-1", !notification.read && "text-foreground")}>
                {notification.title}
              </p>
              <p className="text-xs text-muted-foreground mt-0.5">
                {getTimeString(notification.createdAt, i18n.language)}
              </p>
            </div>
            {!notification.read && (
              <div className="w-2 h-2 rounded-full bg-primary flex-shrink-0 mt-1" />
            )}
          </div>
        </div>
      ))}
    </div>
  );
}
