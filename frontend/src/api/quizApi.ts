import api from './axios';
import { QuizListItem, QuizSummaryDto, StartSessionResponse, AnswerRequest, AnswerResponse, LessonType, QuizSessionSummary, AnswerHistory, QuizProgress, ResumeSessionResponse, StartOrResumeResponse, LessonListResponse } from '../types/quiz';
import { PaginatedResponse } from '../types/common';

export type FilterScope = 'CASE_ONLY' | 'CASE_NUMBER_GENDER';

export interface FilterParams {
  filterScope?: 'CASE_ONLY' | 'CASE_NUMBER_GENDER';
  filterCaseType?: string;
  filterNumberType?: string;
  filterGender?: string;
}

export const quizApi = {
  getQuizList: (category?: string) => {
    const url = category
      ? `/api/v1/lessons/${category}`
      : '/api/v1/lessons';
    return api.get<LessonListResponse>(url);
  },

  getQuizBySlug: (slug: string) => api.get<QuizSummaryDto>(`/api/v1/content/lessons/${slug}`),

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

  startOrResumeFilteredSession: (
    lessonId: string,
    userLocale: string,
    filterScope: FilterScope,
    filterCaseType: string,
    filterNumberType?: string,
    filterGender?: string
  ) => {
    const slug = 'declensions';
    const url = `/api/v1/quiz/${slug}/sessions/start-or-resume`;
    const params: Record<string, string> = {
      lessonId,
      filterScope,
      filterCaseType,
    };
    if (filterNumberType) params.filterNumberType = filterNumberType;
    if (filterGender) params.filterGender = filterGender;
    return api.post<StartOrResumeResponse>(url, null, {
      params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  startOrResumeSessionWithFilters: (quizId: string, lessonType: LessonType, userLocale: string, filters: FilterParams) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start-or-resume`;
    const params: Record<string, string> = { lessonId: quizId };
    if (filters.filterScope) params.filterScope = filters.filterScope;
    if (filters.filterCaseType) params.filterCaseType = filters.filterCaseType;
    if (filters.filterNumberType) params.filterNumberType = filters.filterNumberType;
    if (filters.filterGender) params.filterGender = filters.filterGender;
    return api.post<StartOrResumeResponse>(url, null, {
        params,
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

    startOrResumeWithStatusFilter: (
    lessonId: string,
    lessonType: LessonType,
    userLocale: string,
    statusFilter: string,
  ) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start-or-resume`;
    const params: Record<string, string> = { lessonId, statusFilter };
    return api.post<StartOrResumeResponse>(url, null, {
      params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  getAllSandhiRules: () => {
    return api.get<SandhiRuleDto[]>('/api/v1/eamenau/sandhi-rules');
  },
};

