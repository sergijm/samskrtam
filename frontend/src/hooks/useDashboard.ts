import { useQuery } from '@tanstack/react-query';
import { dashboardApi } from '../api/dashboardApi';
import { DashboardSummaryDto } from '../types/dashboard';
import { useMe } from './useUser';

/**
 * Хук для получения агрегированных данных Dashboard V2.
 *
 * Пока backend не готов — query будет фейлиться.
 * Компоненты должны обрабатывать isError и показывать skeleton/«скоро».
 */
export const useDashboardSummary = () => {
  const { data: user } = useMe();

  return useQuery<DashboardSummaryDto, Error>({
    queryKey: ['dashboardSummary', user?.id],
    queryFn: () => dashboardApi.getDashboardSummary().then((r) => r.data),
    enabled: !!user?.id,
    staleTime: 60_000, // 1 минута
    retry: 1,
  });
};
