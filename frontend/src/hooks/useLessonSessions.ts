import { useQuery } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { QuizSessionSummary, PaginatedResponse } from '../types/quiz';

export const useLessonSessions = (
  quizId: string,
  userId: string,
  page: number = 0,
  size: number = 20,
) => {
  return useQuery<PaginatedResponse<QuizSessionSummary>, Error>({
    queryKey: ['lessonSessions', { quizId, userId, page, size }],
    queryFn: async () => {
      const response = await quizApi.getLessonSessions(quizId, userId, page, size);
      return response.data;
    },
    enabled: !!quizId && !!userId,
    keepPreviousData: true,
  });
};
