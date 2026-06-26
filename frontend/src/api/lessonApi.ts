import api from './axios';

export const lessonApi = {

  // Получить словарный урок с прогрессом пользователя
  getVocabularyLesson: (slug: string) =>
    api.get(`/api/v1/lessons/vocabulary/${slug}`),

  // Получить грамматический урок с прогрессом пользователя
  getGrammarLesson: (type: string) =>
    api.get(`/api/v1/lessons/grammar/${type}`),

  // Получить историю ответов на конкретное слово в уроке
  getWordHistory: (slug: string, wordId: string) =>
    api.get(`/api/v1/lessons/vocabulary/${slug}/words/${wordId}/history`),

  // Получить историю ответов на конкретный вопрос в уроке
  getQuestionHistory: (quizId: string, questionId: string) =>
    api.get(`/api/v1/lessons/grammar/${quizId}/questions/${questionId}/history`),
};