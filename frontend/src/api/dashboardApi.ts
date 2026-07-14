import api from './axios';
import { DashboardSummaryDto } from '../types/dashboard';

/**
 * API-клиент для Dashboard V2.
 *
 * Endpoint пока не реализован в backend (ждёт Агента 2 + Агента 6).
 * Используется как заглушка — всегда возвращает ошибку/пустой ответ,
 * пока не будет готов statistics-service контракт.
 */
export const dashboardApi = {
  getDashboardSummary: () =>
    api.get<DashboardSummaryDto>('/api/v1/statistics/dashboard/summary'),
};
