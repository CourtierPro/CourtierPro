// src/pages/admin/AdminUsersPage.tsx
import { useState, useMemo } from "react";
import { Plus, Search, Shield, User, Filter, ChevronLeft, ChevronRight, ArrowUpDown, ArrowUp, ArrowDown } from "lucide-react";
import { useTranslation } from "react-i18next";
import { toast } from "sonner";

import { InviteUserModal } from "@/features/admin/components/InviteUserModal";
import { PageHeader } from "@/shared/components/branded/PageHeader";
import { LoadingState } from "@/shared/components/branded/LoadingState";
import { ErrorState } from "@/shared/components/branded/ErrorState";
import { Button } from "@/shared/components/ui/button";
import { Input } from "@/shared/components/ui/input";
import { Badge } from "@/shared/components/ui/badge";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuRadioGroup,
  DropdownMenuRadioItem,
  DropdownMenuTrigger,
  DropdownMenuSeparator,
  DropdownMenuLabel,
} from "@/shared/components/ui/dropdown-menu";
import { useAdminUsers } from "@/features/admin/api/queries";
import { useSetUserActiveStatus } from "@/features/admin/api/mutations";
import { logError, getErrorMessage } from "@/shared/utils/error-utils";

type RoleFilter = "all" | "BROKER" | "CLIENT" | "ADMIN";
type StatusFilter = "all" | "active" | "inactive";

const ITEMS_PER_PAGE = 10;

type SortField = 'name' | 'email' | 'role';
type SortOrder = 'asc' | 'desc';

