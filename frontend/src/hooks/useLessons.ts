import { useQuery, keepPreviousData, useQueries } from '@tanstack/react-query';
import { lessonApi } from '../api/lessonApi';
import { sandhiApi } from '../api/sandhiApi';
import type { DeclensionParadigmPageDto } from '../types/content-dtos';

export const useVocabularyLesson = (slug: string) =>
  useQuery({
    queryKey: ['lesson', 'vocabulary', slug],
    queryFn: () => lessonApi.getVocabularyLesson(slug).then(res => res.data),
    enabled: !!slug,
  });

export const useGrammarLesson = (slug: string) =>
  useQuery({
    queryKey: ['lesson', 'grammar', slug],
    queryFn: () => lessonApi.getGrammarLesson(slug).then(res => res.data),
    enabled: !!slug,
  });

export const useWordHistory = (slug: string, wordId: string) =>
  useQuery({
    queryKey: ['word-history', slug, wordId],
    queryFn: () => lessonApi.getWordHistory(slug, wordId).then(res => res.data),
    enabled: !!wordId && !!slug,
  });

export const useQuestionHistory = (slug: string, caseType: string, numberType: string, gender: string) =>
  useQuery({
    queryKey: ['question-history', slug, caseType, numberType, gender],
    queryFn: () => lessonApi.getQuestionHistory(slug, caseType, numberType, gender).then(res => res.data),
    enabled: !!slug && !!caseType && !!numberType && !!gender,
  });

/**
 * One paradigm page by index — lazy, loaded when the "Paradigms" tab is opened.
 * The carousel component advances the index; this hook fetches only the current page.
 */
export const useDeclensionParadigm = (slug: string, index: number, enabled: boolean) =>
  useQuery({
    queryKey: ['declension-paradigm', slug, index],
    queryFn: () => lessonApi.getDeclensionParadigm(slug, index).then(res => res.data),
    enabled: !!slug && enabled,
    staleTime: Infinity,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    placeholderData: keepPreviousData,
  });

/**
 * Fetches every paradigm page (index 0..totalCount-1) of a lesson in parallel.
 * Uses the same queryKey as useDeclensionParadigm, so pages already loaded by
 * the carousel are reused from the React Query cache (no duplicate requests).
 */
export const useAllDeclensionParadigms = (slug: string, totalCount: number, enabled: boolean) => {
  const results = useQueries({
    queries: Array.from({ length: Math.max(0, totalCount) }, (_, index) => ({
      queryKey: ['declension-paradigm', slug, index],
      queryFn: () => lessonApi.getDeclensionParadigm(slug, index).then(res => res.data),
      enabled: !!slug && enabled && totalCount > 0,
      staleTime: Infinity,
      refetchOnMount: false,
      refetchOnWindowFocus: false,
      refetchOnReconnect: false,
    })),
  });

  const pages = results
    .map(r => r.data)
    .filter((d): d is DeclensionParadigmPageDto => d !== undefined);
  const isLoading = results.some(r => r.isLoading);

  return { pages, isLoading };
};

/**
 * Examples for the declension lesson — one request per entire lesson, no index.
 * Lazy: fires only when enabled (active tab === 'examples').
 */
export const useDeclensionExamples = (slug: string, enabled: boolean) =>
  useQuery({
    queryKey: ['declension-examples', slug],
    queryFn: () => lessonApi.getDeclensionExamples(slug).then(res => res.data),
    enabled: !!slug && enabled,
    staleTime: Infinity,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

export const useSandhiRules = (topicCode: string) =>
  useQuery({
    queryKey: ['sandhi-rules', topicCode],
    queryFn: () => sandhiApi.getRules(topicCode).then(res => res.data),
    enabled: !!topicCode,
  });

