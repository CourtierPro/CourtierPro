import { Routes, Route, Navigate } from "react-router-dom";
import { Suspense, lazy } from "react";
import { useAuth0 } from "@auth0/auth0-react";

import { AppShell } from "@/shared/components/layout/AppShell";
import { RequireRole } from "@/features/auth/RequireRole";
import { getRoleFromUser, getTestRole, type AppRole } from "@/features/auth/roleUtils";

const ShowcasePage = lazy(() => import("@/pages/ShowcasePage"));

// Dashboards
const BrokerDashboardPage = lazy(() => import("@/pages/dashboard/BrokerDashboardPage").then(module => ({ default: module.BrokerDashboardPage })));
const ClientDashboardPage = lazy(() => import("@/pages/dashboard/ClientDashboardPage").then(module => ({ default: module.ClientDashboardPage })));
const AdminDashboardPage = lazy(() => import("@/pages/dashboard/AdminDashboardPage").then(module => ({ default: module.AdminDashboardPage })));

// Transactions
const BrokerTransactionsPage = lazy(() => import("@/pages/transactions/BrokerTransactionsPage").then(module => ({ default: module.BrokerTransactionsPage })));
const ClientTransactionsPage = lazy(() => import("@/pages/transactions/ClientTransactionsPage").then(module => ({ default: module.ClientTransactionsPage })));
const BrokerTransactionDetailsPage = lazy(() => import("@/pages/transactions/BrokerTransactionDetailsPage").then(module => ({ default: module.BrokerTransactionDetailsPage })));
const ClientTransactionDetailsPage = lazy(() => import("@/pages/transactions/ClientTransactionDetailsPage").then(module => ({ default: module.ClientTransactionDetailsPage })));
const CreateTransactionPage = lazy(() => import("@/pages/transactions/CreateTransactionPage"));

// Documents
const DocumentsPage = lazy(() => import("@/pages/documents/DocumentsPage").then(module => ({ default: module.DocumentsPage })));
const TransactionDocumentsPage = lazy(() => import("@/pages/documents/TransactionDocumentsPage").then(module => ({ default: module.TransactionDocumentsPage })));

// Appointments / analytics / clients / notifications
const AppointmentsPage = lazy(() => import("@/pages/appointments/AppointmentsPage").then(module => ({ default: module.AppointmentsPage })));
const AnalyticsPage = lazy(() => import("@/pages/analytics/AnalyticsPage").then(module => ({ default: module.AnalyticsPage })));
const ClientsPage = lazy(() => import("@/pages/clients/ClientsPage").then(module => ({ default: module.ClientsPage })));
const NotificationsPage = lazy(() => import("@/pages/notifications/NotificationsPage").then(module => ({ default: module.NotificationsPage })));

// Admin
const AdminUsersPage = lazy(() => import("@/pages/admin/AdminUsersPage").then(module => ({ default: module.AdminUsersPage })));
const AdminSettingsPage = lazy(() => import("@/pages/admin/AdminSettingsPage").then(module => ({ default: module.AdminSettingsPage })));
const LoginAuditPage = lazy(() => import("@/pages/admin/LoginAuditPage").then(module => ({ default: module.LoginAuditPage })));
const SystemLogsPage = lazy(() => import("@/pages/admin/SystemLogsPage").then(module => ({ default: module.SystemLogsPage })));
const PasswordResetAuditPage = lazy(() => import("@/pages/admin/PasswordResetAuditPage").then(module => ({ default: module.PasswordResetAuditPage })));

// Profile
const ProfilePage = lazy(() => import("@/pages/profile/ProfilePage").then(module => ({ default: module.ProfilePage })));

// Error / status pages
const UnauthorizedPage = lazy(() => import("@/pages/status/UnauthorizedPage").then(module => ({ default: module.UnauthorizedPage })));
const ForbiddenPage = lazy(() => import("@/pages/status/ForbiddenPage").then(module => ({ default: module.ForbiddenPage })));
const InternalServerErrorPage = lazy(() => import("@/pages/status/InternalServerErrorPage").then(module => ({ default: module.InternalServerErrorPage })));
const ServiceUnavailablePage = lazy(() => import("@/pages/status/ServiceUnavailablePage").then(module => ({ default: module.ServiceUnavailablePage })));
const NotFoundPage = lazy(() => import("@/pages/status/NotFoundPage").then(module => ({ default: module.NotFoundPage })));

