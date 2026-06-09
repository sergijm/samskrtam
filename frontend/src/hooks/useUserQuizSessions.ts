import { useQuery } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { QuizType, SessionStatus, QuizSessionSummary, PaginatedResponse, AnswerHistory } from '../types/quiz';

export const useUserQuizSessions = (
  userId: string, // userId is now passed as a query parameter
  page: number,
  size: number,
  sortBy: string,
  sortDirection: string,
  quizType?: QuizType,
  status?: SessionStatus
) => {
  return useQuery<PaginatedResponse<QuizSessionSummary>, Error>({
    queryKey: ['userQuizSessions', { userId, page, size, sortBy, sortDirection, quizType, status }], // userId is part of the query key
    queryFn: async () => {
      const response = await quizApi.getUserQuizSessions(userId, page, size, sortBy, sortDirection, quizType, status);
      return response.data;
    },
    enabled: !!userId, // Only run the query if userId is available
    keepPreviousData: true, // Keep previous data while fetching new data for pagination/filters
  });
};

export const useSessionAnswerHistory = (
  sessionId: string,
  userId: string,
) => { // Removed pagination parameters
  return useQuery<AnswerHistory[], Error>({ // Expects List<AnswerHistory>
    queryKey: ['sessionAnswerHistory', { sessionId, userId }], // Removed pagination parameters from queryKey
    queryFn: async () => {
      const response = await quizApi.getSessionAnswerHistory(sessionId, userId); // Removed pagination parameters
      return response.data;
    },
    enabled: !!sessionId && !!userId, // Only run the query if sessionId and userId are available
  });
};

// New hook to fetch a single QuizSessionSummary by sessionId and userId
export const useQuizSessionSummary = (sessionId: string, userId: string) => {
  return useQuery<QuizSessionSummary, Error>({
    queryKey: ['quizSessionSummary', { sessionId, userId }],
    queryFn: async () => {
      const response = await quizApi.getQuizSessionSummary(sessionId); // Use new API method
      return response.data;
    },
    enabled: !!sessionId && !!userId,
  });
};
