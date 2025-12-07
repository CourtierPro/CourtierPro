import { type ReactNode, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";
import { useTranslation } from "react-i18next";

import { Sidebar } from "@/shared/components/layout/Sidebar";
import { TopNav } from "@/shared/components/layout/TopNav";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";

import {
  getRoleFromUser,
  getPreferredLanguage,
  type AppRole,
} from "@/features/auth/roleUtils";

import { registerAccessTokenProvider } from "@/shared/api/axiosInstance";
import { useLanguage } from "@/app/providers/LanguageContext";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { i18n } = useTranslation("common");

  // language context
  const { language, setLanguage } = useLanguage();

  const { user, logout, getAccessTokenSilently, isAuthenticated } = useAuth0();

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

  const handleLogout = () => logout({ logoutParams: { returnTo: window.location.origin } });

  const handleLanguageChange = (lang: "en" | "fr") => {
    setLanguage(lang);
    i18n.changeLanguage(lang);
  };

  return (
    <div className="min-h-screen flex bg-background text-foreground pt-16">
      {/* Sidebar */}
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        language={language}
        userRole={userRole}
        currentRoute={location.pathname}
        onNavigate={handleNavigate}
      />

      {/* Main content */}
      <div className="flex-1 flex flex-col min-w-0">
        <TopNav
          onMenuToggle={() => setSidebarOpen((prev) => !prev)}
          language={language}
          onLanguageChange={handleLanguageChange}
          userRole={userRole}
          onLogout={handleLogout}
          onNavigate={handleNavigate}
        />

        <main className="flex-1 bg-muted/40 p-4 overflow-auto">
          <ErrorBoundary key={location.pathname}>
            {children}
          </ErrorBoundary>
        </main>
      </div>
    </div>
  );
}
