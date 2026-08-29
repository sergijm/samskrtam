import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { quizApi, FilterParams } from '../api/quizApi';
import { StartSessionResponse, AnswerRequest, AnswerResponse, QuizSummaryDto, LessonItemDto, LessonType, ResumeSessionResponse, StartOrResumeResponse, ProgressSummaryDto } from '../types/quiz';
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
    queryKey: ['quizzes', 'list', 'GRAMMAR', locale],
    queryFn: async () => {
      const response = await quizApi.getQuizList('GRAMMAR');
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

export const useProgressSummary = (scope: string) =>
  useQuery<ProgressSummaryDto, Error>({
    queryKey: ['progressSummary', scope],
    queryFn: async () => {
      const response = await quizApi.getProgressSummary(scope);
      return response.data;
    },
  });

export const useComposeQuizSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    ComposeQuizResponse,
    Error,
    { topicCode: string; count: number; progressTagSetId?: string }
  >({
    mutationFn: async ({ topicCode, count, progressTagSetId }) => {
      const request: ComposeQuizRequest = {
        topicCode,
        progressTagSetId,
        limit: count,
        userLocale: locale,
      };
      const response = await quizApi.composeSession(request);
      return response.data;
    },
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

export const useStartOrResumeQuizSessionWithFilters = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartOrResumeResponse,
    Error,
    { quizId: string; lessonType: LessonType; filters: FilterParams }
  >({
    mutationFn: async ({ quizId, lessonType, filters }) => {
      const response = await quizApi.startOrResumeSessionWithFilters(quizId, lessonType, locale, filters);
      return response.data;
    },
  });
};

export const useStartOrResumeWithStatusFilter = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartOrResumeResponse,
    Error,
    { quizId: string; lessonType: LessonType; statusFilter: string }
  >({
    mutationFn: async ({ quizId, lessonType, statusFilter }) => {
      const response = await quizApi.startOrResumeWithStatusFilter(quizId, lessonType, locale, statusFilter);
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

export const useStartOrResumeAllStemsSession = () => {
  const { locale } = useLocaleStore();
  return useMutation<
    StartOrResumeResponse,
    Error,
    { filterVowelTypes?: string[]; filterNumberTypes?: string[]; filterGenders?: string[]; filterCaseTypes?: string[] }
  >({
    mutationFn: async ({ filterVowelTypes, filterNumberTypes, filterGenders, filterCaseTypes }) => {
      const response = await quizApi.startOrResumeAllStemsSession(
        locale,
        filterVowelTypes,
        filterNumberTypes,
        filterGenders,
        filterCaseTypes,
      );
      return response.data;
    },
  });
};

