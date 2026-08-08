/**
 * LexiconService — единая точка доступа к данным «Лексики».
 *
 * UI работает только с этим интерфейсом и не знает источник данных.
 * Реализация — ApiLexiconService (curriculum-service GET /api/v2/curriculum/lexicon).
 */

import { LexiconDashboardData } from '../types/lexicon';
import { lexiconApi } from '../api/lexiconApi';

export interface LexiconService {
  getDashboard(): Promise<LexiconDashboardData>;
}

class ApiLexiconService implements LexiconService {
  async getDashboard(): Promise<LexiconDashboardData> {
    const response = await lexiconApi.getDashboard();
    return response.data;
  }
}

let lexiconService: LexiconService | undefined;

export function getLexiconService(): LexiconService {
  if (!lexiconService) {
    lexiconService = new ApiLexiconService();
  }
  return lexiconService;
}
