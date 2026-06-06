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

  startSession: (quizId: string, quizType: QuizType, userLocale: string) => {
    const slug = quizType.toLowerCase(); // Use quizType as the slug in the path
    const url = `/api/v1/quiz/${slug}/sessions/start`;
    const params = { quizId: quizId }; // Always pass the actual quizId as a query parameter

    return api.post<StartSessionResponse>(url, null, {
      params: params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  submitAnswer: (sessionId: string, quizId: string, quizType: QuizType, answer: AnswerRequest, userLocale: string) => {
    const slug = quizType.toLowerCase(); // Use quizType as the slug in the path
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/answer`;

    return api.post<AnswerResponse>(url, answer, {
      headers: { 'X-User-Locale': userLocale },
    });
  },
};
