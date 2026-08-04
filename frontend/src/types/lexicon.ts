/**
 * Lexicon domain types — стартовая страница «Лексика».
 *
 * UI работает с этими DTO через `LexiconService` (см. src/services/lexiconService.ts).
 * Сейчас данные приходят из mock-репозитория, позже — из API, без изменения UI.
 * `nameKey` — i18n-ключ для локализации; API будет возвращать id, UI переводит.
 */

/** Общий прогресс по всему словарю (2000 слов) */
export interface LexiconProgressSummary {
  totalWords: number;
  masteredCount: number;
}

/** Блок «Сегодня» — что делать сейчас */
export interface LexiconToday {
  reviewDue: number;
  newWords: number;
  weakWords: number;
}

/** Диапазон частотности */
export interface FrequencyBand {
  id: string;
  from: number;
  to: number;
  wordCount: number;
  masteredCount: number;
}

/** Семантическая тема */
export interface LexicalTopic {
  id: string;
  nameKey: string;
  wordCount: number;
  masteredCount: number;
}

/** Часть речи */
export interface PartOfSpeech {
  id: string;
  nameKey: string;
  wordCount: number;
}

/** Произведение / источник лексики */
export interface LexicalSource {
  id: string;
  title: string;
  devanagari?: string;
  wordCount: number;
  masteredCount: number;
}

/** Пользовательский список */
export interface UserCollection {
  id: string;
  nameKey: string;
  wordCount: number;
}

/** Готовый пресет «Быстрый старт» */
export interface QuickStartPreset {
  id: string;
  titleKey: string;
  metaKey: string;
}

/** Агрегированные данные стартовой страницы лексики */
export interface LexiconDashboardData {
  summary: LexiconProgressSummary;
  today: LexiconToday;
  frequencyBands: FrequencyBand[];
  topics: LexicalTopic[];
  pos: PartOfSpeech[];
  sources: LexicalSource[];
  collections: UserCollection[];
  quickStart: QuickStartPreset[];
}
