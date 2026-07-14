import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

import commonRu from './locales/ru/common.json';
import authRu from './locales/ru/auth.json';
import quizRu from './locales/ru/quiz.json';
import dashboardRu from './locales/ru/dashboard.json';
import settingsRu from './locales/ru/settings.json';
import groupsRu from './locales/ru/groups.json';
import adminRu from './locales/ru/admin.json';
import profileRu from './locales/ru/profile.json';
import grammarRu from './locales/ru/grammar.json';
import sangrahaRu from './locales/ru/sangraha.json';
import curriculumRu from './locales/ru/curriculum.json';
import commonEn from './locales/en/common.json';
import authEn from './locales/en/auth.json';
import quizEn from './locales/en/quiz.json';
import dashboardEn from './locales/en/dashboard.json';
import settingsEn from './locales/en/settings.json';
import groupsEn from './locales/en/groups.json';
import adminEn from './locales/en/admin.json';
import profileEn from './locales/en/profile.json';
import grammarEn from './locales/en/grammar.json';
import sangrahaEn from './locales/en/sangraha.json';
import curriculumEn from './locales/en/curriculum.json';

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {
      en: {
        translation: {
          ...commonEn,
          ...authEn,
          ...quizEn,
          ...dashboardEn,
          ...settingsEn,
          ...groupsEn,
          ...adminEn,
          ...profileEn,
          ...grammarEn,
          ...sangrahaEn,
          ...curriculumEn,
        },
      },
      ru: {
        translation: {
          ...commonRu,
          ...authRu,
          ...quizRu,
          ...dashboardRu,
          ...settingsRu,
          ...groupsRu,
          ...adminRu,
          ...profileRu,
          ...grammarRu,
          ...sangrahaRu,
          ...curriculumRu,
        },
      },
    },
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false,
    },
  });

export default i18n;

