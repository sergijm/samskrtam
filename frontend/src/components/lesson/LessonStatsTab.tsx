import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import type { LessonStatusSummary as LessonStatusSummaryType } from '../../types/lesson';

interface LessonStatsTabProps {
  statusSummary: LessonStatusSummaryType | null | undefined;
  /** Колбэк для старта квиза с фильтром статуса (NEW / LEARNING / REVIEW) */
  onStartQuiz: (statusFilter: string) => void;
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

export const LessonStatsTab = ({ statusSummary, onStartQuiz }: LessonStatsTabProps) => {
  const { i18n } = useTranslation();
  const isRu = i18n.language === 'ru';

  if (!statusSummary) {
    return null;
  }

  const { newCount, learning, mastered, reviewDue } = statusSummary;

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
        onClick={() => onStartQuiz('NEW')}
      />

      <StatRow
        className="stats-tab-learning"
        label={isRu ? 'В процессе' : 'Learning'}
        value={learning}
        severity="info"
        buttonLabel={isRu ? 'Продолжить' : 'Continue'}
        buttonIcon="pi pi-forward"
        disabled={learning <= 0}
        onClick={() => onStartQuiz('LEARNING')}
      />

      <StatRow
        className="stats-tab-mastered"
        label={isRu ? 'Изучено' : 'Mastered'}
        value={mastered}
        severity="success"
        buttonLabel={isRu ? 'Повторить' : 'Review'}
        buttonIcon="pi pi-history"
        disabled={reviewDue <= 0}
        onClick={() => onStartQuiz('REVIEW')}
      />
    </div>
  );
};

