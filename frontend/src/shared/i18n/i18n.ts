import i18n from "i18next";
import { initReactI18next } from "react-i18next";

// Import namespaces for EN
import commonEn from "@/shared/i18n/en/common.json";
import sidebarEn from "@/shared/i18n/en/sidebar.json";
import topnavEn from "@/shared/i18n/en/topnav.json";
import transactionsEn from "@/shared/i18n/en/transactions.json";
import appointmentsEn from "@/shared/i18n/en/appointments.json";
import documentsEn from "@/shared/i18n/en/documents.json";
import dashboardEn from "@/shared/i18n/en/dashboard.json";
import adminEn from "@/shared/i18n/en/admin.json";
import clientsEn from "@/shared/i18n/en/clients.json";
import profileEn from "@/shared/i18n/en/profile.json";
import analyticsEn from "@/shared/i18n/en/analytics.json";
import notificationsEn from "@/shared/i18n/en/notifications.json";
import statusEn from "@/shared/i18n/en/status.json";

// Import namespaces for FR
import commonFr from "@/shared/i18n/fr/common.json";
import sidebarFr from "@/shared/i18n/fr/sidebar.json";
import topnavFr from "@/shared/i18n/fr/topnav.json";
import transactionsFr from "@/shared/i18n/fr/transactions.json";
import appointmentsFr from "@/shared/i18n/fr/appointments.json";
import documentsFr from "@/shared/i18n/fr/documents.json";
import dashboardFr from "@/shared/i18n/fr/dashboard.json";
import adminFr from "@/shared/i18n/fr/admin.json";
import clientsFr from "@/shared/i18n/fr/clients.json";
import profileFr from "@/shared/i18n/fr/profile.json";
import analyticsFr from "@/shared/i18n/fr/analytics.json";
import notificationsFr from "@/shared/i18n/fr/notifications.json";
import statusFr from "@/shared/i18n/fr/status.json";

i18n.use(initReactI18next).init({
  resources: {
    en: {
      common: commonEn,
      sidebar: sidebarEn,
      topnav: topnavEn,
      transactions: transactionsEn,
      appointments: appointmentsEn,
      documents: documentsEn,
      dashboard: dashboardEn,
      admin: adminEn,
      clients: clientsEn,
      profile: profileEn,
      analytics: analyticsEn,
      notifications: notificationsEn,
      status: statusEn
    },
    fr: {
      common: commonFr,
      sidebar: sidebarFr,
      topnav: topnavFr,
      transactions: transactionsFr,
      appointments: appointmentsFr,
      documents: documentsFr,
      dashboard: dashboardFr,
      admin: adminFr,
      clients: clientsFr,
      profile: profileFr,
      analytics: analyticsFr,
      notifications: notificationsFr,
      status: statusFr
    }
  },

  lng: "en",
  fallbackLng: "en",

  ns: ["common", "sidebar", "topnav", "transactions", "appointments", "documents", "dashboard", "admin", "clients", "profile", "analytics", "notifications", "status"],
  defaultNS: "common",

  interpolation: { escapeValue: false }
});

export default i18n;
