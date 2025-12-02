// src/pages/admin/ManageUsersPage.tsx
import { useEffect, useState } from "react";
import {
    InviteUserModal,
    type AdminUserResponse,
} from "@/components/modals/InviteUserModal";
import { getAdminUsers, setUserActiveStatus } from "@/pages/admin/adminUserApi";

export function AdminUsersPage() {
    const [users, setUsers] = useState<AdminUserResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const [isInviteOpen, setIsInviteOpen] = useState(false);

    // load users at the beginning
    useEffect(() => {
        async function load() {
            try {
                setLoading(true);
                setError(null);
                const data = await getAdminUsers();
                setUsers(data);
            } catch (e) {
                console.error("Error loading users", e);
                setError("Unable to load users.");
            } finally {
                setLoading(false);
            }
        }

        load();
    }, []);

    // when we create a user it is added to the list
    const handleUserCreated = (user: AdminUserResponse) => {
        setUsers((prev) => [user, ...prev]);
    };

    // Toggle active / inactive
    const handleToggle = async (user: AdminUserResponse) => {
        const newActive = !user.active;

        // petit update optimiste
        setUsers((prev) =>
            prev.map((u) =>
                u.id === user.id ? { ...u, active: newActive } : u
            )
        );

        try {
            const updated = await setUserActiveStatus(user.id, newActive);
            setUsers((prev) =>
                prev.map((u) => (u.id === user.id ? updated : u))
            );
        } catch (e) {
            console.error("Error updating user status", e);
            setError("Unable to update user status.");
            // rollback
            setUsers((prev) =>
                prev.map((u) =>
                    u.id === user.id ? { ...u, active: user.active } : u
                )
            );
        }
    };

    return (
        <>
            <div className="max-w-6xl mx-auto pt-24 space-y-6">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-semibold">Manage Users</h1>
                        <p className="text-sm text-muted-foreground">
                            Manage brokers, clients and admins in your organization.
                        </p>
                    </div>

                    <button
                        type="button"
                        className="rounded-md bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700"
                        onClick={() => setIsInviteOpen(true)}
                    >
                        Invite User
                    </button>
                </div>

                {error && (
                    <div className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-700">
                        {error}
                    </div>
                )}

                {/* Table */}
                <div className="rounded-lg border bg-white shadow-sm overflow-x-auto">
                    {loading ? (
                        <div className="px-4 py-6 text-sm text-muted-foreground">
                            Loading users…
                        </div>
                    ) : users.length === 0 ? (
                        <div className="px-4 py-6 text-sm text-muted-foreground">
                            No users yet. Click “Invite User” to add one.
                        </div>
                    ) : (
                        <table className="min-w-full text-sm">
                            <thead>
                            <tr className="bg-muted/60 text-xs uppercase text-muted-foreground">
                                <th className="px-4 py-2 text-left">Name</th>
                                <th className="px-4 py-2 text-left">Email</th>
                                <th className="px-4 py-2 text-left">Role</th>
                                <th className="px-4 py-2 text-left">Language</th>
                                <th className="px-4 py-2 text-left">Status</th>
                                <th className="px-4 py-2 text-right">Toggle</th>
                            </tr>
                            </thead>
                            <tbody>
                            {users.map((u) => (
                                <tr key={u.id} className="border-t">
                                    <td className="px-4 py-2">
                                        {u.firstName} {u.lastName}
                                    </td>
                                    <td className="px-4 py-2 text-xs text-muted-foreground">
                                        {u.email}
                                    </td>
                                    <td className="px-4 py-2 capitalize">{u.role.toLowerCase()}</td>
                                    <td className="px-4 py-2 uppercase text-xs">
                                        {u.preferredLanguage}
                                    </td>
                                    <td className="px-4 py-2">
                                        {u.active ? "Active" : "Inactive"}
                                    </td>
                                    <td className="px-4 py-2 text-right">
                                        <input
                                            type="checkbox"
                                            checked={u.active}
                                            onChange={() => handleToggle(u)}
                                        />
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    )}
                </div>
            </div>

            {/* Modale d’invitation */}
            <InviteUserModal
                open={isInviteOpen}
                onClose={() => setIsInviteOpen(false)}
                onUserCreated={handleUserCreated}
            />
        </>
    );
}
