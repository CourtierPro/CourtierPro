import { useTranslation } from "react-i18next";

interface DocumentChecklistModalProps {
  open: boolean;
  onClose: () => void;
}

export function DocumentChecklistModal({ open, onClose }: DocumentChecklistModalProps) {
  const { t } = useTranslation("common");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-xl font-semibold mb-2">{t("modals.documentChecklist")}</h2>
        <p className="text-sm text-muted-foreground">
          This is the modal for viewing the checklist of required documents for a transaction (placeholder only).
        </p>
        {/* TODO: list required docs + statuses */}
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
