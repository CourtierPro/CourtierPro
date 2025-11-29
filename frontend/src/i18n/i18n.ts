import i18n from "i18next";
import { initReactI18next } from "react-i18next";

// Import namespaces for EN
import commonEn from "@/i18n/en/common.json";
import sidebarEn from "@/i18n/en/sidebar.json";
import topnavEn from "@/i18n/en/topnav.json";
import transactionsEn from "@/i18n/en/transactions.json";

// Import namespaces for FR
import commonFr from "@/i18n/fr/common.json";
import sidebarFr from "@/i18n/fr/sidebar.json";
import topnavFr from "@/i18n/fr/topnav.json";
import transactionsFr from "@/i18n/fr/transactions.json";

i18n.use(initReactI18next).init({
  resources: {
    en: {
      common: commonEn,
      sidebar: sidebarEn,
      topnav: topnavEn
      ,transactions: transactionsEn
    },
    fr: {
      common: commonFr,
      sidebar: sidebarFr,
      topnav: topnavFr
      ,transactions: transactionsFr
    }
  },

  lng: "en",
  fallbackLng: "en",

  ns: ["common", "sidebar", "topnav"],
  defaultNS: "common",

  interpolation: { escapeValue: false }
});

export default i18n;
