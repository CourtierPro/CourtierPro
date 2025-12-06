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

    const filteredUsers = users?.filter(
        (u) =>
            u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
            u.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
            u.lastName.toLowerCase().includes(searchTerm.toLowerCase())
    ) ?? [];

    if (isLoading) {
        return <LoadingState />;
    }

    if (error) {
        return <ErrorState title={t("errorLoadingUsers")} message={t("couldNotLoadUserList")} />;
    }

    return (
        <div className="space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <PageHeader title={t("userManagement")} subtitle={t("manageSystemAccess")} />
                <Button
                    onClick={() => setShowInviteModal(true)}
                >
                    <Plus className="h-4 w-4 mr-2" />
                    {t("inviteUser")}
                </Button>
            </div>

            <div className="bg-white p-4 rounded-lg shadow-sm border border-gray-200 flex flex-col sm:flex-row gap-4">
                <div className="relative flex-1">
                    <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <Search className="h-5 w-5 text-gray-400" />
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

            <div className="bg-white shadow overflow-hidden sm:rounded-lg border border-gray-200">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                {t("user")}
                            </th>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                {t("role")}
                            </th>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                {t("status")}
                            </th>
                            <th scope="col" className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                {t("language")}
                            </th>
                            <th scope="col" className="relative px-6 py-3">
                                <span className="sr-only">Actions</span>
                            </th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {filteredUsers.length > 0 ? (
                            filteredUsers.map((user) => (
                                <tr key={user.id} className="hover:bg-gray-50">
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center">
                                            <div className="flex-shrink-0 h-10 w-10 rounded-full bg-gray-100 flex items-center justify-center">
                                                <User className="h-5 w-5 text-gray-500" />
                                            </div>
                                            <div className="ml-4">
                                                <div className="text-sm font-medium text-gray-900">
                                                    {user.firstName} {user.lastName}
                                                </div>
                                                <div className="text-sm text-gray-500">{user.email}</div>
                                            </div>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <div className="flex items-center">
                                            <Shield className="h-4 w-4 text-gray-400 mr-2" />
                                            <span className="text-sm text-gray-900">{user.role}</span>
                                        </div>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap">
                                        <Badge
                                            variant={user.active ? "success" : "destructive"}
                                            className="rounded-full"
                                        >
                                            {user.active ? t("active") : t("inactive")}
                                        </Badge>
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                        {user.preferredLanguage === "en" ? "English" : "Français"}
                                    </td>
                                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={() => handleToggleStatus(user.id, user.active)}
                                            className={user.active ? "text-red-600 hover:text-red-900 hover:bg-red-50" : "text-green-600 hover:text-green-900 hover:bg-green-50"}
                                        >
                                            {user.active ? t("deactivate") : t("activate")}
                                        </Button>
                                    </td>
                                </tr>
                            ))
                        ) : (
                            <tr>
                                <td colSpan={5} className="px-6 py-12 text-center text-gray-500">
                                    {t("noUsersFound")}
                                </td>
                            </tr>
                        )}
                    </tbody>
                </table>
            </div>

            <InviteUserModal
                open={showInviteModal}
                onClose={() => setShowInviteModal(false)}
                onUserCreated={() => {
                    // Query invalidation handles refresh automatically
                }}
            />
        </div>
    );
}
