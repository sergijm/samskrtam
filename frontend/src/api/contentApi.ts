import api from './axios';
import { SandhiRuleDto } from '../types/content'; // Assuming types/content.ts exists

export const contentApi = {
  getAllSandhiRules: () => {
    return api.get<SandhiRuleDto[]>('/api/v1/eamenau/sandhi-rules');
  },
};
