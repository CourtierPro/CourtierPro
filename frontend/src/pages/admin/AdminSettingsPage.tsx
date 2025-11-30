// src/pages/admin/AdminSettingsPage.tsx
import { useState } from "react";
import { InviteUserModal } from "@/components/modals/InviteUserModal";

export function AdminSettingsPage() {
    // true/false : est-ce que la modale est ouverte ?
    const [isInviteOpen, setIsInviteOpen] = useState(false);

    return (
        <>
            {/* Principal content of the page */}
            <div className="max-w-6xl mx-auto pt-24 space-y-6">
                {/* Titre + bouton à droite */}
                <div className="flex items-center justify-between gap-4">
                    <div>
                        <h1 className="text-2xl font-semibold">
                            Admin – Organization Settings
                        </h1>
                        <p className="text-sm text-muted-foreground">
                            Manage your organization settings and invite new users to
                            CourtierPro.
                        </p>
                    </div>

                    {/* Boutton that opens the modal */}
                    <button
                        type="button"
                        className="rounded-md bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700"
                        onClick={() => setIsInviteOpen(true)}
                    >
                        Invite User
                    </button>
                </div>

                {/* Simple placeholder card for org settings */}
                <div className="rounded-lg border bg-white p-4 shadow-sm space-y-2">
                    <h2 className="text-lg font-semibold">Organization settings</h2>
                    <p className="text-sm text-muted-foreground">
                        Here you will be able to configure global options for your
                        organization (branding, default language, security rules, etc.).
                        For now, you can invite new brokers and clients using the
                        &quot;Invite User&quot; button above.
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
