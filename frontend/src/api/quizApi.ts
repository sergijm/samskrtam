import api from './axios';
import { QuizListItem, QuizSummaryDto, StartSessionResponse, AnswerRequest, AnswerResponse, LessonType, ResumeSessionResponse, StartOrResumeResponse, LessonListResponse, PaginatedResponse, ComposeQuizRequest, ComposeQuizResponse, QuizSessionSummary, AnswerHistory, QuizProgress, SessionStatus } from '../types/quiz';
import { SandhiRuleDto } from '../types/content';
import { ProgressSummaryDto } from '../types/quiz';

export type FilterScope = 'CASE_ONLY' | 'NUMBER_ONLY' | 'CASE_NUMBER_GENDER' | 'ALL_STEMS';

export interface FilterParams {
  filterScope?: FilterScope;
  filterCaseTypes?: string;
  filterNumberTypes?: string;
  filterCombinations?: string;
  filterVowelTypes?: string;
  filterGenders?: string;
}

export const quizApi = {
  getQuizList: (category?: string) => {
    const url = category
      ? `/api/v2/lessons/${category}`
      : '/api/v2/lessons';
    return api.get<LessonListResponse>(url);
  },

  getQuizBySlug: (slug: string) => api.get<QuizSummaryDto>(`/api/v2/lessons/${slug}`),

  composeSession: (request: ComposeQuizRequest) =>
    api.post<ComposeQuizResponse>('/api/v2/quiz/compose', request, {
      headers: { 'Content-Type': 'application/json' },
    }),

  startSession: (quizId: string, lessonType: LessonType, userLocale: string) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start`;
    const params = { lessonId: quizId };
    return api.post<StartSessionResponse>(url, null, {
      params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  startOrResumeSession: (quizId: string, lessonType: LessonType, userLocale: string) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start-or-resume`;
    const params = { lessonId: quizId };
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
    if (filters.filterCaseTypes) params.filterCaseTypes = filters.filterCaseTypes;
    if (filters.filterNumberTypes) params.filterNumberTypes = filters.filterNumberTypes;
    if (filters.filterCombinations) params.filterCombinations = filters.filterCombinations;
    if (filters.filterVowelTypes) params.filterVowelTypes = filters.filterVowelTypes;
    if (filters.filterGenders) params.filterGenders = filters.filterGenders;
    return api.post<StartOrResumeResponse>(url, null, {
      params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  startOrResumeAllStemsSession: (userLocale: string, filterVowelTypes?: string[], filterNumberTypes?: string[], filterGenders?: string[], filterCaseTypes?: string[]) => {
    const url = '/api/v1/quiz/declensions-all/sessions/start-or-resume';
    const params: Record<string, string> = {
      lessonId: '20000000-0000-0000-0000-00000000000b',
      filterScope: 'ALL_STEMS',
    };
    if (filterVowelTypes && filterVowelTypes.length > 0) params.filterVowelTypes = filterVowelTypes.join(',');
    if (filterNumberTypes && filterNumberTypes.length > 0) params.filterNumberTypes = filterNumberTypes.join(',');
    if (filterGenders && filterGenders.length > 0) params.filterGenders = filterGenders.join(',');
    if (filterCaseTypes && filterCaseTypes.length > 0) params.filterCaseTypes = filterCaseTypes.join(',');
    return api.post<StartOrResumeResponse>(url, null, {
      params,
      headers: { 'X-User-Locale': userLocale },
    });
  },

  startOrResumeWithStatusFilter: (
    lessonId: string,
    lessonType: LessonType,
    userLocale: string,
    statusFilter: string,
    filterVowelTypes?: string,
  ) => {
    const slug = lessonType.toLowerCase();
    const url = `/api/v1/quiz/${slug}/sessions/start-or-resume`;
    const params: Record<string, string> = { lessonId, statusFilter };
    if (filterVowelTypes) params.filterVowelTypes = filterVowelTypes;
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

    // Get sessions for a specific lesson (by quizId), used in SessionsTab
  getLessonSessions: (quizId: string, userId: string, page: number = 0, size: number = 20) => {
    return api.get<PaginatedResponse<QuizSessionSummary>>('/api/v1/quiz-sessions', {
      params: { userId, quizId, page, size, sortBy: 'startedAt', sortDirection: 'desc' },
    });
  },

  getAllSandhiRules: () => {
    return api.get<SandhiRuleDto[]>('/api/v1/eamenau/sandhi-rules');
  },

  getProgressSummary: (scope: string) =>
    api.get<ProgressSummaryDto>('/api/v2/quiz/progress/summary', { params: { scope } }),
};

