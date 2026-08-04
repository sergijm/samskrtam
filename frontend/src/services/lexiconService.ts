/**
 * LexiconService — единая точка доступа к данным «Лексики».
 *
 * UI работает только с этим интерфейсом и не знает источник данных.
 *
 * Сейчас: MockLexiconService (локальные данные, см. src/data/mockLexicon.ts).
 * Позже: ApiLexiconService — заменить `new MockLexiconService()` на API-реализацию
 * в `getLexiconService()` (единственное место переключения).
 */

import { LexiconDashboardData } from '../types/lexicon';
import { mockLexiconDashboard } from '../data/mockLexicon';

export interface LexiconService {
  getDashboard(): Promise<LexiconDashboardData>;
}

class MockLexiconService implements LexiconService {
  async getDashboard(): Promise<LexiconDashboardData> {
    return mockLexiconDashboard;
  }
}

class ApiLexiconService implements LexiconService {
  /** TODO: заменить на axios-клиент (src/api/lexiconApi.ts) после готовности бэкенда. */
  async getDashboard(): Promise<LexiconDashboardData> {
    throw new Error('Lexicon API is not implemented yet');
  }
}

let lexiconService: LexiconService | undefined;

export function getLexiconService(): LexiconService {
  if (!lexiconService) {
    // Единственное место переключения mock → API:
    lexiconService = new MockLexiconService();
  }
  return lexiconService;
}
