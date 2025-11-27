import { Routes, Route, Navigate } from 'react-router-dom'
import { AppShell } from './layout/AppShell'

function BrokerDashboardPage() {
  return <div className="text-lg font-semibold">Broker dashboard (My Day + summaries)</div>
}

function ClientDashboardPage() {
  return <div className="text-lg font-semibold">Client dashboard</div>
}

function AdminDashboardPage() {
  return <div className="text-lg font-semibold">Admin dashboard</div>
}

function TransactionsPage() {
  return <div className="text-lg font-semibold">Transactions list</div>
}

function TransactionPage() {
  return (
    <div className="text-lg font-semibold">
      Transaction details (timeline + documents + appointments)
    </div>
  )
}

function AdminUsersPage() {
  return <div className="text-lg font-semibold">Admin – user management</div>
}

function AdminOrgSettingsPage() {
  return <div className="text-lg font-semibold">Admin – organization settings</div>
}

function NotFoundPage() {
  return <div className="text-lg font-semibold">404 – Page not found</div>
}

function UnauthorizedPage() {
  return <div className="text-lg font-semibold">401 – Unauthorized</div>;
}

function ForbiddenPage() {
  return <div className="text-lg font-semibold">403 – Forbidden</div>;
}

function InternalServerErrorPage() {
  return (
    <div className="text-lg font-semibold">500 – Internal server error</div>
  );
}

function ServiceUnavailablePage() {
  return (
    <div className="text-lg font-semibold">503 – Service unavailable</div>
  );
}

type Role = 'broker' | 'client' | 'admin'; //placeholder until Auth0 is integrated

export default function App() {
  // TODO: derive from Auth0 / backend

  const role: Role = 'broker' //placeholder until Auth0 is integrated

  const defaultRouteForRole: Record<Role, string> = {
    broker: '/dashboard/broker',
    client: '/dashboard/client',
    admin: '/dashboard/admin',
  }

  return (
    <Routes>
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

      {/* Transactions */}
      <Route
        path="/transactions"
        element={
          <AppShell>
            <TransactionsPage />
          </AppShell>
        }
      />
      <Route
        path="/transactions/:transactionId"
        element={
          <AppShell>
            <TransactionPage />
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
            <AdminOrgSettingsPage />
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

    </Routes>
  )
}
