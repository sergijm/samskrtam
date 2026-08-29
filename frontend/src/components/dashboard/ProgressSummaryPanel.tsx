import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressBar } from 'primereact/progressbar';
import { useProgressSummary } from '../../hooks/useQuiz';

export type ProgressScope = 'learn-graph' | 'grammar' | 'lexicon';

interface ProgressSummaryPanelProps {
  scope: ProgressScope;
  /** Показывать ли заголовок области (для автономного использования). */
  showScopeTitle?: boolean;
}

/**
 * «Панелька» реального прогресса по области. Все вычисления идут через
 * quiz-service (GET /api/v2/quiz/progress/summary?scope=...), который
 * считает прогресс по таблице quiz_item_score. Показывает реальное число
 * progress_tag и процент прохождения.
 */
const ProgressSummaryPanel: React.FC<ProgressSummaryPanelProps> = ({ scope, showScopeTitle = true }) => {
  const { t } = useTranslation();
  const { data, isLoading, isError } = useProgressSummary(scope);

  const scopeLabel = t(`progressPanel.scope.${scope}`);
  const total = data?.totalProgressTags ?? 0;
  const mastered = data?.masteredProgressTags ?? 0;
  const percent = data?.percent ?? 0;

  return (
    <Card className="progress-summary-panel border-round-xl" pt={{ body: { className: 'p-3' }, content: { className: 'p-0' } }}>
      <div className="flex flex-column gap-2">
        {showScopeTitle && (
          <div className="flex align-items-center gap-2">
            <i className="pi pi-chart-bar text-primary" />
            <span className="font-medium">{scopeLabel}</span>
          </div>
        )}
        <div className="flex justify-content-between align-items-center">
          <span className="text-sm text-500">{t('progressPanel.realTags')}</span>
          <span className="font-bold">{total}</span>
        </div>
        <div className="flex justify-content-between align-items-center">
          <span className="text-sm text-500">{t('progressPanel.mastered')}</span>
          <span className="font-bold text-green-500">{mastered}</span>
        </div>
        <ProgressBar value={percent} style={{ height: '0.75rem' }} />
        <div className="flex justify-content-between align-items-center">
          <span className="text-sm text-500">{t('progressPanel.percent')}</span>
          <span className="font-bold">{percent}%</span>
        </div>
        {isLoading && <span className="text-xs text-500">{t('common.loading')}</span>}
        {isError && <span className="text-xs text-500">{t('progressPanel.unavailable')}</span>}
      </div>
    </Card>
  );
};

export default React.memo(ProgressSummaryPanel);
