import api from './axios';
import { QuizListItem, QuizSummaryDto, StartSessionResponse, AnswerRequest, AnswerResponse, QuizType } from '../types/quiz';

export const quizApi = {
  getQuizList: (category?: string) => {
    let url = '/api/v1/content/quizzes';
    if (category) {
      url += `?category=${category}`;
    }
    return api.get<QuizListItem[]>(url);
  },

  getQuizBySlug: (slug: string) => api.get<QuizSummaryDto>(`/api/v1/content/quizzes/by-slug/${slug}`),

  startSession: (quizIdentifier: string, quizType: QuizType, userLocale: string) => {
    let url = '';
    let params: { quizId?: string } = {};

    if (quizType === 'VOCABULARY') {
      url = `/api/v1/quiz/vocabulary/${quizIdentifier}/sessions/start`;
    } else { // DECLENSIONS or CONJUGATIONS
      url = `/api/v1/quiz/${quizType.toLowerCase()}/sessions/start`;
      params = { quizId: quizIdentifier }; // quizIdentifier is the actual UUID for grammar quizzes
    }
    return api.post<StartSessionResponse>(url, null, {
      params: params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  submitAnswer: (sessionId: string, quizIdentifier: string, quizType: QuizType, answer: AnswerRequest, userLocale: string) => {
    let url = '';
    if (quizType === 'VOCABULARY') {
      url = `/api/v1/quiz/vocabulary/${quizIdentifier}/sessions/${sessionId}/answer`;
    } else { // DECLENSIONS or CONJUGATIONS
      url = `/api/v1/quiz/${quizType.toLowerCase()}/sessions/${sessionId}/answer`;
    }
    return api.post<AnswerResponse>(url, answer, {
      headers: { 'X-User-Locale': userLocale },
    });
  },
};
