import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressBar } from 'primereact/progressbar';
import { Button } from 'primereact/button';
import { Skeleton } from 'primereact/skeleton';
import type { DashboardReadingPath } from '../../types/dashboard';

interface ReadingPathProps {
  data: DashboardReadingPath | null | undefined;
  isLoading: boolean;
  isError: boolean;
}

/**
 * Блок «Путь к чтению» (§5.4 IA).
 *
 * Метрика готовности к конкретному тексту: «72% лексики гл. 2 Гиты уже пройдено».
 *
 * До готовности vidyut-cheda (блок V) — временная заглушка «скоро».
 * Если available=false — то же поведение: показываем «скоро».
 */
const ReadingPath: React.FC<ReadingPathProps> = ({ data, isLoading, isError }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <Card title={t('dashboard.readingPathTitle')} className="dashboard-reading-path mb-4">
        <div className="flex flex-column gap-2">
          <Skeleton width="80%" height="1rem" />
          <Skeleton width="100%" height="0.75rem" />
        </div>
      </Card>
    );
  }

  // Пока backend не готов или нет данных — «скоро»
  if (isError || !data || !data.available) {
    return (
      <Card title={t('dashboard.readingPathTitle')} className="dashboard-reading-path mb-4 fade-in">
        <div className="flex flex-column align-items-center gap-2 p-2">
          <i className="pi pi-book text-3xl text-400" />
          <span className="text-500">{t('dashboard.readingPathComingSoon')}</span>
          <span className="text-sm text-400">{t('dashboard.readingPathComingSoonDesc')}</span>
        </div>
      </Card>
    );
  }

  return (
    <Card title={t('dashboard.readingPathTitle')} className="dashboard-reading-path mb-4 fade-in">
      <div className="flex flex-column gap-3">
        <div className="flex flex-column gap-1">
          <span className="font-medium">{data.textTitleKey}</span>
          <span className="text-sm text-500">{data.chapterRef}</span>
        </div>
        <div className="flex flex-column gap-2">
          <div className="flex justify-content-between align-items-center">
            <span className="text-sm text-500">{t('dashboard.readingPathCoverage')}</span>
            <span className="font-bold">{Math.round(data.vocabularyCoveragePercent)}%</span>
          </div>
          <ProgressBar
            value={Math.round(data.vocabularyCoveragePercent)}
            color="#3b82f6"
            style={{ height: '0.75rem' }}
          />
        </div>
        {data.route && (
          <Button
            link
            size="small"
            icon="pi pi-arrow-right"
            iconPos="right"
            label={t('dashboard.readingPathOpenText')}
            className="align-self-start p-0 text-sm"
            onClick={() => navigate(data.route!)}
          />
        )}
      </div>
    </Card>
  );
};

export default React.memo(ReadingPath);
