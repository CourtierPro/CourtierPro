import { Routes, Route, Navigate } from 'react-router-dom'
import { AppShell } from './layout/AppShell'

import ShowcasePage from '@/pages/ShowcasePage'

// Dashboards
import { BrokerDashboardPage } from '@/pages/dashboard/BrokerDashboardPage'
import { ClientDashboardPage } from '@/pages/dashboard/ClientDashboardPage'
import { AdminDashboardPage } from '@/pages/dashboard/AdminDashboardPage'

// Transactions
import { BrokerTransactionsPage } from '@/pages/transactions/BrokerTransactionsPage'
import { ClientTransactionsPage } from '@/pages/transactions/ClientTransactionsPage'
import { BrokerTransactionDetailsPage } from './pages/transactions/BrokerTransactionDetailsPage'
import { ClientTransactionDetailsPage } from './pages/transactions/ClientTransactionDetailsPage'
import CreateTransactionPage from '@/pages/transactions/CreateTransactionPage'

// Documents
import { DocumentsPage } from '@/pages/documents/DocumentsPage'
import { TransactionDocumentsPage } from '@/pages/documents/TransactionDocumentsPage'

// Appointments / analytics / clients / notifications
import { AppointmentsPage } from '@/pages/appointments/AppointmentsPage'
import { AnalyticsPage } from '@/pages/analytics/AnalyticsPage'
import { ClientsPage } from '@/pages/clients/ClientsPage'
import { NotificationsPage } from '@/pages/notifications/NotificationsPage'

// Admin
import { AdminUsersPage } from '@/pages/admin/AdminUsersPage'
import { AdminSettingsPage } from '@/pages/admin/AdminSettingsPage'
import { LoginAuditPage } from '@/pages/admin/LoginAuditPage'
import { SystemLogsPage } from '@/pages/admin/SystemLogsPage'

// Profile
import { ProfilePage } from '@/pages/profile/ProfilePage'

// --- Simple inline error / fallback pages ---
import { UnauthorizedPage } from '@/pages/status/UnauthorizedPage'
import { ForbiddenPage } from '@/pages/status/ForbiddenPage'
import { InternalServerErrorPage } from '@/pages/status/InternalServerErrorPage'
import { ServiceUnavailablePage } from '@/pages/status/ServiceUnavailablePage'
import { NotFoundPage } from '@/pages/status/NotFoundPage'

type Role = 'broker' | 'client' | 'admin' // placeholder until Auth0 is integrated

export default function App() {
  // TODO: derive from Auth0 / backend
  const role: Role = 'broker' // placeholder until Auth0 is integrated

  const defaultRouteForRole: Record<Role, string> = {
    broker: '/dashboard/broker',
    client: '/dashboard/client',
    admin: '/dashboard/admin',
  }

  return (
    <Routes>
      {/* Default redirect based on role */}
      <Route
        path="/"
        element={<Navigate to={defaultRouteForRole[role]} replace />}
      />

      {/* Dashboards */}
      <Route
        path="/dashboard/broker"
        element={
          <AppShell>
            <BrokerDashboardPage />
          </AppShell>
        }
      />
      <Route
        path="/dashboard/client"
        element={
          <AppShell>
            <ClientDashboardPage />
          </AppShell>
        }
      />
      <Route
        path="/dashboard/admin"
        element={
          <AppShell>
            <AdminDashboardPage />
          </AppShell>
        }
      />

      {/* Broker transactions list */}
      <Route
        path="/transactions"
        element={
          <AppShell>
            <BrokerTransactionsPage />
          </AppShell>
        }
      />

      {/* Create transaction (broker) */}
      <Route
        path="/transactions/new"
        element={
          <AppShell>
            <CreateTransactionPage />
          </AppShell>
        }
      />

      <Route
        path="/transactions/:transactionId"
        element={
          <AppShell>
            <BrokerTransactionDetailsPage />
          </AppShell>
        }
      />

      {/* Client transactions */}
      <Route
        path="/my-transaction"
        element={
          <AppShell>
            <ClientTransactionsPage />
          </AppShell>
        }
      />

      <Route
        path="/my-transaction/:transactionId"
        element={
          <AppShell>
            <ClientTransactionDetailsPage />
          </AppShell>
        }
      />

      {/* Documents (broker + client) */}
      <Route
        path="/documents"
        element={
          <AppShell>
            <DocumentsPage />
          </AppShell>
        }
      />
      <Route
        path="/transactions/:transactionId/documents"
        element={
          <AppShell>
            <TransactionDocumentsPage />
          </AppShell>
        }
      />


      {/* Appointments */}
      <Route
        path="/appointments"
        element={
          <AppShell>
            <AppointmentsPage />
          </AppShell>
        }
      />

      {/* Analytics */}
      <Route
        path="/analytics"
        element={
          <AppShell>
            <AnalyticsPage />
          </AppShell>
        }
      />

      {/* Clients */}
      <Route
        path="/clients"
        element={
          <AppShell>
            <ClientsPage />
          </AppShell>
        }
      />

      {/* Notifications */}
      <Route
        path="/notifications"
        element={
          <AppShell>
            <NotificationsPage />
          </AppShell>
        }
      />

      {/* Admin */}
      <Route
        path="/admin/users"
        element={
          <AppShell>
            <AdminUsersPage />
          </AppShell>
        }
      />
      <Route
        path="/admin/settings"
        element={
          <AppShell>
            <AdminSettingsPage />
          </AppShell>
        }
      />
      <Route
        path="/admin/login-audit"
        element={
          <AppShell>
            <LoginAuditPage />
          </AppShell>
        }
      />
      <Route
        path="/admin/system-logs"
        element={
          <AppShell>
            <SystemLogsPage />
          </AppShell>
        }
      />

      {/* Profile */}
      <Route
        path="/profile"
        element={
          <AppShell>
            <ProfilePage />
          </AppShell>
        }
      />

      {/* Feedback and UI componenents showcase */}
      <Route
        path="/dev/showcase"
        element={
          <AppShell>
            <ShowcasePage />
          </AppShell>
        }
      />

      {/* Error / status pages used by axios error handler */}
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
  )
}
