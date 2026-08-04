import { useQuery } from '@tanstack/react-query';
import { getLexiconService } from '../services/lexiconService';
import { LexiconDashboardData } from '../types/lexicon';

/**
 * Данные стартовой страницы «Лексика» через LexiconService.
 * Сейчас отдаёт mock, позже — API (без изменения компонентов).
 */
export const useLexiconDashboard = () =>
  useQuery<LexiconDashboardData, Error>({
    queryKey: ['lexiconDashboard'],
    queryFn: () => getLexiconService().getDashboard(),
    staleTime: 60_000,
  });
