import { createContext, useContext } from "react";

type LanguageContextType = {
    language: "en" | "fr";
    setLanguage: (lang: "en" | "fr") => void;
};

export const LanguageContext = createContext<LanguageContextType | undefined>(undefined);

export function useLanguage() {
    const context = useContext(LanguageContext);
    if (!context) {
        throw new Error("useLanguage must be used within a LanguageProvider");
    }
    return context;
}
