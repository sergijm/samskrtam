import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import LanguageDetector from 'i18next-browser-languagedetector';

export type SupportedLng = 'en' | 'ru';

const SUPPORTED_LANGUAGES: SupportedLng[] = ['en', 'ru'];

/**
 * Раньше здесь статически импортировались все JSON-словари обоих языков
 * (24 файла) — это утяжеляло главный чанк на несколько сотен КБ, даже
 * если пользователю нужен только один язык.
 *
 * Теперь словари подгружаются динамически (import()) только для нужного
 * языка, а второй язык подтягивается лениво при переключении.
 */
async function loadLocaleBundle(lng: SupportedLng) {
  const [
    common,
    auth,
    quiz,
    settings,
    groups,
    admin,
    profile,
    grammar,
    sangraha,
    curriculum,
    lexicon,
  ] = await Promise.all([
    import(`./locales/${lng}/common.json`),
    import(`./locales/${lng}/auth.json`),
    import(`./locales/${lng}/quiz.json`),
    import(`./locales/${lng}/settings.json`),
    import(`./locales/${lng}/groups.json`),
    import(`./locales/${lng}/admin.json`),
    import(`./locales/${lng}/profile.json`),
    import(`./locales/${lng}/grammar.json`),
    import(`./locales/${lng}/sangraha.json`),
    import(`./locales/${lng}/curriculum.json`),
    import(`./locales/${lng}/lexicon.json`),
  ]);

  return {
    ...common.default,
    ...auth.default,
    ...quiz.default,
    ...settings.default,
    ...groups.default,
    ...admin.default,
    ...profile.default,
    ...grammar.default,
    ...sangraha.default,
    ...curriculum.default,
    ...lexicon.default,
  };
}

const loadedLanguages = new Set<SupportedLng>();

async function ensureLanguageLoaded(lng: string) {
  const normalized = (lng?.split('-')[0] as SupportedLng) || 'en';
  const target: SupportedLng = SUPPORTED_LANGUAGES.includes(normalized) ? normalized : 'en';

  if (loadedLanguages.has(target)) return;

  const bundle = await loadLocaleBundle(target);
  i18n.addResourceBundle(target, 'translation', bundle, true, true);
  loadedLanguages.add(target);
}

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources: {},
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false,
    },
    // Пока язык не загружен, i18next не будет падать на пустых ключах
    partialBundledLanguages: true,
  });

// Подгружаем словарь для определённого LanguageDetector'ом языка сразу при старте.
void ensureLanguageLoaded(i18n.language).then(() => {
  // Если детектор определил язык уже после init (например, из localStorage),
  // форсируем ре-рендер переводов.
  if (i18n.isInitialized) {
    i18n.emit('loaded');
  }
});

// При ручном переключении языка (i18n.changeLanguage('ru')) подгружаем
// словарь на лету перед фактическим переключением.
i18n.on('languageChanged', (lng) => {
  void ensureLanguageLoaded(lng);
});

export default i18n;
