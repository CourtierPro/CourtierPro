import { useTranslation } from "react-i18next";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Section } from "@/shared/components/branded/Section";
import { SectionHeader } from "@/shared/components/branded/SectionHeader";
import { AttributeRow } from "@/shared/components/branded/AttributeRow";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { useUserProfile } from "@/features/profile/api/queries";

export function ProfilePage() {
  const { t } = useTranslation("profile");
  const { data: user, isLoading, error, refetch } = useUserProfile();

  if (isLoading) return <LoadingState />;
  if (error) return <ErrorState message={error.message} onRetry={() => refetch()} />;
  if (!user) return <ErrorState message={t("userNotFound")} />;

  return (
    <div className="space-y-6">
      <PageHeader
        title={t("title")}
        subtitle={t("subtitle")}
        actions={
          <Button variant="outline">{t("editProfile")}</Button>
        }
      />

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Section className="md:col-span-2">
          <SectionHeader title={t("personalInfo")} />
          <div className="space-y-0">
            <AttributeRow label={t("fullName")} value={user.name} />
            <AttributeRow label={t("email")} value={user.email} />
            <AttributeRow label={t("role")} value={user.role} />
          </div>
        </Section>

        <Section>
          <SectionHeader title={t("professionalDetails")} />
          <div className="space-y-0">
            <AttributeRow label={t("agency")} value={user.agency} />
            <AttributeRow label={t("licenseNumber")} value={user.licenseNumber} />
          </div>
        </Section>
      </div>
    </div>
  );
}
