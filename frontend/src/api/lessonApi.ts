import api from './axios';
import {
  DeclensionParadigmPageDto,
  ConjugationParadigmPageDto,
  CaseEndingDto,
  VerbalEndingDto,
} from '../types/content-dtos';

export const lessonApi = {

// Получить словарный урок с прогрессом пользователя (v2)
  getVocabularyLesson: (slug: string) =>
    api.get(`/api/v2/lessons/vocabulary/${slug}`),

  // Получить грамматический урок с прогрессом пользователя (v2 — данные из curriculum-service)
  getGrammarLesson: (slug: string) =>
    api.get(`/api/v2/lessons/grammar/${slug}`),

  // Получить историю ответов на конкретное слово в уроке (v2)
  getWordHistory: (slug: string, wordId: string) =>
    api.get(`/api/v2/lessons/vocabulary/${slug}/words/${wordId}/history`),

  // Получить историю ответов на конкретный грамматический вопрос по caseType + numberType (v2)
  getQuestionHistory: (slug: string, caseType: string, numberType: string, gender: string) =>
    api.get(`/api/v2/lessons/grammar/${slug}/questions/history`, {
      params: { caseType, numberType, gender },
    }),

    // Получить ОДНУ парадигму склонений по индексу (карусель grammar-lesson-page §2.2) — v2
  getDeclensionParadigm: (slug: string, index: number) =>
    api.get<DeclensionParadigmPageDto>(`/api/v2/lessons/${slug}/declension-paradigms`, {
      params: { index },
    }),

  // Получить ОДНУ парадигму спряжений по индексу (карусель verb lesson) — v2
  getConjugationParadigm: (slug: string, index: number, voice?: string | null) => {
    const params: Record<string, string | number> = { index };
    if (voice) params.voice = voice;
    return api.get<ConjugationParadigmPageDto>(`/api/v2/lessons/${slug}/conjugation-paradigms`, { params });
  },

  // Получить справочную таблицу падежных окончаний (lingua.case_endings)
  getCaseEndings: () =>
    api.get<CaseEndingDto[]>('/api/v2/curriculum/lingua/case-endings'),

  // Получить справочную таблицу глагольных окончаний (lingua.verbal_endings)
  getVerbalEndings: () =>
    api.get<VerbalEndingDto[]>('/api/v2/curriculum/lingua/verbal-endings'),
};