import React from "react";
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
} from "lucide-react";
import { useTranslation } from "react-i18next";
import { cn } from "@/shared/utils/utils";

interface SidebarProps {
  isOpen: boolean;
  onClose: () => void;
  language: "en" | "fr"; // currently unused, but kept for compatibility with AppShell
  userRole: "broker" | "client" | "admin";
  currentRoute: string;
  onNavigate: (route: string) => void;
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
  currentRoute,
  onNavigate,
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
            label: t("loginAudit"),
            route: "/admin/login-audit",
            icon: <Shield size={iconSize} />,
          },
          {
            label: t("systemLogs"),
            route: "/admin/system-logs",
            icon: <Activity size={iconSize} />,
          },
        ];
      default:
        return [];
    }
  };

  const navItems = getNavItems();

  const handleNavigation = (route: string) => {
    onNavigate(route);
    if (typeof window !== "undefined" && window.innerWidth < 768) {
      onClose();
    }
  };

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
        className={`fixed top-0 left-0 z-40 h-full w-64 bg-white border-r border-slate-200 transition-transform duration-300 ease-in-out ${isOpen ? "translate-x-0" : "-translate-x-full"
          } md:translate-x-0 md:sticky md:top-16 md:h-[calc(100vh-4rem)]`}
        role="navigation"
        aria-label="Sidebar navigation"
      >
        <div className="flex h-full flex-col pt-16 md:pt-0">
          {/* Mobile header */}
          <div className="flex items-center justify-between p-4 md:hidden">
            <span className="text-slate-800">{t("menuLabel")}</span>
            <button
              onClick={onClose}
              className="p-2 rounded-lg hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 transition-colors"
              aria-label={t("closeSidebar")}
            >
              <ChevronLeft className="w-5 h-5 text-slate-800" />
            </button>
          </div>

          {/* Navigation items */}
          <nav className="flex-1 px-4 py-6 overflow-y-auto">
            <ul className="space-y-2" role="list">
              {navItems.map((item) => {
                const isActive = currentRoute === item.route;

                return (
                  <li key={item.route}>
                    <button
                      onClick={() => handleNavigation(item.route)}
                      className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-orange-500 focus:ring-offset-2 ${isActive
                        ? "bg-orange-500 text-white hover:bg-orange-600"
                        : "text-slate-700 hover:bg-slate-100"
                        }`}
                      aria-current={isActive ? "page" : undefined}
                    >
                      <span className="flex items-center">
                        {item.icon &&
                          React.cloneElement(item.icon, {
                            className: cn(
                              "w-5 h-5 shrink-0",
                              (item.icon.props as { className?: string }).className,
                            ),
                          })}
                      </span>
                      <span>{item.label}</span>
                    </button>
                  </li>
                );
              })}
            </ul>
          </nav>
        </div>
      </aside>
    </>
  );
}
