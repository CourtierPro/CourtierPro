import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";

export function TransactionDocumentsPage() {
  const { t } = useTranslation("documents");
  return (
    <div className="space-y-6">
      <PageHeader
        title={t("transactionDocuments")}
        subtitle={t("transactionDocumentsSubtitle")}
      />
    </div>
  );
}
