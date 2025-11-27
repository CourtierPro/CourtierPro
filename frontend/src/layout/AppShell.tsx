import { type ReactNode, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Sidebar } from "@/components/Sidebar";
import { TopNav } from "@/components/TopNav";

type AppShellProps = {
  children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
  const { i18n } = useTranslation("common");
  const navigate = useNavigate();
  const location = useLocation();

  // TODO: derive from Auth0 claims
  const [userRole] = useState<"broker" | "client" | "admin">("broker");

  const [language, setLanguage] = useState<"en" | "fr">(
    (i18n.language as "en" | "fr") ?? "en"
  );
  const [sidebarOpen, setSidebarOpen] = useState(true);

  const handleLanguageChange = (lang: "en" | "fr") => {
    setLanguage(lang);
    i18n.changeLanguage(lang);
  };

  const handleNavigate = (route: string) => {
    navigate(route);
  };

  const handleLogout = () => {
    // TODO: integrate with Auth0 logout
    console.log("TODO: logout and redirect to login");
  };

  return (
    <div className="min-h-screen flex bg-background text-foreground">
      {/* Sidebar */}
      <Sidebar
        isOpen={sidebarOpen}
        onClose={() => setSidebarOpen(false)}
        language={language}
        userRole={userRole}
        currentRoute={location.pathname}
        onNavigate={handleNavigate}
      />

      {/* Main area */}
      <div className="flex-1 flex flex-col min-w-0">
        <TopNav
          onMenuToggle={() => setSidebarOpen((prev) => !prev)}
          language={language}
          onLanguageChange={handleLanguageChange}
          userRole={userRole}
          onLogout={handleLogout}
          onNavigate={handleNavigate}
        />

        <main className="flex-1 bg-muted/40 p-4 overflow-auto">{children}</main>
      </div>
    </div>
  );
}
