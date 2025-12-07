import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";

interface CreateTransactionModalProps {
  open: boolean;
  onClose: () => void;
}

export function CreateTransactionModal({
  open,
  onClose,
}: CreateTransactionModalProps) {
  const { t } = useTranslation("common");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-xl font-semibold mb-2">{t("modals.createTransaction")}</h2>
        <p className="text-sm text-muted-foreground">
          This is the modal for creating a new transaction (placeholder only).
        </p>
        {/* TODO: Implement create transaction form and actions */}
        <Button variant="link" onClick={onClose} className="mt-4 text-orange-600 p-0">
          {t("actions.close")}
        </Button>
      </div>
    </div>
  );
}
