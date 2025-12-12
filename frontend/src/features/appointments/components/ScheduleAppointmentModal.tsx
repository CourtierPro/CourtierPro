import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";

interface ScheduleAppointmentModalProps {
  open: boolean;
  onClose: () => void;
}

export function ScheduleAppointmentModal({
  open,
  onClose,
}: ScheduleAppointmentModalProps) {
  const { t } = useTranslation("common");

  return (
    <Dialog open={open} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="sm:max-w-md rounded-lg">
        <DialogHeader>
          <DialogTitle>{t("modals.scheduleAppointment")}</DialogTitle>
        </DialogHeader>
        <p className="text-sm text-muted-foreground">
          {t("modals.comingSoon")}
        </p>
        <Button variant="link" onClick={onClose} className="mt-4 text-primary p-0">
          {t("actions.close")}
        </Button>
      </DialogContent>
    </Dialog>
  );
}
