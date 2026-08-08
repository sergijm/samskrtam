import { useLocaleStore } from '../store/localeStore';

type Localized = { ru: string; en: string };

/**
 * Возвращает функцию выбора локализованной подписи из пары ru/en,
 * пришедшей с бэкенда. Используется вместо i18n-ключей там, где подпись
 * приходит из таксономии curriculum-service.
 */
export const useLexiconLocale = () => {
  const { locale } = useLocaleStore();
  return (pair: Localized) => (locale === 'en' ? pair.en : pair.ru);
};
