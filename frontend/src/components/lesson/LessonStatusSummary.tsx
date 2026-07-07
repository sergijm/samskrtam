import React from 'react';
import { useTranslation } from 'react-i18next';
import { Badge } from 'primereact/badge';
import { ProgressBar } from 'primereact/progressbar';
import type { LessonStatusSummary as LessonStatusSummaryType } from '../../types/lesson';

interface LessonStatusSummaryProps {
  statusSummary: LessonStatusSummaryType | null | undefined;
  total?: number;
  learned?: number;
}

export const LessonStatusSummary = ({ statusSummary, total, learned }: LessonStatusSummaryProps) => {
  const { t, i18n } = useTranslation();
  const isRu = i18n.language === 'ru';

  if (!statusSummary) {
    return null;
  }

  const displayTotal = total ?? statusSummary.total;
  const displayLearned = learned ?? statusSummary.mastered;
  const progressPercent = displayTotal > 0 ? Math.round((displayLearned / displayTotal) * 100) : 0;

  return (
    <div className="card mb-3">
      <div className="mb-3">
        <div className="flex justify-content-between mb-1">
          <span>{isRu ? 'Прогресс' : 'Progress'}</span>
          <span>{displayLearned} {isRu ? 'из' : 'of'} {displayTotal}</span>
        </div>
        <ProgressBar value={progressPercent} showValue={false} />
      </div>

      <div className="flex flex-wrap gap-3 align-items-center">
        <div className="flex align-items-center gap-1">
          <Badge value={statusSummary.total} severity="info" />
          <span className="text-sm text-color-secondary">
            {isRu ? 'Всего' : 'Total'}
          </span>
        </div>
        <div className="flex align-items-center gap-1">
          <Badge value={statusSummary.newCount} severity="warning" />
          <span className="text-sm text-color-secondary">
            {isRu ? 'Новых' : 'New'}
          </span>
        </div>
        <div className="flex align-items-center gap-1">
          <Badge value={statusSummary.learning} severity="info" />
          <span className="text-sm text-color-secondary">
            {isRu ? 'В процессе' : 'Learning'}
          </span>
        </div>
        <div className="flex align-items-center gap-1">
          <Badge value={statusSummary.mastered} severity="success" />
          <span className="text-sm text-color-secondary">
            {isRu ? 'Изучено' : 'Mastered'}
          </span>
        </div>
        {statusSummary.reviewDue > 0 && (
          <div className="flex align-items-center gap-1">
            <Badge value={statusSummary.reviewDue} severity="danger" />
            <span className="text-sm text-color-secondary">
              {isRu ? 'На повторение' : 'Review due'}
            </span>
          </div>
        )}
      </div>
    </div>
  );
};