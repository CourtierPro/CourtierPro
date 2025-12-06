// src/pages/admin/AdminUsersPage.tsx
import { useEffect, useState } from "react";
import {
  InviteUserModal,
  type AdminUserResponse,
} from "@/components/modals/InviteUserModal";
import { getAdminUsers, setUserActiveStatus } from "@/pages/admin/adminUserApi";
import "./AdminUsersPage.css";

export function AdminUsersPage() {
  const [users, setUsers] = useState<AdminUserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isInviteOpen, setIsInviteOpen] = useState(false);

  // Load users on mount
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

  // When a new user is created from the modal, add it at the top of the list
  const handleUserCreated = (user: AdminUserResponse) => {
    setUsers((prev) => [user, ...prev]);
  };

  // Toggle active / inactive
  const handleToggle = async (user: AdminUserResponse) => {
    const newActive = !user.active;

    // Optimistic update
    setUsers((prev) =>
      prev.map((u) => (u.id === user.id ? { ...u, active: newActive } : u)),
    );

    try {
      const updated = await setUserActiveStatus(user.id, newActive);
      setUsers((prev) =>
        prev.map((u) => (u.id === user.id ? updated : u)),
      );
    } catch (e) {
      console.error("Error updating user status", e);
      setError("Unable to update user status.");

      // Rollback on error
      setUsers((prev) =>
        prev.map((u) =>
          u.id === user.id ? { ...u, active: user.active } : u,
        ),
      );
    }
  };

  return (
    <>
      <div className="admin-page admin-users-page">
        {/* Header */}
        <div className="admin-page-header">
          <div>
            <h1 className="admin-page-title">Manage Users</h1>
            <p className="admin-page-subtitle">
              Manage brokers, clients and admins in your organization.
            </p>
          </div>

          <button
            type="button"
            className="admin-primary-btn"
            onClick={() => setIsInviteOpen(true)}
          >
            Invite User
          </button>
        </div>

        {error && <div className="users-alert users-alert-error">{error}</div>}

        {/* Table */}
        <div className="admin-table-card">
          {loading ? (
            <div className="admin-table-empty">Loading users…</div>
          ) : users.length === 0 ? (
            <div className="admin-table-empty">
              No users yet. Click “Invite User” to add one.
            </div>
          ) : (
            <table className="admin-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Language</th>
                  <th>Status</th>
                  <th className="admin-table-toggle-col">Toggle</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>
                      {u.firstName} {u.lastName}
                    </td>
                    <td className="admin-table-email">{u.email}</td>
                    <td className="admin-table-role">
                      {u.role.toLowerCase()}
                    </td>
                    <td className="admin-table-lang">
                      {u.preferredLanguage}
                    </td>
                    <td>{u.active ? "Active" : "Inactive"}</td>
                    <td className="admin-table-toggle-col">
                      <label className="user-toggle">
                        <input
                          type="checkbox"
                          checked={u.active}
                          onChange={() => handleToggle(u)}
                        />
                        <span className="user-toggle-slider" />
                      </label>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>

      {/* Invite modal */}
      <InviteUserModal
        open={isInviteOpen}
        onClose={() => setIsInviteOpen(false)}
        onUserCreated={handleUserCreated}
      />
    </>
  );
}
