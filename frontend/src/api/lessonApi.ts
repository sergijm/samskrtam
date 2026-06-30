import api from './axios';

export const lessonApi = {

  // Получить словарный урок с прогрессом пользователя
  getVocabularyLesson: (slug: string) =>
    api.get(`/api/v1/lessons/vocabulary/${slug}`),

  // Получить грамматический урок с прогрессом пользователя
  getGrammarLesson: (slug: string) =>
    api.get(`/api/v1/lessons/grammar/${slug}`),

  // Получить историю ответов на конкретное слово в уроке
  getWordHistory: (slug: string, wordId: string) =>
    api.get(`/api/v1/lessons/vocabulary/${slug}/words/${wordId}/history`),

  // Получить историю ответов на конкретный грамматический вопрос по caseType + numberType
  getQuestionHistory: (slug: string, caseType: string, numberType: string) =>
    api.get(`/api/v1/lessons/grammar/${slug}/questions/history`, {
      params: { caseType, numberType },
    }),
};