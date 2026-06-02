import { useQuery } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';

export const useQuizList = () =>
  useQuery({
    queryKey: ['quizzes', 'list'],
    queryFn: async () => { // Make queryFn async
      const response = await quizApi.getQuizList(); // Get the full Axios response
      return response.data; // Return only the data part (the array of quizzes)
    },
  });
