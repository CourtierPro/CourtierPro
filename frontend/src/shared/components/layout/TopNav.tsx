import { useState, useEffect, useRef } from "react";
import { Globe, Menu, ChevronDown } from "lucide-react";
import { useTranslation } from "react-i18next";
import { NotificationPopover } from '@/features/notifications/components/NotificationPopover';
import { Button } from "@/shared/components/ui/button";
import { ModeToggle } from "@/shared/components/ui/mode-toggle";

interface NotificationItem {
  id: string | number;
  title: string;
  description: string;
  timestamp: string;
  unread: boolean;
  icon?: React.ComponentType<{ className?: string }>;
}

interface TopNavProps {
  onMenuToggle: () => void;
  language: "en" | "fr";
  onLanguageChange: (lang: "en" | "fr") => void;
  userRole: "broker" | "client" | "admin";
  onLogout: () => void;
  onNavigate: (route: string) => void;
  notifications?: NotificationItem[];
  unreadCount?: number;
}

export function TopNav({
  onMenuToggle,
  language,
  onLanguageChange,
  userRole,
  onLogout,
  onNavigate,
}: TopNavProps) {
  const { t } = useTranslation("topnav");

  const [isLanguageMenuOpen, setIsLanguageMenuOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);

  const languageRef = useRef<HTMLDivElement>(null);
  const userMenuRef = useRef<HTMLDivElement>(null);

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        languageRef.current &&
        !languageRef.current.contains(event.target as Node)
      ) {
        setIsLanguageMenuOpen(false);
      }
      if (
        userMenuRef.current &&
        !userMenuRef.current.contains(event.target as Node)
      ) {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  const toggleLanguageMenu = () => {
    setIsLanguageMenuOpen((prev) => !prev);
    setIsUserMenuOpen(false);
  };

  const toggleUserMenu = () => {
    setIsUserMenuOpen((prev) => !prev);
    setIsLanguageMenuOpen(false);
  };

  const selectLanguage = (lang: "en" | "fr") => {
    onLanguageChange(lang);
    setIsLanguageMenuOpen(false);
  };

  const handleLogoutClick = () => {
    setIsUserMenuOpen(false);
    onLogout();
  };

  const handleProfile = () => {
    setIsUserMenuOpen(false);
    onNavigate("/profile");
  };

  return (
    <nav
      className="fixed top-0 left-0 right-0 z-50 flex h-16 items-center justify-between border-b border-border bg-background px-4 md:px-6"
      role="banner"
      aria-label="Main navigation"
      style={{ viewTransitionName: 'topnav' } as React.CSSProperties}
    >
      {/* Left section */}
      <div className="flex items-center gap-4">
        {/* Mobile menu toggle */}
        <Button
          variant="ghost"
          size="icon"
          onClick={onMenuToggle}
          className="md:hidden"
          aria-label={t("menu")}
          aria-expanded="false"
        >
          <Menu className="w-6 h-6 text-foreground" />
        </Button>

        {/* Logo */}
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary">
            <span className="text-primary-foreground font-semibold">CP</span>
          </div>
          <span className="hidden sm:block text-foreground font-medium">
            CourtierPro
          </span>
        </div>
      </div>

      {/* Right section */}
      <div className="flex items-center gap-2 md:gap-4">
        <ModeToggle />
        {/* Language selector */}
        <div className="relative" ref={languageRef}>
          <Button
            variant="ghost"
            onClick={toggleLanguageMenu}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                toggleLanguageMenu();
              }
              if (e.key === "Escape") {
                setIsLanguageMenuOpen(false);
              }
            }}
            className="flex items-center gap-2 px-3 py-2 text-foreground"
            aria-label={t("language")}
            aria-expanded={isLanguageMenuOpen}
            aria-haspopup="true"
          >
            <Globe className="w-5 h-5" />
            <span className="hidden sm:inline">
              {language.toUpperCase()}
            </span>
          </Button>
          {isLanguageMenuOpen && (
            <div
              className="absolute right-0 mt-2 w-32 overflow-hidden rounded-lg border border-border bg-popover text-popover-foreground shadow-lg"
              role="menu"
              aria-label={t("language")}
            >
              <button
                onClick={() => selectLanguage("en")}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    selectLanguage("en");
                  }
                }}
                className={`w-full px-4 py-2 text-left hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:outline-none ${language === "en" ? "bg-accent/50" : ""
                  }`}
                role="menuitem"
              >
                English
              </button>
              <button
                onClick={() => selectLanguage("fr")}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    selectLanguage("fr");
                  }
                }}
                className={`w-full px-4 py-2 text-left hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:outline-none ${language === "fr" ? "bg-accent/50" : ""
                  }`}
                role="menuitem"
              >
                Français
              </button>
            </div>
          )}
        </div>

        {/* Notifications */}
        <NotificationPopover />

        {/* User avatar + menu */}
        <div className="relative" ref={userMenuRef}>
          <Button
            variant="ghost"
            onClick={toggleUserMenu}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") {
                e.preventDefault();
                toggleUserMenu();
              }
              if (e.key === "Escape") {
                setIsUserMenuOpen(false);
              }
            }}
            className="flex items-center gap-2 p-1 text-foreground"
            aria-label={t("userMenu")}
            aria-expanded={isUserMenuOpen}
            aria-haspopup="true"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-semibold">
              <span>
                {userRole === "broker"
                  ? "B"
                  : userRole === "client"
                    ? "C"
                    : "A"}
              </span>
            </div>
            <ChevronDown className="hidden h-4 w-4 sm:block" />
          </Button>
          {isUserMenuOpen && (
            <div
              className="absolute right-0 mt-2 w-48 overflow-hidden rounded-lg border border-border bg-popover text-popover-foreground shadow-lg"
              role="menu"
              aria-label={t("userMenu")}
            >
              <button
                onClick={handleProfile}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    handleProfile();
                  }
                }}
                className="w-full px-4 py-3 text-left text-sm hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:outline-none"
                role="menuitem"
              >
                {t("profile")}
              </button>
              <button
                onClick={handleLogoutClick}
                onKeyDown={(e) => {
                  if (e.key === "Enter" || e.key === " ") {
                    e.preventDefault();
                    handleLogoutClick();
                  }
                }}
                className="w-full px-4 py-3 text-left text-sm hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:outline-none"
                role="menuitem"
              >
                {t("logout")}
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}
