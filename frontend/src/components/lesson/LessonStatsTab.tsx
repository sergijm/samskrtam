import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import type { LessonStatusSummary as LessonStatusSummaryType } from '../../types/lesson';

interface LessonStatsTabProps {
  statusSummary: LessonStatusSummaryType | null | undefined;
  /** Путь квиза для навигации (e.g., '/quiz/vocabulary/:slug' or '/quiz/grammar/:type') */
  quizPath: string;
}

interface StatRowProps {
  className: string;
  label: string;
  value: number;
  severity: 'success' | 'warning' | 'info' | 'secondary';
  buttonLabel: string;
  buttonIcon: string;
  disabled: boolean;
  onClick: () => void;
}

const StatRow = ({ className, label, value, severity, buttonLabel, buttonIcon, disabled, onClick }: StatRowProps) => {
  const severityClass = disabled ? 'text-color-secondary' : `stat-row-${severity}`;

  return (
    <div className={`${className} card p-2 flex-1 flex align-items-center justify-content-between gap-2 border-1 border-200 border-round`}>
      <div className="flex align-items-center gap-2">
        <span className={`text-2xl font-bold ${severityClass}`}>{value}</span>
        <span className="text-base">{label}</span>
      </div>
        <Button
        label={buttonLabel}
        icon={buttonIcon}
        size="small"
        outlined
        disabled={disabled}
        onClick={onClick}
      />
    </div>
  );
};

export const LessonStatsTab = ({ statusSummary, quizPath }: LessonStatsTabProps) => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();
  const isRu = i18n.language === 'ru';

  if (!statusSummary) {
    return null;
  }

  const { newCount, learning, mastered, reviewDue } = statusSummary;

  const handleStatusClick = (statusFilter: string) => {
    navigate(`${quizPath}?statusFilter=${statusFilter}`);
  };

  return (
    <div className="flex flex-wrap gap-3">

      <StatRow
        className="stats-tab-new"
        label={isRu ? 'Не изучено' : 'Not studied'}
        value={newCount}
        severity="warning"
        buttonLabel={isRu ? 'Изучить' : 'Study'}
        buttonIcon="pi pi-play"
        disabled={newCount <= 0}
        onClick={() => handleStatusClick('NEW')}
      />

      <StatRow
        className="stats-tab-learning"
        label={isRu ? 'В процессе' : 'Learning'}
        value={learning}
        severity="info"
        buttonLabel={isRu ? 'Продолжить' : 'Continue'}
        buttonIcon="pi pi-forward"
        disabled={learning <= 0}
        onClick={() => handleStatusClick('LEARNING')}
      />

      <StatRow
        className="stats-tab-mastered"
        label={isRu ? 'Изучено' : 'Mastered'}
        value={mastered}
        severity="success"
        buttonLabel={isRu ? 'Повторить' : 'Review'}
        buttonIcon="pi pi-history"
        disabled={reviewDue <= 0}
        onClick={() => handleStatusClick('REVIEW')}
      />
    </div>
  );
};

