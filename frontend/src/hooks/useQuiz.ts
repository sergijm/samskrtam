import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { quizApi } from '../api/quizApi';
import { StartSessionResponse, AnswerRequest, AnswerResponse, QuizSummaryDto, LessonItemDto, LessonType, ResumeSessionResponse, StartOrResumeResponse } from '../types/quiz';
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

export const useStartQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartSessionResponse,
    Error,
    { quizIdentifier: string; lessonType: LessonType }
  >({
    mutationFn: async ({ quizIdentifier, lessonType }) => {
      const response = await quizApi.startSession(quizIdentifier, lessonType, locale);
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
    mutationFn: async ({ quizId, lessonType }) => {
      const response = await quizApi.startOrResumeSession(quizId, lessonType, locale);
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
    mutationFn: async ({ sessionId, lessonType }) => {
      const response = await quizApi.resumeSession(sessionId, lessonType, locale);
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
    mutationFn: async ({ sessionId, quizIdentifier, lessonType, answerRequest }) => {
      const response = await quizApi.submitAnswer(sessionId, quizIdentifier, lessonType, answerRequest, locale);
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
    mutationFn: async ({ sessionId, lessonType }) => {
      await quizApi.completeSession(sessionId, lessonType, locale);
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
    mutationFn: async ({ sessionId, lessonType, slug }) => {
      const response = await quizApi.retakeSession(sessionId, lessonType, slug, locale);
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
    mutationFn: async ({ sessionId, lessonType, slug }) => {
      const response = await quizApi.startNewQuizSession(sessionId, lessonType, slug, locale);
      return response.data;
    },
  });
};

