import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";

export function SystemLogsPage() {
  const { t } = useTranslation("admin");
  return (
    <div className="space-y-6">
      <PageHeader
        title={t("systemLogs")}
        subtitle={t("systemLogsSubtitle")}
      />
    </div>
  );
}
