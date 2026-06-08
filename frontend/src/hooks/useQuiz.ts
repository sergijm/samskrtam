import { useQuery, useMutation } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { StartSessionResponse, AnswerRequest, AnswerResponse, QuizSummaryDto, QuizListItem, QuizType, ResumeSessionResponse } from '../types/quiz'; // Import ResumeSessionResponse
import { useLocaleStore } from '../store/localeStore'; // Import useLocaleStore

export const useQuizList = (category?: string) => {
  const { locale } = useLocaleStore(); // Get current locale
  return useQuery<QuizListItem[], Error>({
    queryKey: ['quizzes', 'list', category, locale], // Add locale to queryKey
    queryFn: async () => {
      const response = await quizApi.getQuizList(category);
      return response.data;
    },
  });
};

export const useQuizBySlug = (slug: string) => {
  const { locale } = useLocaleStore(); // Get current locale
  return useQuery<QuizSummaryDto, Error>({
    queryKey: ['quizzes', slug, locale], // Add locale to queryKey
    queryFn: async () => {
      const response = await quizApi.getQuizBySlug(slug);
      return response.data;
    },
    enabled: !!slug, // Only run the query if slug is provided
  });
};

export const useStartQuizSession = () => {
  const { locale } = useLocaleStore(); // Get current locale
  return useMutation<
    StartSessionResponse,
    Error,
    { quizIdentifier: string; quizType: QuizType }
  >({
    mutationFn: async ({ quizIdentifier, quizType }) => {
      const response = await quizApi.startSession(quizIdentifier, quizType, locale);
      return response.data;
    },
  });
};

export const useResumeQuizSession = () => {
  const { locale } = useLocaleStore(); // Get current locale
  return useMutation<
    ResumeSessionResponse,
    Error,
    { sessionId: string; quizType: QuizType }
  >({
    mutationFn: async ({ sessionId, quizType }) => {
      const response = await quizApi.resumeSession(sessionId, quizType, locale);
      return response.data;
    },
  });
};

export const useSubmitQuizAnswer = () => {
  const { locale } = useLocaleStore(); // Get current locale
  return useMutation<
    AnswerResponse,
    Error,
    { sessionId: string; quizIdentifier: string; quizType: QuizType; answerRequest: AnswerRequest }
  >({
    mutationFn: async ({ sessionId, quizIdentifier, quizType, answerRequest }) => {
      const response = await quizApi.submitAnswer(sessionId, quizIdentifier, quizType, answerRequest, locale);
      return response.data;
    },
  });
};
