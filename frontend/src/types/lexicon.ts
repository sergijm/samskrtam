/**
 * Lexicon domain types — стартовая страница «Лексика».
 *
 * Данные приходят из curriculum-service GET /api/v2/curriculum/lexicon через
 * `LexiconService` (см. src/services/lexiconService.ts).
 * Локализуемые подписи приходят с бэкенда как пары ru/en и выбираются по
 * текущей локали (см. src/store/localeStore.ts) — таксономия слишком велика,
 * чтобы перечислять её ключами в lexicon.json.
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
  nameRu: string;
  nameEn: string;
  wordCount: number;
  masteredCount: number;
}

/** Семантическая тема (привязка леммы к топику через семантические классы) */
export interface SemanticTopic {
  id: string;
  nameRu: string;
  nameEn: string;
  wordCount: number;
  masteredCount: number;
}

/** Часть речи */
export interface LexiconPos {
  id: string;
  nameRu: string;
  nameEn: string;
  wordCount: number;
}

/** Пользовательский список */
export interface UserCollection {
  id: string;
  name: string;
  wordCount: number;
}

/** Готовый пресет «Быстрый старт» */
export interface QuickStartPreset {
  id: string;
  titleRu: string;
  titleEn: string;
  metaRu: string;
  metaEn: string;
}

/** Агрегированные данные стартовой страницы лексики */
export interface LexiconDashboardData {
  summary: LexiconProgressSummary;
  today: LexiconToday;
  frequencyBands: FrequencyBand[];
  topics: LexicalTopic[];
  semanticTopics: SemanticTopic[];
  pos: LexiconPos[];
  collections: UserCollection[];
  quickStart: QuickStartPreset[];
}