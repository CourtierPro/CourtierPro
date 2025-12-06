import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";

export function ClientsPage() {
  const { t } = useTranslation("clients");
  return (
    <div className="space-y-6">
      <PageHeader
        title={t("title")}
        subtitle={t("subtitle")}
      />
    </div>
  );
}
