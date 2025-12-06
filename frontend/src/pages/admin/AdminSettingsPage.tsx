// src/pages/admin/AdminSettingsPage.tsx
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { InviteUserModal } from "@/features/admin/components/InviteUserModal";
import { PageHeader } from "@/shared/components/branded/PageHeader";

export function AdminSettingsPage() {
    const { t } = useTranslation("admin");
    // true/false : est-ce que la modale est ouverte ?
    const [isInviteOpen, setIsInviteOpen] = useState(false);

    return (
        <>
            {/* Principal content of the page */}
            <div className="max-w-6xl mx-auto pt-24 space-y-6">
                {/* Titre + bouton à droite */}
                <div className="flex items-center justify-between gap-4">
                    <PageHeader
                        title={t("orgSettings")}
                        subtitle={t("manageOrgSettings")}
                    />

                    {/* Boutton that opens the modal */}
                    <button
                        type="button"
                        className="rounded-md bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700"
                        onClick={() => setIsInviteOpen(true)}
                    >
                        {t("inviteUser")}
                    </button>
                </div>

                {/* Simple placeholder card for org settings */}
                <div className="rounded-lg border bg-white p-4 shadow-sm space-y-2">
                    <h2 className="text-lg font-semibold">{t("orgSettingsTitle")}</h2>
                    <p className="text-sm text-muted-foreground">
                        {t("orgSettingsDesc")}
                    </p>
                </div>
            </div>

            {/* Invitation mode (outside the container to float in the middle of the screen) */}
            <InviteUserModal
                open={isInviteOpen}
                onClose={() => setIsInviteOpen(false)}
            // onUserCreated={(user) => console.log("Created:", user)}
            />
        </>
    );
}
