import api from './axios';
import type { LexiconDashboardData } from '../types/lexicon';

export const lexiconApi = {
  getDashboard: () => api.get<LexiconDashboardData>('/api/v2/curriculum/lexicon'),
};
