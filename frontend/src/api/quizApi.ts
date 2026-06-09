import api from './axios';
import { QuizListItem, QuizSummaryDto, StartSessionResponse, AnswerRequest, AnswerResponse, QuizType, QuizSessionSummary, AnswerHistory, QuizProgress, ResumeSessionResponse, StartOrResumeResponse } from '../types/quiz';
import { PaginatedResponse } from '../types/common';

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
    const slug = quizType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start`;
    const params = { quizId: quizId };

    return api.post<StartSessionResponse>(url, null, {
      params: params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  startOrResumeSession: (quizId: string, quizType: QuizType, userLocale: string) => {
    const slug = quizType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start-or-resume`;
    const params = { quizId: quizId };

    return api.post<StartOrResumeResponse>(url, null, {
        params: params,
        headers: { 'X-User-Locale': userLocale },
    });
  },

  resumeSession: (sessionId: string, quizType: QuizType, userLocale: string) => {
    const slug = quizType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/resume`;
    return api.get<ResumeSessionResponse>(url, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  submitAnswer: (sessionId: string, quizId: string, quizType: QuizType, answer: AnswerRequest, userLocale: string) => {
    const slug = quizType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/answer`;

    return api.post<AnswerResponse>(url, answer, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  completeSession: (sessionId: string, quizType: QuizType, userLocale: string) => {
    const slug = quizType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/complete`;
    return api.post(url, null, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  getUserQuizSessions: (userId: string, page: number, size: number, sortBy: string, sortDirection: string, quizType?: QuizType, status?: string) => {
    return api.get<PaginatedResponse<QuizSessionSummary>>(`/api/v1/quiz-sessions`, {
      params: {
        userId,
        page,
        size,
        sortBy,
        sortDirection,
        quizType,
        status,
      },
    });
  },

  getQuizSessionSummary: (sessionId: string) => {
    return api.get<QuizSessionSummary>(`/api/v1/quiz-sessions/${sessionId}/summary`);
  },

  getSessionAnswerHistory: (sessionId: string, userId: string) => {
    return api.get<AnswerHistory[]>(`/api/v1/quiz-sessions/${sessionId}/answers`, {
      params: {
        userId,
      },
    });
  },

  getLatestUnfinishedQuizProgress: (userId: string, quizId: string) => {
    return api.get<QuizProgress>(`/api/v1/quiz-sessions/progress`, {
      params: { userId, quizId },
    });
  },
};
