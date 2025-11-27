import i18n from "i18next";
import { initReactI18next } from "react-i18next";

// Import namespaces for EN
import commonEn from "@/i18n/en/common.json";
import sidebarEn from "@/i18n/en/sidebar.json";
import topnavEn from "@/i18n/en/topnav.json";

// Import namespaces for FR
import commonFr from "@/i18n/fr/common.json";
import sidebarFr from "@/i18n/fr/sidebar.json";
import topnavFr from "@/i18n/fr/topnav.json";

i18n.use(initReactI18next).init({
  resources: {
    en: {
      common: commonEn,
      sidebar: sidebarEn,
      topnav: topnavEn
    },
    fr: {
      common: commonFr,
      sidebar: sidebarFr,
      topnav: topnavFr
    }
  },

  lng: "en",
  fallbackLng: "en",

  ns: ["common", "sidebar", "topnav"],
  defaultNS: "common",

  interpolation: { escapeValue: false }
});

export default i18n;
