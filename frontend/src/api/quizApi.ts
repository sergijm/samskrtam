import api from './axios';
import { QuizSummaryDto, AnswerRequest, AnswerResponse, StartOrResumeResponse, LessonListResponse, ComposeQuizRequest, ComposeQuizResponse } from '../types/quiz';

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

  resumeSession: (sessionId: string) =>
    api.get<StartOrResumeResponse>(`/api/v2/quiz/sessions/${sessionId}/resume`),

  submitAnswer: (sessionId: string, answer: AnswerRequest) =>
    api.post<AnswerResponse>(`/api/v2/quiz/sessions/${sessionId}/answer`, answer),

  completeSession: (sessionId: string) =>
    api.post(`/api/v2/quiz/sessions/${sessionId}/complete`),

  retakeSession: (sessionId: string) =>
    api.post<StartOrResumeResponse>(`/api/v2/quiz/sessions/${sessionId}/retake`),

  getSession: (sessionId: string) =>
    api.get(`/api/v2/quiz/sessions/${sessionId}`),

  getSessionQuestions: (sessionId: string) =>
    api.get(`/api/v2/quiz/sessions/${sessionId}/questions`),
};