export function AdminUsersPage() {
  const { t } = useTranslation("admin");
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [roleFilter, setRoleFilter] = useState<RoleFilter>("all");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("all");
  const [sortField, setSortField] = useState<SortField>('name');
  const [sortOrder, setSortOrder] = useState<SortOrder>('asc');
  const [currentPage, setCurrentPage] = useState(1);

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

  // Filter and search logic
  const filteredUsers = useMemo(() => {
    if (!users) return [];

    return users.filter((u) => {
      // Search filter
      const matchesSearch =
        u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
        u.lastName.toLowerCase().includes(searchTerm.toLowerCase());

      // Role filter
      const matchesRole = roleFilter === "all" || u.role === roleFilter;

      // Status filter
      const matchesStatus =
        statusFilter === "all" ||
        (statusFilter === "active" && u.active) ||
        (statusFilter === "inactive" && !u.active);

      return matchesSearch && matchesRole && matchesStatus;
    }).sort((a, b) => {
      let comparison = 0;
      if (sortField === 'name') {
        const nameA = `${a.firstName} ${a.lastName}`.toLowerCase();
        const nameB = `${b.firstName} ${b.lastName}`.toLowerCase();
        comparison = nameA.localeCompare(nameB);
      } else if (sortField === 'email') {
        comparison = a.email.toLowerCase().localeCompare(b.email.toLowerCase());
      } else if (sortField === 'role') {
        comparison = a.role.localeCompare(b.role);
      }
      return sortOrder === 'asc' ? comparison : -comparison;
    });
  }, [users, searchTerm, roleFilter, statusFilter, sortField, sortOrder]);

  // Pagination logic
  const totalPages = Math.ceil(filteredUsers.length / ITEMS_PER_PAGE);
  const paginatedUsers = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    return filteredUsers.slice(startIndex, startIndex + ITEMS_PER_PAGE);
  }, [filteredUsers, currentPage]);

  // Reset to page 1 when filters change
  const handleSearchChange = (value: string) => {
    setSearchTerm(value);
    setCurrentPage(1);
  };

  const handleRoleFilterChange = (value: string) => {
    setRoleFilter(value as RoleFilter);
    setCurrentPage(1);
  };

  const handleStatusFilterChange = (value: string) => {
    setStatusFilter(value as StatusFilter);
    setCurrentPage(1);
  };

  const handleSortChange = (field: SortField) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
    setCurrentPage(1);
  };

  const getSortIcon = (field: SortField) => {
    if (sortField !== field) return <ArrowUpDown className="h-3 w-3 ml-1 opacity-50" />;
    return sortOrder === 'asc'
      ? <ArrowUp className="h-3 w-3 ml-1" />
      : <ArrowDown className="h-3 w-3 ml-1" />;
  };

  const handlePreviousPage = () => {
    setCurrentPage((prev) => Math.max(prev - 1, 1));
  };

  const handleNextPage = () => {
    setCurrentPage((prev) => Math.min(prev + 1, totalPages));
  };

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

      {/* Search and Filters */}
      <div className="bg-card rounded-xl border border-border p-4 shadow-sm">
        <div className="flex flex-col sm:flex-row gap-4">
          {/* Search Bar */}
          <div className="relative flex items-center flex-1">
            <div className="absolute left-3 flex items-center pointer-events-none">
              <Search className="h-5 w-5 text-muted-foreground" />
            </div>
            <Input
              type="text"
              className="pl-10"
              placeholder={t("searchUsers")}
              value={searchTerm}
              onChange={(e) => handleSearchChange(e.target.value)}
            />
          </div>

          {/* Filters */}
          <div className="flex gap-2">
            {/* Role Filter */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm" className="gap-2">
                  <Filter className="h-4 w-4" />
                  {t("role")}: {roleFilter === "all" ? t("filterAll") : t(`inviteUser_role_${roleFilter}`)}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>{t("role")}</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuRadioGroup value={roleFilter} onValueChange={handleRoleFilterChange}>
                  <DropdownMenuRadioItem value="all">{t("filterAll")}</DropdownMenuRadioItem>
                  <DropdownMenuRadioItem value="BROKER">{t("inviteUser_role_BROKER")}</DropdownMenuRadioItem>
                  <DropdownMenuRadioItem value="CLIENT">{t("inviteUser_role_CLIENT")}</DropdownMenuRadioItem>
                  <DropdownMenuRadioItem value="ADMIN">{t("inviteUser_role_ADMIN")}</DropdownMenuRadioItem>
                </DropdownMenuRadioGroup>
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Status Filter */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm" className="gap-2">
                  <Filter className="h-4 w-4" />
                  {t("status")}: {statusFilter === "all" ? t("filterAll") : t(statusFilter)}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuLabel>{t("status")}</DropdownMenuLabel>
                <DropdownMenuSeparator />
                <DropdownMenuRadioGroup value={statusFilter} onValueChange={handleStatusFilterChange}>
                  <DropdownMenuRadioItem value="all">{t("filterAll")}</DropdownMenuRadioItem>
                  <DropdownMenuRadioItem value="active">{t("active")}</DropdownMenuRadioItem>
                  <DropdownMenuRadioItem value="inactive">{t("inactive")}</DropdownMenuRadioItem>
                </DropdownMenuRadioGroup>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </div>

      {/* Users table */}
      <div className="rounded-xl border border-border bg-card shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm text-left">
            <thead className="bg-muted/50 text-muted-foreground text-xs uppercase tracking-wider">
              <tr>
                <th
                  className="px-6 py-4 font-medium cursor-pointer hover:text-foreground transition-colors"
                  onClick={() => handleSortChange('name')}
                >
                  <div className="flex items-center">
                    {t("user")}
                    {getSortIcon('name')}
                  </div>
                </th>
                <th
                  className="px-6 py-4 font-medium cursor-pointer hover:text-foreground transition-colors"
                  onClick={() => handleSortChange('role')}
                >
                  <div className="flex items-center">
                    {t("role")}
                    {getSortIcon('role')}
                  </div>
                </th>
                <th className="px-6 py-4 font-medium">{t("status")}</th>
                <th className="px-6 py-4 font-medium">{t("language")}</th>
                <th className="px-6 py-4 font-medium text-right">
                  <span className="sr-only">{t("actions")}</span>
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {paginatedUsers.length > 0 ? (
                paginatedUsers.map((user) => (
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

        {/* Pagination */}
        {totalPages > 1 && (
          <div className="flex items-center justify-between border-t border-border px-6 py-4">
            <div className="text-sm text-muted-foreground">
              {t("showingResults", {
                from: (currentPage - 1) * ITEMS_PER_PAGE + 1,
                to: Math.min(currentPage * ITEMS_PER_PAGE, filteredUsers.length),
                total: filteredUsers.length,
                defaultValue: `Showing ${(currentPage - 1) * ITEMS_PER_PAGE + 1} - ${Math.min(currentPage * ITEMS_PER_PAGE, filteredUsers.length)} of ${filteredUsers.length}`
              })}
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handlePreviousPage}
                disabled={currentPage === 1}
              >
                <ChevronLeft className="h-4 w-4" />
                {t("previous")}
              </Button>
              <div className="text-sm text-muted-foreground px-2">
                {t("pageOf", {
                  current: currentPage,
                  total: totalPages,
                  defaultValue: `${currentPage} / ${totalPages}`
                })}
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={handleNextPage}
                disabled={currentPage === totalPages}
              >
                {t("next")}
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
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
