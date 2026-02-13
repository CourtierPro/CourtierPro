import { type ReactNode, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import { useTranslation } from "react-i18next";
import { useArrowNavigation } from "@/shared/hooks/useArrowNavigation";

import { Sidebar } from "@/shared/components/layout/Sidebar";
import { TopNav } from "@/shared/components/layout/TopNav";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";
import { GlobalSearch } from "@/shared/components/branded/GlobalSearch";

import {
  getRoleFromUser,
  getPreferredLanguage,
  type AppRole,
} from "@/features/auth/roleUtils";

import { registerAccessTokenProvider } from "@/shared/api/axiosInstance";
import { useLanguage } from "@/app/providers/LanguageContext";
import { useSessionTimeout } from "@/features/auth/hooks/useSessionTimeout";
import { useLogout } from "@/features/auth/hooks/useLogout";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { t, i18n } = useTranslation("common");

  // Enable global arrow key navigation
  useArrowNavigation();

  // language context
  const { language, setLanguage } = useLanguage();

  const { user, getAccessTokenSilently, isAuthenticated } = useAuth0();

  // Centralized logout with event logging
  const logout = useLogout();

  // Session timeout - auto logout after 30 minutes of inactivity
  useSessionTimeout({
    timeout: 30 * 60 * 1000, // 30 minutes
    onTimeout: () => logout({ reason: 'session_timeout' }),
    enabled: isAuthenticated,
  });

  // role
  const userRole: AppRole = getRoleFromUser(user) ?? "broker";

  // initial language from user claim
  const initialLang = getPreferredLanguage(user);

  // ensure LanguageContext syncs with user preferred language
  useEffect(() => {
    if (initialLang && initialLang !== language) {
      setLanguage(initialLang);
      i18n.changeLanguage(initialLang);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [initialLang]);

  const [sidebarOpen, setSidebarOpen] = useState(true);

  // axios auth
  useEffect(() => {
    registerAccessTokenProvider(async () => {
      if (!isAuthenticated) return undefined;

      return await getAccessTokenSilently({
        authorizationParams: {
          audience: import.meta.env.VITE_AUTH0_AUDIENCE,
        },
      });
    });
  }, [isAuthenticated, getAccessTokenSilently]);

  const handleNavigate = (route: string) => navigate(route);

  const handleLogout = () => logout({ reason: 'manual' });

  const handleLanguageChange = (lang: "en" | "fr") => {
    setLanguage(lang);
    i18n.changeLanguage(lang);
  };

  return (
    <div className="min-h-screen flex bg-background text-foreground pt-16">
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:absolute focus:z-[100] focus:p-4 focus:bg-background focus:text-foreground focus:outline-none focus:ring-2 focus:ring-ring"
      >
        {t("skipToContent", "Skip to main content")}
      </a>
      {/* Sidebar */}
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        language={language}
        userRole={userRole}
      />

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        <TopNav
          isMenuOpen={sidebarOpen}
          onMenuToggle={() => setSidebarOpen((prev) => !prev)}
          language={language}
          onLanguageChange={handleLanguageChange}
          userRole={userRole}
          onLogout={handleLogout}
          onNavigate={handleNavigate}
        />

        <main
          id="main-content"
          tabIndex={-1}
          className="flex-1 bg-muted/40 p-4 overflow-auto focus:outline-none"
        >
          <ErrorBoundary key={location.pathname}>
            {children}
          </ErrorBoundary>
        </main>
      </div>
      <GlobalSearch />
    </div>
  );
}
