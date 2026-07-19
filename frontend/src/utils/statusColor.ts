import type { WordStatus } from '../types/lesson';

/**
 * Маппинг статус → цвет ProgressBar.
 * Палитра согласована с WordStatusIcon.tsx:
 *   NEW      → серый (text-color-secondary)
 *   LEARNING → синий (text-primary)
 *   REVIEW   → жёлтый (text-yellow-500)
 *   MASTERED → зелёный (text-green-500)
 */
const STATUS_COLOR_MAP: Record<WordStatus, string> = {
  NEW: '#9ca3af',                        // grey-400, аналог text-color-secondary
  LEARNING: 'var(--primary-color, #3b82f6)', // text-primary → CSS-переменная темы
  REVIEW: '#eab308',                     // yellow-500
  MASTERED: '#22c55e',                   // green-500
};

/**
 * Возвращает CSS-цвет для ProgressBar по статусу.
 * Используется через проп color у PrimeReact ProgressBar.
 */
export const statusToProgressColor = (status: WordStatus): string =>
  STATUS_COLOR_MAP[status];
