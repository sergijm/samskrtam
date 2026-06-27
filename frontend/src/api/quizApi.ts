import api from './axios';
import { QuizListItem, QuizSummaryDto, StartSessionResponse, AnswerRequest, AnswerResponse, LessonType, QuizSessionSummary, AnswerHistory, QuizProgress, ResumeSessionResponse, StartOrResumeResponse, LessonListResponse } from '../types/quiz';
import { PaginatedResponse } from '../types/common';

export const quizApi = {
  getQuizList: (category?: string) => {
    const url = category
      ? `/api/v1/lessons/${category}`
      : '/api/v1/lessons';
    return api.get<LessonListResponse>(url);
  },

  getQuizBySlug: (slug: string) => api.get<QuizSummaryDto>(`/api/v1/content/lessons/by-slug/${slug}`),

  startSession: (quizId: string, lessonType: LessonType, userLocale: string) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start`;
    const params = { lessonId: quizId };

    return api.post<StartSessionResponse>(url, null, {
      params: params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  startOrResumeSession: (quizId: string, lessonType: LessonType, userLocale: string) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start-or-resume`;
    const params = { lessonId: quizId };

    return api.post<StartOrResumeResponse>(url, null, {
        params: params,
        headers: { 'X-User-Locale': userLocale },
    });
  },

  resumeSession: (sessionId: string, lessonType: LessonType, userLocale: string) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/resume`;
    return api.get<ResumeSessionResponse>(url, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  submitAnswer: (sessionId: string, quizId: string, lessonType: LessonType, answer: AnswerRequest, userLocale: string) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/answer`;

    return api.post<AnswerResponse>(url, answer, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  completeSession: (sessionId: string, lessonType: LessonType, userLocale: string) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/complete`;
    return api.post(url, null, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  retakeSession: (sessionId: string, lessonType: LessonType, slug: string, userLocale: string) => {
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/retake`;
    return api.post<StartOrResumeResponse>(url, null, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  startNewQuizSession: (sessionId: string, lessonType: LessonType, slug: string, userLocale: string) => {
    const url = `/api/v1/quiz/${slug}/sessions/${sessionId}/new-quiz`;
    return api.post<StartOrResumeResponse>(url, null, {
      headers: { 'X-User-Locale': userLocale },
    });
  },

  getUserQuizSessions: (userId: string, page: number, size: number, sortBy: string, sortDirection: string, lessonType?: LessonType, status?: SessionStatus) => {
    return api.get<PaginatedResponse<QuizSessionSummary>>(`/api/v1/quiz-sessions`, {
      params: {
        userId,
        page,
        size,
        sortBy,
        sortDirection,
        lessonType,
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

  getAllSandhiRules: () => {
    return api.get<SandhiRuleDto[]>('/api/v1/eamenau/sandhi-rules');
  },
};

