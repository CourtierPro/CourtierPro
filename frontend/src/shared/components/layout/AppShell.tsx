
import { type ReactNode, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth0 } from "@auth0/auth0-react";

import { Sidebar } from "@/shared/components/layout/Sidebar";
import { TopNav } from "@/shared/components/layout/TopNav";
import { ErrorBoundary } from "@/shared/components/error/ErrorBoundary";
import {
    getRoleFromUser,
    type AppRole,
} from "@/features/auth/roleUtils";
import { useLanguage } from '@/app/providers/LanguageContext';

type AppShellProps = {
    children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
    const navigate = useNavigate();
    const location = useLocation();
    const { language, setLanguage } = useLanguage();

    const {
        user,
        logout,
    } = useAuth0();

    // Role of the user from the Auth0 token
    const userRole: AppRole = getRoleFromUser(user) ?? "broker";

    const [sidebarOpen, setSidebarOpen] = useState(true);

    const handleNavigate = (route: string) => {
        navigate(route);
    };

    const handleLogout = () => {
        logout({
            logoutParams: {
                returnTo: window.location.origin,
            },
        });
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

            {/* Main area */}
            <div className="flex-1 flex flex-col min-w-0">
                <TopNav
                    onMenuToggle={() => setSidebarOpen((prev) => !prev)}
                    language={language}
                    onLanguageChange={setLanguage}
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
