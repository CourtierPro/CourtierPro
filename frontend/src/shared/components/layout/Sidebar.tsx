import React from "react";
import { NavLink } from "react-router-dom";
import {
  LayoutDashboard,
  FileText,
  Upload,
  Calendar,
  BarChart3,
  Users,
  Settings,
  Shield,
  ChevronLeft,
  Activity,
  Bell,
  Palette,
  MessageSquare,
  Database,
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { cn } from "@/shared/utils/utils";
import { Button } from "@/shared/components/ui/button";
import { FeedbackModal } from "@/shared/components/feedback/FeedbackModal";

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  language: "en" | "fr"; // currently unused, but kept for compatibility with AppShell
  userRole: "broker" | "client" | "admin";
}

interface NavItem {
  label: string;
  route: string;
  icon: React.ReactElement<{ className?: string }>;
}

export function Sidebar({
  isOpen,
  onClose,
  userRole,
}: SidebarProps) {
  const { t } = useTranslation("sidebar");

  const getNavItems = (): NavItem[] => {
    const iconSize = 20;

    switch (userRole) {
      case "broker":
        return [
          {
            label: t("dashboard"),
            route: "/dashboard/broker",
            icon: <LayoutDashboard size={iconSize} />,
          },
          {
            label: t("notifications"),
            route: "/notifications",
            icon: <Bell size={iconSize} />,
          },
          {
            label: t("clients"),
            route: "/clients",
            icon: <Users size={iconSize} />,
          },
          {
            label: t("transactions"),
            route: "/transactions",
            icon: <FileText size={iconSize} />,
          },
          {
            label: t("documents"),
            route: "/documents",
            icon: <Upload size={iconSize} />,
          },
          {
            label: t("appointments"),
            route: "/appointments",
            icon: <Calendar size={iconSize} />,
          },
          {
            label: t("analytics"),
            route: "/analytics",
            icon: <BarChart3 size={iconSize} />,
          },
        ];
      case "client":
        return [
          {
            label: t("dashboard"),
            route: "/dashboard/client",
            icon: <LayoutDashboard size={iconSize} />,
          },
          {
            label: t("notifications"),
            route: "/notifications",
            icon: <Bell size={iconSize} />,
          },
          {
            label: t("myTransaction"),
            route: "/my-transaction",
            icon: <FileText size={iconSize} />,
          },
          {
            label: t("myDocuments"),
            route: "/my-documents",
            icon: <Upload size={iconSize} />,
          },
          {
            label: t("appointments"),
            route: "/appointments",
            icon: <Calendar size={iconSize} />,
          },
        ];
      case "admin":
        return [
          {
            label: t("dashboard"),
            route: "/dashboard/admin",
            icon: <LayoutDashboard size={iconSize} />,
          },
          {
            label: t("notifications"),
            route: "/notifications",
            icon: <Bell size={iconSize} />,
          },
          {
            label: t("manageUsers"),
            route: "/admin/users",
            icon: <Users size={iconSize} />,
          },
          {
            label: t("orgSettings"),
            route: "/admin/settings",
            icon: <Settings size={iconSize} />,
          },
          {
            label: t("resources"),
            route: "/admin/resources",
            icon: <Database size={iconSize} />,
          },
          {
            label: t("loginAudit"),
            route: "/admin/login-audit",
            icon: <Shield size={iconSize} />,
          },
          {
            label: t("systemLogs"),
            route: "/admin/system-logs",
            icon: <Activity size={iconSize} />,
          },
          {
            label: t("showcase"),
            route: "/dev/showcase",
            icon: <Palette size={iconSize} />,
          },
        ];
      default:
        return [];
    }
  };

  const navItems = getNavItems();

  return (
    <>
      {/* Overlay for mobile */}
      {isOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/20 md:hidden"
          onClick={onClose}
          aria-hidden="true"
        />
      )}

      {/* Sidebar */}
      <aside
        id="sidebar-nav"
        className={`fixed top-0 left-0 z-40 h-full w-64 bg-sidebar border-r border-sidebar-border transition-transform duration-300 ease-in-out ${isOpen ? "translate-x-0" : "-translate-x-full"
          } md:translate-x-0 md:sticky md:top-16 md:h-[calc(100vh-4rem)]`}
        role="navigation"
        aria-label="Sidebar navigation"
        style={{ viewTransitionName: 'sidebar' } as React.CSSProperties}
      >
        <div className="flex h-full flex-col pt-16 md:pt-0">
          <div className="flex items-center justify-between p-4 md:hidden">
            <span className="text-sidebar-foreground font-semibold">{t("menuLabel")}</span>
            <Button
              variant="ghost"
              size="icon"
              onClick={onClose}
              aria-label={t("closeSidebar")}
              className="text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
            >
              <ChevronLeft className="w-5 h-5" />
            </Button>
          </div>

          {/* Navigation items */}
          <nav className="flex-1 px-4 py-6 overflow-y-auto">
            <ul className="space-y-2" role="list">
              {navItems.map((item) => (
                <li key={item.route}>
                  <NavLink
                    to={item.route}
                    onClick={() => {
                      if (typeof window !== "undefined" && window.innerWidth < 768) {
                        onClose();
                      }
                    }}
                    className={({ isActive }) =>
                      `w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2 ${isActive
                        ? "bg-sidebar-primary text-sidebar-primary-foreground hover:bg-sidebar-primary/90"
                        : "text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground"
                      }`
                    }
                  >
                    {({ isActive }) => (
                      <>
                        <span className="flex items-center">
                          {item.icon &&
                            React.cloneElement(item.icon, {
                              className: cn(
                                "w-5 h-5 shrink-0",
                                (item.icon.props as { className?: string }).className,
                              ),
                            })}
                        </span>
                        <span className="whitespace-nowrap">{item.label}</span>
                        {/* Hidden text for screen readers if needed to indicate current page */}
                        {isActive && <span className="sr-only">(Current)</span>}
                      </>
                    )}
                  </NavLink>
                </li>
              ))}
            </ul>
          </nav>

          {/* Feedback button at bottom */}
          <div className="px-4 py-4 border-t border-sidebar-border">
            <FeedbackModal
              trigger={
                <button
                  className="w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors text-sidebar-foreground hover:bg-sidebar-accent hover:text-sidebar-accent-foreground focus:outline-none focus:ring-2 focus:ring-primary focus:ring-offset-2"
                >
                  <MessageSquare className="w-5 h-5 shrink-0" />
                  <span className="whitespace-nowrap">{t("button", { ns: "feedback" })}</span>
                </button>
              }
            />
          </div>
        </div>
      </aside>
    </>
  );
}
