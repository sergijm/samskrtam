import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { AnswerRequest, AnswerResponse, QuizSummaryDto, LessonItemDto, StartOrResumeResponse } from '../types/quiz';
import { ComposeQuizRequest, ComposeQuizResponse } from '../types/quiz';
import { useLocaleStore } from '../store/localeStore';

export const useQuizList = (category?: string) => {
  const { locale } = useLocaleStore();
  return useQuery<LessonItemDto[], Error>({
    queryKey: ['quizzes', 'list', category, locale],
    queryFn: async () => {
      const response = await quizApi.getQuizList(category);
      return response.data.lessons;
    },
  });
};

export const useDeclensionLessons = () => {
  const { locale } = useLocaleStore();
  return useQuery<LessonItemDto[], Error>({
    queryKey: ['quizzes', 'list', 'DECLENSIONS', locale],
    queryFn: async () => {
      const response = await quizApi.getQuizList('DECLENSIONS');
      return response.data.lessons;
    },
  });
};

export const useConjugationLessons = () => {
  const { locale } = useLocaleStore();
  return useQuery<LessonItemDto[], Error>({
    queryKey: ['quizzes', 'list', 'CONJUGATIONS', locale],
    queryFn: async () => {
      const response = await quizApi.getQuizList('CONJUGATIONS');
      return response.data.lessons;
    },
  });
};

export const useQuizBySlug = (slug: string) => {
  const { locale } = useLocaleStore();
  return useQuery<QuizSummaryDto, Error>({
    queryKey: ['quizzes', slug, locale],
    queryFn: async () => {
      const response = await quizApi.getQuizBySlug(slug);
      return response.data;
    },
    enabled: !!slug,
  });
};

export const useComposeQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    ComposeQuizResponse,
    Error,
    { topicCode: string; count: number }
  >({
    mutationFn: async ({ topicCode, count }) => {
      const request: ComposeQuizRequest = {
        topics: [{ topicCode, count }],
        userLocale: locale,
      };
      const response = await quizApi.composeSession(request);
      return response.data;
    },
  });
};

export const useResumeQuizSession = () => {
  return useMutation<
    StartOrResumeResponse,
    Error,
    { sessionId: string }
  >({
    mutationFn: async ({ sessionId }) => {
      const response = await quizApi.resumeSession(sessionId);
      return response.data;
    },
  });
};

export const useSubmitQuizAnswer = () => {
  return useMutation<
    AnswerResponse,
    Error,
    { sessionId: string; answerRequest: AnswerRequest }
  >({
    mutationFn: async ({ sessionId, answerRequest }) => {
      const response = await quizApi.submitAnswer(sessionId, answerRequest);
      return response.data;
    },
  });
};

export const useCompleteQuizSession = () => {
  const queryClient = useQueryClient();
  return useMutation<
    void,
    Error,
    { sessionId: string }
  >({
    mutationFn: async ({ sessionId }) => {
      await quizApi.completeSession(sessionId);
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries(['quizSessionSummary', variables.sessionId]);
    },
  });
};

export const useRetakeQuizSession = () => {
  return useMutation<
    StartOrResumeResponse,
    Error,
    { sessionId: string }
  >({
    mutationFn: async ({ sessionId }) => {
      const response = await quizApi.retakeSession(sessionId);
      return response.data;
    },
  });
};