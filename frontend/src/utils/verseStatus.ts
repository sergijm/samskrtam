import type { VerseStatus } from '../types/sangraha';

/**
 * Единый маппинг статуса стиха → цвет/иконка.
 * Используется ChapterPage и VersesBatchPage (sangraha-service/batch-verse-review.md) —
 * не заводить второй источник статус-кодировки.
 */
export const verseStatusSeverity: Record<VerseStatus, 'success' | 'info' | 'warn' | 'danger'> = {
  ANALYZED: 'success',
  ANALYZING: 'info',
  DRAFT: 'warn',
  FAILED: 'danger',
};

export const verseStatusIcon: Record<VerseStatus, { icon: string; color: string }> = {
  ANALYZED: { icon: 'pi pi-check-circle', color: 'var(--green-500)' },
  ANALYZING: { icon: 'pi pi-spin pi-spinner', color: 'var(--blue-500)' },
  DRAFT: { icon: 'pi pi-pencil', color: 'var(--yellow-500)' },
  FAILED: { icon: 'pi pi-exclamation-circle', color: 'var(--red-500)' },
};
