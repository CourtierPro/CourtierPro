import { useTranslation } from "react-i18next";
import "./InviteClientModal.css";

interface InviteClientModalProps {
  open: boolean;
  onClose: () => void;
}

export function InviteClientModal({ open, onClose }: InviteClientModalProps) {
  const { t } = useTranslation("common");
  if (!open) return null;

  return (
    <div className="cp-modal-backdrop">
      <div className="cp-modal-container">
        <h2 className="cp-modal-title">{t("modals.inviteClient")}</h2>

        <p className="cp-modal-subtitle">
          This is the modal for inviting a new client to CourtierPro (placeholder only).
        </p>

        <div className="cp-modal-actions">
          <button className="cp-modal-close-btn" onClick={onClose}>
            {t("actions.close")}
          </button>
        </div>
      </div>
    </div>
  );
}
