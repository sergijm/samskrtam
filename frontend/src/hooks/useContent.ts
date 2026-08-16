import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/contentApi';
import type { SandhiRulesResponse } from '../types/content';

export const useSandhiRules = () => {
  return useQuery<SandhiRulesResponse, Error>({
    queryKey: ['eamenau', 'sandhi-rules'],
    queryFn: async () => {
      const response = await contentApi.getAllSandhiRules();
      return response.data;
    },
  });
};