export function AppRoutes() {
    const authDisabled = import.meta.env.VITE_AUTH_DISABLED === "true";
    const { user } = useAuth0();
    const role: AppRole | null = authDisabled ? getTestRole() : getRoleFromUser(user);

    const defaultRouteForRole: Record<AppRole, string> = {
        broker: "/dashboard/broker",
        client: "/dashboard/client",
        admin: "/dashboard/admin",
    };

    return (
        <Suspense
            fallback={
                <div className="flex min-h-screen items-center justify-center">
                    <div className="h-8 w-8 animate-spin rounded-full border-4 border-primary border-t-transparent" />
                </div>
            }
        >
            <Routes>
                <Route
                    path="/"
                    element={
                        role ? (
                            <Navigate to={defaultRouteForRole[role]} replace />
                        ) : (
                            <Navigate to="/unauthorized" replace />
                        )
                    }
                />

                {/* Dashboards */}
                <Route
                    path="/dashboard/broker"
                    element={
                        <RequireRole allowed={["broker"]}>
                            <AppShell>
                                <BrokerDashboardPage />
                            </AppShell>
                        </RequireRole>
                    }
                />
                <Route
                    path="/dashboard/client"
                    element={
                        <RequireRole allowed={["client"]}>
                            <AppShell>
                                <ClientDashboardPage />
                            </AppShell>
                        </RequireRole>
                    }
                />
                <Route
                    path="/dashboard/admin"
                    element={
                        <RequireRole allowed={["admin"]}>
                            <AppShell>
                                <AdminDashboardPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Broker transactions list */}
                <Route
                    path="/transactions"
                    element={
                        <RequireRole allowed={["broker"]}>
                            <AppShell>
                                <BrokerTransactionsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Create transaction (broker) */}
                <Route
                    path="/transactions/new"
                    element={
                        <RequireRole allowed={["broker"]}>
                            <AppShell>
                                <CreateTransactionPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Client transactions list */}
                <Route
                    path="/my-transaction"
                    element={
                        <RequireRole allowed={["client"]}>
                            <AppShell>
                                <ClientTransactionsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Transaction details */}
                <Route
                    path="/transactions/:transactionId"
                    element={
                        <RequireRole allowed={["broker", "client"]}>
                            <AppShell>
                                {role === "broker" ? (
                                    <BrokerTransactionDetailsPage />
                                ) : (
                                    <ClientTransactionDetailsPage />
                                )}
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Documents (broker + client) */}
                <Route
                    path="/documents"
                    element={
                        <RequireRole allowed={["broker", "client"]}>
                            <AppShell>
                                <DocumentsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />
                <Route
                    path="/transactions/:transactionId/documents"
                    element={
                        <RequireRole allowed={["broker", "client"]}>
                            <AppShell>
                                <TransactionDocumentsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Appointments */}
                <Route
                    path="/appointments"
                    element={
                        <RequireRole allowed={["broker"]}>
                            <AppShell>
                                <AppointmentsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Analytics */}
                <Route
                    path="/analytics"
                    element={
                        <RequireRole allowed={["broker", "admin"]}>
                            <AppShell>
                                <AnalyticsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Clients */}
                <Route
                    path="/clients"
                    element={
                        <RequireRole allowed={["broker"]}>
                            <AppShell>
                                <ClientsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Notifications – everyone */}
                <Route
                    path="/notifications"
                    element={
                        <RequireRole allowed={["broker", "client", "admin"]}>
                            <AppShell>
                                <NotificationsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Admin pages – only ADMIN */}
                <Route
                    path="/admin/users"
                    element={
                        <RequireRole allowed={["admin"]}>
                            <AppShell>
                                <AdminUsersPage />
                            </AppShell>
                        </RequireRole>
                    }
                />
                <Route
                    path="/admin/settings"
                    element={
                        <RequireRole allowed={["admin"]}>
                            <AppShell>
                                <AdminSettingsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />
                <Route
                    path="/admin/login-audit"
                    element={
                        <RequireRole allowed={["admin"]}>
                            <AppShell>
                                <LoginAuditPage />
                            </AppShell>
                        </RequireRole>
                    }
                />
                <Route
                    path="/admin/system-logs"
                    element={
                        <RequireRole allowed={["admin"]}>
                            <AppShell>
                                <SystemLogsPage />
                            </AppShell>
                        </RequireRole>
                    }
                />
                <Route
                    path="/admin/password-reset-audit"
                    element={
                        <RequireRole allowed={["admin"]}>
                            <AppShell>
                                <PasswordResetAuditPage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Profile */}
                <Route
                    path="/profile"
                    element={
                        <RequireRole allowed={["broker", "client", "admin"]}>
                            <AppShell>
                                <ProfilePage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Showcase */}
                <Route
                    path="/dev/showcase"
                    element={
                        <RequireRole allowed={["admin"]}>
                            <AppShell>
                                <ShowcasePage />
                            </AppShell>
                        </RequireRole>
                    }
                />

                {/* Error / status pages (used by axios error handler) */}
                <Route
                    path="/unauthorized"
                    element={
                        <AppShell>
                            <UnauthorizedPage />
                        </AppShell>
                    }
                />
                <Route
                    path="/forbidden"
                    element={
                        <AppShell>
                            <ForbiddenPage />
                        </AppShell>
                    }
                />
                <Route
                    path="/internal-server-error"
                    element={
                        <AppShell>
                            <InternalServerErrorPage />
                        </AppShell>
                    }
                />
                <Route
                    path="/service-unavailable"
                    element={
                        <AppShell>
                            <ServiceUnavailablePage />
                        </AppShell>
                    }
                />

                {/* 404 */}
                <Route
                    path="*"
                    element={
                        <AppShell>
                            <NotFoundPage />
                        </AppShell>
                    }
                />
            </Routes>
        </Suspense>
    );
}
