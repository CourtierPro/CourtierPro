import { useTranslation } from "react-i18next";
import { Button } from "@/shared/components/ui/button";

interface DocumentReviewModalProps {
  open: boolean;
  onClose: () => void;
}

export function DocumentReviewModal({ open, onClose }: DocumentReviewModalProps) {
  const { t } = useTranslation("common");
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
      <div className="w-full max-w-md rounded-lg bg-white p-6 shadow-lg">
        <h2 className="text-xl font-semibold mb-2">{t("modals.reviewDocument")}</h2>
        <p className="text-sm text-muted-foreground">
          This is the modal for reviewing and approving a document (placeholder only).
        </p>
        {/* TODO: show document metadata + approve / reject actions */}
        <Button variant="link" onClick={onClose} className="mt-4 text-orange-600 p-0">
          {t("actions.close")}
        </Button>
      </div>
    </div>
  );
}
