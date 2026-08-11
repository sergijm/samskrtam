import { useQuery } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';

export const useSessionAnswerHistory = (
  sessionId: string,
  userId: string,
) => {
  return useQuery<{ questions: Array<{ questionNumber: number; text: string; correctAnswer: string | null; questionType: string }> }, Error>({
    queryKey: ['sessionQuestions', { sessionId }],
    queryFn: async () => {
      const response = await quizApi.getSessionQuestions(sessionId);
      return { questions: response.data };
    },
    enabled: !!sessionId,
  });
};

export const useQuizSessionSummary = (sessionId: string, userId: string) => {
  return useQuery<{
    id: string;
    totalQuestions: number;
    answeredQuestions: number;
    score: number;
    status: string;
    slug?: string;
    lessonType?: string;
    sessionId: string;
    startedAt?: string;
    completedAt?: string;
  }, Error>({
    queryKey: ['quizSessionSummary', { sessionId }],
    queryFn: async () => {
      const response = await quizApi.getSession(sessionId);
      const s = response.data as any;
      return {
        id: s.id,
        totalQuestions: s.totalQuestions,
        answeredQuestions: s.answeredQuestions,
        score: s.score,
        status: s.status,
        slug: s.lessonId ?? undefined,
        lessonType: s.lessonType ?? undefined,
        sessionId: s.id,
        startedAt: s.startedAt,
        completedAt: s.completedAt,
      };
    },
    enabled: !!sessionId && !!userId,
  });
};