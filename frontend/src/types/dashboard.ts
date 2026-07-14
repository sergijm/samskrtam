/**
 * DashboardSummaryDto — агрегированные данные для новой структуры Dashboard (§5 IA).
 *
 * Источник: docs/frontend/information-architecture/03-onboarding-dashboard.md §5
 *
 * Контракт требует согласования с Агентом 6 (API Contract) и Агентом 2 (statistics-service).
 * Пока backend-эндпоинт не реализован — типы используются как заглушки для фронта.
 */

/** Следующий шаг обучения: урок курикулума или SRS-очередь */
export interface DashboardContinueCta {
  /** Тип следующего шага */
  source: 'CURRICULUM_NEXT' | 'SRS_QUEUE' | 'NONE';
  /** ID урока/квиза для запуска */
  lessonId?: string;
  /** Тип урока */
  lessonType?: string;
  /** Человекочитаемый заголовок (i18n-ключ или текст) */
  titleKey?: string;
  /** Маршрут для перехода */
  route?: string;
  /** Количество SRS-карточек на сегодня (только для source=SRS_QUEUE) */
  srsDueCount?: number;
}

/** Streak и общий прогресс */
export interface DashboardStreakProgress {
  /** Текущая серия дней подряд */
  currentStreak: number;
  /** Максимальная серия */
  longestStreak: number;
  /** Всего освоено форм (словоформ/падежных форм/глагольных форм) */
  totalFormsMastered: number;
  /** Активен ли streak сегодня */
  streakActiveToday: boolean;
}

/** Одна пара «слабое место» */
export interface DashboardWeakSpot {
  /** Человекочитаемое описание пары форм, e.g. "Loc.Pl. m. — Nom.Pl. m." */
  labelKey: string;
  /** Тип урока */
  lessonType: string;
  /** ID урока */
  lessonId: string;
  /**
   * successRate по формуле §6 IA (Wilson lower bound + decay).
   * TODO: заменить на формулу §6 после реализации в statistics-service.
   * Пока — сырой correct/attempts по последним N ответам.
   */
  successRate: number;
  /** Количество попыток */
  attempts: number;
  /** Маршрут для перехода к тренировке */
  route?: string;
}

/** Прогресс пути к чтению конкретного текста */
export interface DashboardReadingPath {
  /** Название текста */
  textTitleKey: string;
  /** Глава */
  chapterRef: string;
  /** Процент пройденной лексики главы (0–100) */
  vocabularyCoveragePercent: number;
  /** Маршрут к тексту */
  route?: string;
  /** Доступна ли метрика (false пока не готов vidyut-cheda) */
  available: boolean;
}

/** Полный агрегированный DTO для дашборда */
export interface DashboardSummaryDto {
  continueCta: DashboardContinueCta;
  streakProgress: DashboardStreakProgress;
  weakSpots: DashboardWeakSpot[];
  readingPath: DashboardReadingPath | null;
}
