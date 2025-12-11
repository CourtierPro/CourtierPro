import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";

interface InviteClientModalProps {
  open: boolean;
  onClose: () => void;
}

export function InviteClientModal({ open, onClose }: InviteClientModalProps) {
  const { t } = useTranslation("common");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-background/80 backdrop-blur-sm">
      <div className="w-full max-w-lg p-6 bg-card rounded-xl border border-border shadow-lg animate-in fade-in zoom-in-95 duration-200">
        <h2 className="text-lg font-semibold text-card-foreground mb-2">{t("modals.inviteClient")}</h2>

        <p className="text-sm text-muted-foreground mb-4">
          This is the modal for inviting a new client to CourtierPro (placeholder only).
        </p>

        <div className="flex justify-end mt-4">
          <Button variant="ghost" onClick={onClose}>
            {t("actions.close")}
          </Button>
        </div>
      </div>
    </div>
  );
}
