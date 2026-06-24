import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { StartSessionResponse, AnswerRequest, AnswerResponse, QuizSummaryDto, QuizListItem, LessonType, ResumeSessionResponse, StartOrResumeResponse } from '../types/quiz';
import { useLocaleStore } from '../store/localeStore';

export const useQuizList = (category?: string) => {
  const { locale } = useLocaleStore();
  return useQuery<QuizListItem[], Error>({
    queryKey: ['quizzes', 'list', category, locale],
    queryFn: async () => {
      const response = await quizApi.getQuizList(category); // Pass category to API call
      return response.data;
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

export const useStartQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartSessionResponse,
    Error,
    { quizIdentifier: string; lessonType: LessonType }
  >({
    mutationFn: async ({ quizIdentifier, quizType }) => {
      const response = await quizApi.startSession(quizIdentifier, quizType, locale);
      return response.data;
    },
  });
};

export const useStartOrResumeQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartOrResumeResponse,
    Error,
    { quizId: string; lessonType: LessonType }
  >({
    mutationFn: async ({ quizId, quizType }) => {
      const response = await quizApi.startOrResumeSession(quizId, quizType, locale);
      return response.data;
    },
  });
};

export const useResumeQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    ResumeSessionResponse,
    Error,
    { sessionId: string; lessonType: LessonType }
  >({
    mutationFn: async ({ sessionId, quizType }) => {
      const response = await quizApi.resumeSession(sessionId, quizType, locale);
      return response.data;
    },
  });
};

export const useSubmitQuizAnswer = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    AnswerResponse,
    Error,
    { sessionId: string; quizIdentifier: string; lessonType: LessonType; answerRequest: AnswerRequest }
  >({
    mutationFn: async ({ sessionId, quizIdentifier, quizType, answerRequest }) => {
      const response = await quizApi.submitAnswer(sessionId, quizIdentifier, quizType, answerRequest, locale);
      return response.data;
    },
  });
};

export const useCompleteQuizSession = () => {
  const { locale } = useLocaleStore();
  const queryClient = useQueryClient();
  return useMutation<
    void,
    Error,
    { sessionId: string; lessonType: LessonType }
  >({
    mutationFn: async ({ sessionId, quizType }) => {
      await quizApi.completeSession(sessionId, quizType, locale);
    },
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries(['quizSessionSummary', variables.sessionId]);
    },
  });
};

export const useRetakeQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartOrResumeResponse,
    Error,
    { sessionId: string; lessonType: LessonType; slug: string }
  >({
    mutationFn: async ({ sessionId, quizType, slug }) => {
      const response = await quizApi.retakeSession(sessionId, quizType, slug, locale);
      return response.data;
    },
  });
};

export const useStartNewQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartOrResumeResponse,
    Error,
    { sessionId: string; lessonType: LessonType; slug: string }
  >({
    mutationFn: async ({ sessionId, quizType, slug }) => {
      const response = await quizApi.startNewQuizSession(sessionId, quizType, slug, locale);
      return response.data;
    },
  });
};
