import { useState } from "react";
import { useTranslation } from "react-i18next";
import { InviteUserModal } from "@/features/admin/components/InviteUserModal";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { Button } from "@/shared/components/ui/button";

export function AdminSettingsPage() {
    const { t } = useTranslation("admin");
    const [isInviteOpen, setIsInviteOpen] = useState(false);

    return (
        <>
            <div className="max-w-6xl mx-auto pt-24 space-y-6">
                <div className="flex items-center justify-between gap-4">
                    <PageHeader
                        title={t("orgSettings")}
                        subtitle={t("manageOrgSettings")}
                    />

                    <Button
                        onClick={() => setIsInviteOpen(true)}
                    >
                        {t("inviteUser")}
                    </Button>
                </div>

                <div className="rounded-lg border bg-white p-4 shadow-sm space-y-2">
                    <h2 className="text-lg font-semibold">{t("orgSettingsTitle")}</h2>
                    <p className="text-sm text-muted-foreground">
                        {t("orgSettingsDesc")}
                    </p>
                </div>
            </div>

            <InviteUserModal
                open={isInviteOpen}
                onClose={() => setIsInviteOpen(false)}
            />
        </>
    );
}
