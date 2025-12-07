import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";

export function AnalyticsPage() {
  const { t } = useTranslation("analytics");
  return (
    <div className="space-y-6">
      <PageHeader
        title={t("title")}
        subtitle={t("subtitle")}
      />
    </div>
  );
}
