import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'

const resources = {
  en: {
    common: {
      appName: 'CourtierPro',
      nav: {
        dashboard: 'Dashboard',
        transactions: 'Transactions',
        admin: 'Admin',
      },
    },
  },
  fr: {
    common: {
      appName: 'CourtierPro',
      nav: {
        dashboard: 'Tableau de bord',
        transactions: 'Transactions',
        admin: 'Admin',
      },
    },
  },
}

i18n.use(initReactI18next).init({
  resources,
  lng: 'en', // TODO: derive from user/org settings
  fallbackLng: 'en',
  ns: ['common'],
  defaultNS: 'common',
  interpolation: {
    escapeValue: false,
  },
})

export default i18n
