import { useState, useEffect, useRef } from "react";
import { Globe, Menu, ChevronDown, Search } from "lucide-react";
import { useSearchContext } from "@/features/search/context/useSearchContext";
import { useTranslation } from "react-i18next";
import { NotificationPopover } from '@/features/notifications/components/NotificationPopover';
import { Button } from "@/shared/components/ui/button";
import { Badge } from "@/shared/components/ui/badge";
import { ModeToggle } from "@/shared/components/ui/mode-toggle";
import { ProfileModal } from "@/features/profile";
import { useUserProfile } from "@/features/profile";

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
  onLogout,
}: TopNavProps) {
  const { t } = useTranslation("topnav");
  const { setIsOpen } = useSearchContext();
  const { data: user, isLoading: isUserLoading } = useUserProfile();

  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isProfileModalOpen, setIsProfileModalOpen] = useState(false);

  const userMenuRef = useRef<HTMLDivElement>(null);

  // Compute user initials from profile data
  const userInitials = user
    ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
    : '';

  // Show loading dot animation when user is loading
  const avatarContent = isUserLoading
    ? <span className="animate-pulse">•••</span>
    : (userInitials || '?');

  // Close dropdowns when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
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

  const toggleLanguage = () => {
    const newLang = language === "en" ? "fr" : "en";
    onLanguageChange(newLang);
  };

  const toggleUserMenu = () => {
    setIsUserMenuOpen((prev) => !prev);
  };

  const handleLogoutClick = () => {
    setIsUserMenuOpen(false);
    onLogout();
  };

  const handleProfile = () => {
    setIsUserMenuOpen(false);
    setIsProfileModalOpen(true);
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
      <div className="flex items-center">
        {/* User Role Badge (Desktop) - Fixed width to prevent shifting "Broker" vs "Courtier" */}
        {user && (
          <div className="hidden md:flex items-center justify-end w-36">
            <Badge
              variant={
                user.role === 'ADMIN' ? 'destructive' :
                  user.role === 'CLIENT' ? 'secondary' : 'default'
              }
            >
              {t(`role${user.role}`)}
            </Badge>
          </div>
        )}

        {/* Search Trigger (Fixed width container) */}
        <div className="flex items-center justify-center w-10 md:w-72">
          <Button
            variant="outline"
            className="relative h-9 w-9 p-0 md:h-9 md:w-60 md:justify-start md:px-3 md:text-muted-foreground"
            onClick={() => setIsOpen(true)}
          >
            <Search className="h-4 w-4 md:mr-2" />
            <span className="hidden md:inline-flex">{t("searchPlaceholder", "Search...")}</span>
            <kbd className="pointer-events-none absolute right-1.5 top-1.5 hidden h-6 select-none items-center gap-1 rounded border bg-muted px-1.5 font-mono text-[10px] font-medium opacity-100 md:flex">
              <span className="text-xs">⌘</span>K
            </kbd>
          </Button>
        </div>

        {/* Mode Toggle */}
        <div className="flex items-center justify-center w-10 md:w-20">
          <ModeToggle />
        </div>

        {/* Language selector toggle */}
        <div className="flex items-center justify-center w-12 md:w-20">
          <Button
            variant="ghost"
            onClick={toggleLanguage}
            className="flex items-center gap-2 px-2 py-2 text-foreground"
            aria-label={t("language")}
          >
            <Globe className="w-5 h-5" />
            <span className="hidden sm:inline-block w-6 text-center">
              {language === "en" ? "FR" : "EN"}
            </span>
          </Button>
        </div>

        {/* Notifications */}
        <div className="flex items-center justify-center w-10 md:w-12">
          <NotificationPopover />
        </div>

        {/* User avatar + menu */}
        <div className="flex items-center justify-center w-12 md:w-24 relative" ref={userMenuRef}>
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
              <span>{avatarContent}</span>
            </div>
            <ChevronDown className="hidden h-4 w-4 sm:block" />
          </Button>
          {isUserMenuOpen && (
            <div
              className="absolute top-full right-0 mt-2 w-48 overflow-hidden rounded-lg border border-border bg-popover text-popover-foreground shadow-lg"
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

      {/* Profile Modal */}
      <ProfileModal
        isOpen={isProfileModalOpen}
        onClose={() => setIsProfileModalOpen(false)}
      />
    </nav>
  );
}
