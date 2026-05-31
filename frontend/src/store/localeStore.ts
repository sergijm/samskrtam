import { create } from 'zustand';
import i18next from 'i18next';
import { persist } from 'zustand/middleware';

type Locale = 'ru' | 'en';

interface LocaleState {
  locale: Locale;
  setLocale: (locale: Locale) => void;
}

export const useLocaleStore = create<LocaleState>()(
  persist(
    (set) => ({
      locale: (localStorage.getItem('locale') as Locale) ?? 'ru',
      setLocale: (locale) => {
        i18next.changeLanguage(locale);
        set({ locale });
      },
    }),
    {
      name: 'locale-storage',
    }
  )
);
