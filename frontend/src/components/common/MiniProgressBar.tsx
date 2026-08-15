import React from 'react';
import { ProgressBar } from 'primereact/progressbar';
import { statusToProgressColor } from '../../utils/statusColor';
import type { WordStatus } from '../../types/lesson';

export interface MiniProgressBarProps {
  /** Значение прогресса 0–100 */
  value: number;
  /** Статус для цвета (если не передан — цвет по умолчанию от PrimeReact) */
  status?: WordStatus;
  /** Клик по тексту процента (только когда showValue=false) */
  onClick?: () => void;
  /** Показывать значение внутри ProgressBar или рядом текстом */
  showValue?: boolean;
  /** Высота ProgressBar, по умолчанию '5px' */
  height?: string;
  /** Ширина ProgressBar, по умолчанию '80px' */
  width?: string;
  className?: string;
}

/**
 * Миниатюрный прогресс-бар для колонки «Изучено» в таблицах уроков.
 *
 * Используется в:
 * - GrammarProgressGrid (вкладка «Прогресс» грамматического урока)
 * - VocabularyLessonPage (лексические уроки)
 * - VerseWordsList (таблица слов на VersePage)
 */
export const MiniProgressBar: React.FC<MiniProgressBarProps> = ({
  value,
  status,
  onClick,
  showValue = false,
  height = '8px',
  width = '80px',
  className,
}) => {
  const color = status ? statusToProgressColor(status) : undefined;

  return (
    <div className={`flex align-items-center gap-2 ${className ?? ''}`}>
      <ProgressBar
        value={value}
        color={color}
        style={{ height, width }}
        showValue={showValue}
      />

    </div>
  );
};
