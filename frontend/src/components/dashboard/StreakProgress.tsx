import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { Skeleton } from 'primereact/skeleton';
import type { DashboardStreakProgress } from '../../types/dashboard';

interface StreakProgressProps {
  data: DashboardStreakProgress | undefined;
  isLoading: boolean;
  isError: boolean;
}

/**
 * Блок Streak / общий прогресс (§5.2 IA).
 *
 * Показывает: текущий streak (дни подряд), максимальный streak,
 * общее количество освоенных форм, активен ли streak сегодня.
 */
const StreakProgress: React.FC<StreakProgressProps> = ({ data, isLoading, isError }) => {
  const { t } = useTranslation();

  if (isLoading) {
    return (
      <Card className="dashboard-streak mb-4">
        <div className="flex justify-content-around gap-3">
          {[1, 2, 3].map((i) => (
            <div key={i} className="flex flex-column align-items-center gap-2">
              <Skeleton shape="circle" size="3rem" />
              <Skeleton width="4rem" height="1rem" />
            </div>
          ))}
        </div>
      </Card>
    );
  }

  if (isError || !data) {
    return (
      <Card className="dashboard-streak mb-4 fade-in">
        <div className="flex flex-column align-items-center gap-2 p-2">
          <i className="pi pi-calendar text-3xl text-400" />
          <span className="text-500">{t('dashboard.streakComingSoon')}</span>
        </div>
      </Card>
    );
  }

  const metrics = [
    {
      icon: data.streakActiveToday ? 'pi pi-fire text-orange-500' : 'pi pi-calendar text-500',
      value: data.currentStreak,
      label: t('dashboard.streakCurrent'),
    },
    {
      icon: 'pi pi-star text-yellow-500',
      value: data.longestStreak,
      label: t('dashboard.streakLongest'),
    },
    {
      icon: 'pi pi-check-circle text-green-500',
      value: data.totalFormsMastered,
      label: t('dashboard.streakFormsMastered'),
    },
  ];

  return (
    <Card className="dashboard-streak mb-4 fade-in">
      <div className="flex justify-content-around flex-wrap gap-3">
        {metrics.map((m) => (
          <div key={m.label} className="flex flex-column align-items-center gap-1">
            <div className="flex align-items-center gap-2">
              <i className={`${m.icon} text-2xl`} />
              <span className="text-3xl font-bold">{m.value}</span>
            </div>
            <span className="text-sm text-500">{m.label}</span>
          </div>
        ))}
      </div>
    </Card>
  );
};

export default React.memo(StreakProgress);
