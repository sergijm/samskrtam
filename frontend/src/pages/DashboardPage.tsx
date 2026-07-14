import React from 'react';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useMe } from '../hooks/useUser';
import { useDashboardSummary } from '../hooks/useDashboard';
import ContinueCta from '../components/dashboard/ContinueCta';
import StreakProgress from '../components/dashboard/StreakProgress';
import WeakSpots from '../components/dashboard/WeakSpots';
import ReadingPath from '../components/dashboard/ReadingPath';
import ProgressMapLink from '../components/dashboard/ProgressMapLink';
import CategoryTiles from '../components/dashboard/CategoryTiles';

/**
 * DashboardPage — «командный центр» (§5 IA).
 *
 * Структура:
 *   1. Главный CTA «Продолжить» (ContinueCta)
 *   2. Streak / общий прогресс (StreakProgress)
 *   3. Слабые места (WeakSpots)
 *   4. Путь к чтению (ReadingPath)
 *   5. Ссылка на карту прогресса (ProgressMapLink)
 *   6. Плитки категорий внизу (CategoryTiles)
 *
 * Пока backend-эндпоинты не готовы — каждый блок показывает «скоро»/skeleton
 * вместо фиктивных данных.
 */
export default function DashboardPage() {
  const { t } = useTranslation();
  const { data: user, isLoading: userLoading, isError: userError, error: userErr } = useMe();
  const {
    data: dashboardSummary,
    isLoading: dashboardLoading,
    isError: dashboardError,
  } = useDashboardSummary();

  if (userLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (userError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('userProfile.errorLoadingUser', { message: (userErr as Error)?.message })} />
      </div>
    );
  }

  return (
    <div className="dashboard-page p-3 md:p-4">
      {/* Главный CTA «Продолжить» — §5.1 */}
      <ContinueCta
        data={dashboardSummary?.continueCta}
        isLoading={dashboardLoading}
        isError={dashboardError}
      />

      {/* Streak / общий прогресс — §5.2 */}
      <StreakProgress
        data={dashboardSummary?.streakProgress}
        isLoading={dashboardLoading}
        isError={dashboardError}
      />

      {/* Слабые места — §5.3 */}
      <WeakSpots
        data={dashboardSummary?.weakSpots}
        isLoading={dashboardLoading}
        isError={dashboardError}
      />

      {/* Путь к чтению — §5.4 */}
      <ReadingPath
        data={dashboardSummary?.readingPath}
        isLoading={dashboardLoading}
        isError={dashboardError}
      />

      {/* Ссылка на карту прогресса — §5.5 */}
      <ProgressMapLink />

      {/* Плитки категорий внизу — §5.6 */}
      <CategoryTiles />
    </div>
  );
}


