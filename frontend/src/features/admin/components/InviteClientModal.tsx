import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";
import { Dialog, DialogContent, DialogHeader, DialogTitle } from "@/shared/components/ui/dialog";

interface InviteClientModalProps {
  open: boolean;
  onClose: () => void;
}

export function InviteClientModal({ open, onClose }: InviteClientModalProps) {
  const { t } = useTranslation("common");

  return (
    <Dialog open={open} onOpenChange={(val) => !val && onClose()}>
      <DialogContent className="sm:max-w-lg rounded-xl">
        <DialogHeader>
          <DialogTitle>{t("modals.inviteClient")}</DialogTitle>
        </DialogHeader>

        <p className="text-sm text-muted-foreground mb-4">
          This is the modal for inviting a new client to CourtierPro (placeholder only).
        </p>

        <div className="flex justify-end mt-4">
          <Button variant="ghost" onClick={onClose}>
            {t("actions.close")}
          </Button>
        </div>
      </DialogContent>
    </Dialog>
  );
}
