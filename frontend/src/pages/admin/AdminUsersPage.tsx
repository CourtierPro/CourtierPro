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
    <div className="space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <PageHeader
          title={t("userManagement")}
          subtitle={t("manageSystemAccess")}
        />
        <Button
          className="rounded-md px-6"
          onClick={() => setShowInviteModal(true)}
        >
          <Plus className="h-4 w-4 mr-2" />
          {t("inviteUser")}
        </Button>
      </div>

      {/* Search bar */}
      <div className="bg-card rounded-xl border border-border p-4 shadow-sm">
        <div className="relative flex items-center">
          <div className="absolute left-3 flex items-center pointer-events-none">
            <Search className="h-5 w-5 text-muted-foreground" />
          </div>
          <Input
            type="text"
            className="pl-10"
            placeholder={t("searchUsers")}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* Users table */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/50 text-muted-foreground text-xs uppercase tracking-wider">
              <tr>
                <th className="px-6 py-4 font-medium">{t("user")}</th>
                <th className="px-6 py-4 font-medium">{t("role")}</th>
                <th className="px-6 py-4 font-medium">{t("status")}</th>
                <th className="px-6 py-4 font-medium">{t("language")}</th>
                <th className="px-6 py-4 font-medium text-right">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {filteredUsers.length > 0 ? (
                filteredUsers.map((user) => (
                  <tr key={user.id} className="group transition-colors hover:bg-muted/30">
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-secondary text-secondary-foreground">
                          <User className="h-5 w-5" />
                        </div>
                        <div className="flex flex-col">
                          <div className="font-semibold text-foreground">
                            {user.firstName} {user.lastName}
                          </div>
                          <div className="text-xs text-muted-foreground">
                            {user.email}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center text-foreground">
                        <Shield className="h-4 w-4 text-muted-foreground mr-2" />
                        <span>{user.role}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <Badge
                        variant={user.active ? "success" : "destructive"}
                        className="rounded-md"
                      >
                        {user.active ? t("active") : t("inactive")}
                      </Badge>
                    </td>
                    <td className="px-6 py-4 text-xs uppercase text-muted-foreground">
                      {user.preferredLanguage === "en" ? "EN" : "FR"}
                    </td>
                    <td className="px-6 py-4 text-right">
                      <Button
                        variant="ghost"
                        size="sm"
                        onClick={() =>
                          handleToggleStatus(user.id, user.active)
                        }
                        className={
                          user.active
                            ? "text-destructive hover:text-destructive hover:bg-destructive/10"
                            : "text-emerald-600 hover:text-emerald-700 hover:bg-emerald-50 dark:text-emerald-400 dark:hover:bg-emerald-900/20"
                        }
                      >
                        {user.active ? t("deactivate") : t("activate")}
                      </Button>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan={5} className="px-6 py-8 text-center text-muted-foreground">
                    {t("noUsersFound")}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Invite modal */}
      <InviteUserModal
        open={showInviteModal}
        onClose={() => setShowInviteModal(false)}
        onUserCreated={() => { }}
      />
    </div>
  );
}
