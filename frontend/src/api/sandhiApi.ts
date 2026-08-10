import api from './axios';
import type { SandhiRulesResponse } from '../types/content-dtos';

export const sandhiApi = {
  getRules: (topicCode: string) =>
    api.get<SandhiRulesResponse>(`/api/v2/curriculum/sandhi-rules/${topicCode}`),

  getRulesByNumbers: (numbers: number[]) =>
    api.get<SandhiRulesResponse>('/api/v2/curriculum/sandhi-rules', {
      params: { rule: numbers },
      paramsSerializer: { indexes: null },
    }),
};