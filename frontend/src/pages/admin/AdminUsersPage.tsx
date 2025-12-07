// src/pages/admin/AdminUsersPage.tsx
import { useState } from "react";
import { Plus, Search, Shield, User } from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { InviteUserModal } from "@/features/admin/components/InviteUserModal";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Badge } from "@/shared/components/ui/badge";
import { useAdminUsers } from "@/features/admin/api/queries";
import { useSetUserActiveStatus } from "@/features/admin/api/mutations";
import { logError, getErrorMessage } from "@/shared/utils/error-utils";

import "./AdminUsersPage.css";

export function AdminUsersPage() {
  const { t } = useTranslation("admin");
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");

  const { data: users, isLoading, error } = useAdminUsers();
  const setUserActiveStatus = useSetUserActiveStatus();

  const handleToggleStatus = async (userId: string, currentStatus: boolean) => {
    try {
      await setUserActiveStatus.mutateAsync({ userId, active: !currentStatus });
    } catch (err) {
      const message = getErrorMessage(err, t("failedToToggleStatus"));
      toast.error(message);
      if (err instanceof Error) {
        logError(err);
      }
    }
  };

  const filteredUsers =
    users?.filter(
      (u) =>
        u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.lastName.toLowerCase().includes(searchTerm.toLowerCase()),
    ) ?? [];

  if (isLoading) {
    return <LoadingState />;
  }

  if (error) {
    return (
      <ErrorState
        title={t("errorLoadingUsers")}
        message={t("couldNotLoadUserList")}
      />
    );
  }

  return (
    <div className="admin-users-page">
      {/* Header */}
      <div className="admin-users-header">
        <PageHeader
          title={t("userManagement")}
          subtitle={t("manageSystemAccess")}
        />
        <Button
          className="admin-users-invite-btn"
          onClick={() => setShowInviteModal(true)}
        >
          <Plus className="h-4 w-4 mr-2" />
          {t("inviteUser")}
        </Button>
      </div>

      {/* Search bar */}
      <div className="admin-users-search-card">
        <div className="admin-users-search-wrapper">
          <div className="admin-users-search-icon">
            <Search className="h-5 w-5 text-gray-400" />
          </div>
          <Input
            type="text"
            className="admin-users-search-input"
            placeholder={t("searchUsers")}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Users table */}
      <div className="admin-users-table-card">
        <table className="admin-users-table">
          <thead>
          <tr>
            <th>{t("user")}</th>
            <th>{t("role")}</th>
            <th>{t("status")}</th>
            <th>{t("language")}</th>
            <th className="admin-users-actions-col">
              <span className="sr-only">Actions</span>
            </th>
          </tr>
          </thead>
          <tbody>
          {filteredUsers.length > 0 ? (
            filteredUsers.map((user) => (
              <tr key={user.id} className="admin-users-row">
                <td>
                  <div className="admin-users-user-cell">
                    <div className="admin-users-avatar">
                      <User className="h-5 w-5 text-gray-500" />
                    </div>
                    <div className="admin-users-user-text">
                      <div className="admin-users-user-name">
                        {user.firstName} {user.lastName}
                      </div>
                      <div className="admin-users-user-email">
                        {user.email}
                      </div>
                    </div>
                  </div>
                </td>
                <td>
                  <div className="admin-users-role-cell">
                    <Shield className="h-4 w-4 text-gray-400 mr-2" />
                    <span>{user.role}</span>
                  </div>
                </td>
                <td>
                  <Badge
                    variant={user.active ? "success" : "destructive"}
                    className="rounded-full"
                  >
                    {user.active ? t("active") : t("inactive")}
                  </Badge>
                </td>
                <td className="admin-users-lang-cell">
                  {user.preferredLanguage === "en" ? "EN" : "FR"}
                </td>
                <td className="admin-users-actions-col">
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() =>
                      handleToggleStatus(user.id, user.active)
                    }
                    className={
                      user.active
                        ? "text-red-600 hover:text-red-900 hover:bg-red-50"
                        : "text-green-600 hover:text-green-900 hover:bg-green-50"
                    }
                  >
                    {user.active ? t("deactivate") : t("activate")}
                  </Button>
                </td>
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={5} className="admin-users-empty">
                {t("noUsersFound")}
              </td>
            </tr>
          )}
          </tbody>
        </table>
      </div>

      {/* Invite modal */}
      <InviteUserModal
        open={showInviteModal}
        onClose={() => setShowInviteModal(false)}
        onUserCreated={() => {}}
      />
    </div>
  );
}
