import { useTranslation } from "react-i18next";

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
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-xl font-semibold mb-2">{t("modals.scheduleAppointment")}</h2>
        <p className="text-sm text-muted-foreground">
          This is the modal for scheduling an appointment (placeholder only).
        </p>
        {/* TODO: Implement appointment scheduling form and integration */}
        <button
          className="mt-4 text-sm text-orange-600 hover:underline"
          onClick={onClose}
        >
          {t("actions.close")}
        </button>
      </div>
    </div>
  );
}
