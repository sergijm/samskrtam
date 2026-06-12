import { useQuery } from '@tanstack/react-query';
import api from '../api/axios'; // Corrected import to default export
import { UserQuizStatisticDto } from '../types/statistics';

export const useUserStatistics = (userId: string | undefined) => {
  return useQuery<UserQuizStatisticDto[], Error>({
    queryKey: ['userStatistics', userId],
    queryFn: async () => {
      if (!userId) {
        throw new Error('User ID is required to fetch statistics');
      }
      const response = await api.get(`/api/v1/statistics/users/${userId}/quizzes`);
      return response.data;
    },
    enabled: !!userId, // Only run the query if userId is available
  });
};
