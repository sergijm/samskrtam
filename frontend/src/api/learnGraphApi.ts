import api from './axios';
import type { LearnGraphResponse } from '../types/learnGraph';

export const learnGraphApi = {
  getLearnGraph: () => api.get<LearnGraphResponse>('/api/v2/curriculum/learn-graph'),
};