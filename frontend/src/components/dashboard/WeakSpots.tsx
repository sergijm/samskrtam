import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressBar } from 'primereact/progressbar';
import { Skeleton } from 'primereact/skeleton';
import type { DashboardWeakSpot } from '../../types/dashboard';
import { PageButton } from '../common/buttons';

interface WeakSpotsProps {
  data: DashboardWeakSpot[] | undefined;
  isLoading: boolean;
  isError: boolean;
}

/**
 * Блок «Слабые места» (§5.3 IA).
 *
 * 2–3 пары падежей/форм с низким successRate.
 *
 * TODO: заменить сырой successRate на формулу §6 IA
 * (Wilson lower bound + decay, три уровня свёртки)
 * после реализации в statistics-service.
 */
const WeakSpots: React.FC<WeakSpotsProps> = ({ data, isLoading, isError }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <Card title={t('dashboard.weakSpotsTitle')} className="dashboard-weak-spots mb-4">
        <div className="flex flex-column gap-3">
          {[1, 2].map((i) => (
            <div key={i} className="flex flex-column gap-2">
              <Skeleton width="70%" height="1rem" />
              <Skeleton width="100%" height="0.75rem" />
            </div>
          ))}
        </div>
      </Card>
    );
  }

  if (isError || !data) {
    return (
      <Card title={t('dashboard.weakSpotsTitle')} className="dashboard-weak-spots mb-4 fade-in">
        <div className="flex flex-column align-items-center gap-2 p-2">
          <i className="pi pi-chart-scatter text-3xl text-400" />
          <span className="text-500">{t('dashboard.weakSpotsComingSoon')}</span>
        </div>
      </Card>
    );
  }

  if (data.length === 0) {
    return (
      <Card title={t('dashboard.weakSpotsTitle')} className="dashboard-weak-spots mb-4 fade-in">
        <div className="flex flex-column align-items-center gap-2 p-2">
          <i className="pi pi-check-circle text-3xl text-green-400" />
          <span className="text-500">{t('dashboard.weakSpotsNone')}</span>
        </div>
      </Card>
    );
  }

  const successRateColor = (rate: number): string => {
    if (rate < 40) return '#ef4444';
    if (rate < 70) return '#f59e0b';
    return '#22c55e';
  };

  return (
    <Card title={t('dashboard.weakSpotsTitle')} className="dashboard-weak-spots mb-4 fade-in">
      <div className="flex flex-column gap-3">
        {data.slice(0, 3).map((spot, idx) => (
          <div key={idx} className="flex flex-column gap-2">
            <div className="flex justify-content-between align-items-center">
              <span className="font-medium">{spot.labelKey}</span>
              <span className="text-sm text-500">
                {spot.attempts} {t('dashboard.weakSpotsAttempts')}
              </span>
            </div>
            <ProgressBar
              value={Math.round(spot.successRate)}
              color={successRateColor(spot.successRate)}
              showValue
              unit="%"
              style={{ height: '0.75rem' }}
            />
            {spot.route && (
              <PageButton
                variant="navigation"
                labelKey="dashboard.weakSpotsPractice"
                className="align-self-start p-0 text-sm"
                onClick={() => navigate(spot.route!)}
              />
            )}
          </div>
        ))}
      </div>
    </Card>
  );
};

export default React.memo(WeakSpots);
