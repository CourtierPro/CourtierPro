// src/layout/AppShell.tsx
import { type ReactNode, useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useAuth0 } from "@auth0/auth0-react";

import { Sidebar } from "@/components/Sidebar";
import { TopNav } from "@/components/TopNav";
import {
    getPreferredLanguage,
    getRoleFromUser,
    type AppRole,
} from "@/auth/roleUtils";
import { registerAccessTokenProvider } from "@/api/axiosInstance";

type AppShellProps = {
    children: ReactNode;
};

export function AppShell({ children }: AppShellProps) {
    const { i18n } = useTranslation("common");
    const navigate = useNavigate();
    const location = useLocation();

    const {
        user,
        logout,
        getAccessTokenSilently,
        isAuthenticated,
    } = useAuth0();

    // Role of the user from the Auth0 token
    const userRole: AppRole = getRoleFromUser(user) ?? "broker";

    // Initial language from the claim, otherwise "en"
    const initialLang = getPreferredLanguage(user);
    const [language, setLanguage] = useState<"en" | "fr">(initialLang);

    const [sidebarOpen, setSidebarOpen] = useState(true);

    //  here we connect Auth0 -> axios
    useEffect(() => {
        registerAccessTokenProvider(async () => {
            if (!isAuthenticated) return undefined;

            const token = await getAccessTokenSilently({
                authorizationParams: {
                    // // must match what your backend expects as an audience
                    audience: import.meta.env.VITE_AUTH0_AUDIENCE,
                },
            });

            return token;
        });
    }, [getAccessTokenSilently, isAuthenticated]);

    const handleLanguageChange = (lang: "en" | "fr") => {
        setLanguage(lang);
        i18n.changeLanguage(lang);
    };

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

                <main className="flex-1 bg-muted/40 p-4 overflow-auto">
                    {children}
                </main>
            </div>
        </div>
    );
}
