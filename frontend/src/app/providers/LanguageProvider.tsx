import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useAuth0 } from "@auth0/auth0-react";
import { getPreferredLanguage } from "@/features/auth/roleUtils";
import { LanguageContext } from "@/app/providers/LanguageContext";

export function LanguageProvider({ children }: { children: React.ReactNode }) {
    const authDisabled = import.meta.env.VITE_AUTH_DISABLED === "true";

    const { i18n } = useTranslation("common");
    const { user, isLoading } = useAuth0();

    const [language, setLanguage] = useState<"en" | "fr">("en");

    useEffect(() => {
        if (authDisabled) return;

        if (!isLoading && user) {
            const pref = getPreferredLanguage(user);
            // Only update if different to avoid unnecessary renders/loops
            if (pref !== i18n.language) {
                setLanguage(pref);
                i18n.changeLanguage(pref);
            }
        }
        // We only want to run this when the user loads or changes, not when i18n changes manually
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [authDisabled, user?.sub, isLoading]);

    const handleSetLanguage = (lang: "en" | "fr") => {
        setLanguage(lang);
        i18n.changeLanguage(lang);
    };

    if (authDisabled) {
        const staticValue = {
            language: "en" as "en" | "fr",
            // cast a no-op to the proper function type to avoid unused var lint
            setLanguage: (() => {}) as (lang: "en" | "fr") => void,
        };

        return (
            <LanguageContext.Provider value={staticValue}>
                {children}
            </LanguageContext.Provider>
        );
    }

    return (
        <LanguageContext.Provider value={{ language, setLanguage: handleSetLanguage }}>
            {children}
        </LanguageContext.Provider>
    );
}
