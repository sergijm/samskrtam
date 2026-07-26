import { useQuery, keepPreviousData } from '@tanstack/react-query';
import { lessonApi } from '../api/lessonApi';

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

