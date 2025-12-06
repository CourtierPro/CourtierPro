import { useTranslation } from "react-i18next";

interface InviteClientModalProps {
  open: boolean;
  onClose: () => void;
}

export function InviteClientModal({ open, onClose }: InviteClientModalProps) {
  const { t } = useTranslation("common");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-xl font-semibold mb-2">{t("modals.inviteClient")}</h2>
        <p className="text-sm text-muted-foreground">
          This is the modal for inviting a new client to CourtierPro (placeholder only).
        </p>
        {/* TODO: Implement client invite form and email flow */}
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
