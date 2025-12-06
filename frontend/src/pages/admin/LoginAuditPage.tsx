import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";

export function LoginAuditPage() {
  const { t } = useTranslation("admin");
  return (
    <div className="space-y-6">
      <PageHeader
        title={t("loginAudit")}
        subtitle={t("loginAuditSubtitle")}
      />
    </div>
  );
}
