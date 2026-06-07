import { useQuery } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { AnswerHistory, PaginatedResponse } from '../types/quiz';

export const useSessionAnswerHistory = (
  sessionId: string,
  userId: string,
  page: number,
  size: number,
  sortBy: string,
  sortDirection: string
) => {
  return useQuery<PaginatedResponse<AnswerHistory>, Error>({
    queryKey: ['sessionAnswerHistory', sessionId, userId, { page, size, sortBy, sortDirection }],
    queryFn: async () => {
      const response = await quizApi.getSessionAnswerHistory(sessionId, userId, page, size, sortBy, sortDirection);
      return response.data;
    },
    enabled: !!sessionId && !!userId, // Only run the query if sessionId and userId are available
    keepPreviousData: true, // Keep previous data while fetching new data for pagination/filters
  });
};
