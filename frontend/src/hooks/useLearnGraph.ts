import { useQuery } from '@tanstack/react-query';
import { learnGraphApi } from '../api/learnGraphApi';
import type { LearnGraphResponse } from '../types/learnGraph';

export const useLearnGraph = () =>
  useQuery<LearnGraphResponse, Error>({
    queryKey: ['learnGraph'],
    queryFn: () => learnGraphApi.getLearnGraph().then((r) => r.data),
    staleTime: 60_000,
  });
