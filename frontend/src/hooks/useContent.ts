import { useQuery } from '@tanstack/react-query';
import { contentApi } from '../api/contentApi'; // Assuming contentApi exists
import { SandhiRuleDto } from '../types/content'; // Assuming types/content.ts exists

export const useSandhiRules = () => {
  return useQuery<SandhiRuleDto[], Error>({
    queryKey: ['eamenau', 'sandhi-rules'],
    queryFn: async () => {
      const response = await contentApi.getAllSandhiRules();
      return response.data;
    },
  });
};
