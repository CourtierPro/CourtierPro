import { type ReactNode } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

type AppShellProps = {
  children: ReactNode
}

export function AppShell({ children }: AppShellProps) {
  const { t } = useTranslation('common')
  const location = useLocation()

  const navLinkClasses = (path: string) =>
    [
      'px-3 py-1.5 rounded-md text-sm font-medium',
      location.pathname.startsWith(path)
        ? 'bg-slate-900 text-white'
        : 'text-slate-700 hover:bg-slate-100',
    ].join(' ')

  return (
    <div className="min-h-screen flex flex-col">
      <header className="border-b bg-white px-4 py-2 flex items-center justify-between">
        <div className="font-semibold text-slate-900">{t('appName')}</div>
        <nav className="flex gap-2">
          <Link to="/dashboard/broker" className={navLinkClasses('/dashboard')}>
            {t('nav.dashboard')}
          </Link>
          <Link to="/transactions" className={navLinkClasses('/transactions')}>
            {t('nav.transactions')}
          </Link>
          <Link to="/admin/users" className={navLinkClasses('/admin')}>
            {t('nav.admin')}
          </Link>
        </nav>
      </header>

      <main className="flex-1 bg-slate-50 p-4">{children}</main>
    </div>
  )
}
