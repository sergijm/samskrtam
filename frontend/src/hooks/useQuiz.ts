import { useQuery, useMutation } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { StartSessionResponse } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/StartSessionResponse';
import { AnswerRequest } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/AnswerRequest';
import { AnswerResponse } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/AnswerResponse';
import { QuizSummaryDto } from '../../shared/quiz-content-dtos/src/main/java/sm/selflearn/samskrtam/content/dto/QuizSummaryDto';

export const useQuizList = (category?: string) =>
  useQuery({
    queryKey: ['quizzes', 'list', category],
    queryFn: async () => {
      const response = await quizApi.getQuizList(category);
      return response.data;
    },
  });

export const useQuizBySlug = (slug: string) =>
  useQuery<QuizSummaryDto, Error>({
    queryKey: ['quizzes', slug],
    queryFn: async () => {
      const response = await quizApi.getQuizBySlug(slug);
      return response.data;
    },
    enabled: !!slug, // Only run the query if slug is provided
  });

export const useStartQuizSession = () => {
  return useMutation<
    StartSessionResponse,
    Error,
    { quizId: string; quizType: string; userLocale: string }
  >({
    mutationFn: async ({ quizId, quizType, userLocale }) => {
      const response = await quizApi.startSession(quizId, quizType, userLocale);
      return response.data;
    },
  });
};

export const useSubmitQuizAnswer = () => {
  return useMutation<
    AnswerResponse,
    Error,
    { sessionId: string; quizType: string; answerRequest: AnswerRequest; userLocale: string }
  >({
    mutationFn: async ({ sessionId, quizType, answerRequest, userLocale }) => {
      const response = await quizApi.submitAnswer(sessionId, quizType, answerRequest, userLocale);
      return response.data;
    },
  });
};
