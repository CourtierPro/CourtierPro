// src/App.tsx
import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

import { AppShell } from "./layout/AppShell";

import ShowcasePage from "@/pages/ShowcasePage";

// Dashboards
import { BrokerDashboardPage } from "@/pages/dashboard/BrokerDashboardPage";
import { ClientDashboardPage } from "@/pages/dashboard/ClientDashboardPage";
import { AdminDashboardPage } from "@/pages/dashboard/AdminDashboardPage";

// Transactions
import { BrokerTransactionsPage } from "@/pages/transactions/BrokerTransactionsPage";
import { ClientTransactionsPage } from "@/pages/transactions/ClientTransactionsPage";
import { BrokerTransactionDetailsPage } from "./pages/transactions/BrokerTransactionDetailsPage";
import { ClientTransactionDetailsPage } from "./pages/transactions/ClientTransactionDetailsPage";

// Documents
import { DocumentsPage } from "@/pages/documents/DocumentsPage";
import { TransactionDocumentsPage } from "@/pages/documents/TransactionDocumentsPage";

// Appointments / analytics / clients / notifications
import { AppointmentsPage } from "@/pages/appointments/AppointmentsPage";
import { AnalyticsPage } from "@/pages/analytics/AnalyticsPage";
import { ClientsPage } from "@/pages/clients/ClientsPage";
import { NotificationsPage } from "@/pages/notifications/NotificationsPage";

// Admin
import { AdminUsersPage } from "@/pages/admin/AdminUsersPage";
import { AdminSettingsPage } from "@/pages/admin/AdminSettingsPage";
import { LoginAuditPage } from "@/pages/admin/LoginAuditPage";
import { SystemLogsPage } from "@/pages/admin/SystemLogsPage";

// Profile
import { ProfilePage } from "@/pages/profile/ProfilePage";

// Error / status pages
import { UnauthorizedPage } from "@/pages/status/UnauthorizedPage";
import { ForbiddenPage } from "@/pages/status/ForbiddenPage";
import { InternalServerErrorPage } from "@/pages/status/InternalServerErrorPage";
import { ServiceUnavailablePage } from "@/pages/status/ServiceUnavailablePage";
import { NotFoundPage } from "@/pages/status/NotFoundPage";

import { getRoleFromUser, type AppRole } from "@/auth/roleUtils";
import { RequireRole } from "@/auth/RequireRole";

export default function App() {
    const { isAuthenticated, isLoading, loginWithRedirect, user } = useAuth0();

    if (isLoading) {
        return (
            <div className="flex min-h-screen items-center justify-center">
                <p>Loading…</p>
            </div>
        );
    }

    const role: AppRole | null = getRoleFromUser(user);


    if (!isAuthenticated || !role) {
        return (
            <div className="flex min-h-screen items-center justify-center bg-muted">
                <button
                    onClick={() => loginWithRedirect()}
                    className="rounded-md bg-orange-600 px-4 py-2 text-sm font-medium text-white hover:bg-orange-700"
                >
                    Sign in with Auth0
                </button>
            </div>
        );
    }

    const defaultRouteForRole: Record<AppRole, string> = {
        broker: "/dashboard/broker",
        client: "/dashboard/client",
        admin: "/dashboard/admin",
    };

    return (
        <Routes>
            {/* Default redirect based on Auth0 role */}
            <Route
                path="/"
                element={<Navigate to={defaultRouteForRole[role]} replace />}
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

            {/* Transaction details – choose component based on the role */}
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

            {/* Profile – accessible to every connected roles */}
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

            {/* Feedback and UI components showcase */}
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

            {/* Catch-all 404 */}
            <Route
                path="*"
                element={
                    <AppShell>
                        <NotFoundPage />
                    </AppShell>
                }
            />
        </Routes>
    );
}
