import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";

interface ScheduleAppointmentModalProps {
  open: boolean;
  onClose: () => void;
}

export function ScheduleAppointmentModal({
  open,
  onClose,
}: ScheduleAppointmentModalProps) {
  const { t } = useTranslation("common");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div className="w-full max-w-md rounded-lg bg-card p-6 shadow-lg border border-border">
        <h2 className="text-xl font-semibold mb-2">{t("modals.scheduleAppointment")}</h2>
        <p className="text-sm text-muted-foreground">
          {t("modals.comingSoon")}
        </p>
        <Button variant="link" onClick={onClose} className="mt-4 text-primary p-0">
          {t("actions.close")}
        </Button>
      </div>
    </div>
  );
}
