import { useQuery, keepPreviousData, useQueries } from '@tanstack/react-query';
import { lessonApi } from '../api/lessonApi';
import { sangrahaApi } from '../api/sangraha';
import { sandhiApi } from '../api/sandhiApi';
import { contentApi } from '../api/contentApi';
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
 * Examples for the declension lesson — one request to sangraha-service per lesson.
 * Lazy: fires only when enabled (active tab === 'examples') and the lesson's
 * stem class (vowelType, gender) is already known from the paradigm page.
 */
export const useDeclensionExamples = (
  slug: string,
  vowelType: string,
  gender: string,
  enabled: boolean,
) =>
  useQuery({
    queryKey: ['declension-examples', slug, vowelType, gender],
    queryFn: () => sangrahaApi.getDeclensionExamples(vowelType, gender).then(res => res.data),
    enabled: !!slug && !!vowelType && !!gender && enabled,
    staleTime: Infinity,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
  });

/**
 * One conjugation paradigm page by index — lazy, fired from the verb-lesson
 * "Paradigms" carousel. The carousel advances the index.
 */
export const useConjugationParadigm = (
  slug: string,
  index: number,
  voice: string,
  enabled: boolean,
) =>
  useQuery({
    queryKey: ['conjugation-paradigm', slug, index, voice],
    queryFn: () => lessonApi.getConjugationParadigm(slug, index, voice).then(res => res.data),
    enabled: !!slug && enabled,
    staleTime: Infinity,
    refetchOnMount: false,
    refetchOnWindowFocus: false,
    refetchOnReconnect: false,
    placeholderData: keepPreviousData,
  });

export const useSandhiRules = (topicCode: string) =>
  useQuery({
    queryKey: ['sandhi-rules', topicCode],
    queryFn: () => sandhiApi.getRules(topicCode).then(res => res.data),
    enabled: !!topicCode,
  });

export const useSandhiRulesByNumbers = (ruleNumbers: number[]) =>
  useQuery({
    queryKey: ['sandhi-rules-by-numbers', ruleNumbers],
    queryFn: async () => {
      const res = await contentApi.getAllSandhiRules();
      const all = res.data.rules;
      const byNumber = new Map(all.map(r => [r.number, r]));

      const selected = new Set<number>();
      const requested = new Set(ruleNumbers);
      const stack = [...ruleNumbers];
      while (stack.length) {
        const n = stack.pop()!;
        if (selected.has(n)) continue;
        selected.add(n);
        const rule = byNumber.get(n);
        rule?.dependsOn?.forEach(d => stack.push(d));
      }

      const requestedRules = ruleNumbers
        .filter(n => byNumber.has(n))
        .map(n => byNumber.get(n)!);
      const dependencyRules = all
        .filter(r => selected.has(r.number) && !requested.has(r.number))
        .sort((a, b) => a.number - b.number);

      return { topicCode: res.data.topicCode, title: res.data.title, rules: [...requestedRules, ...dependencyRules] };
    },
    enabled: ruleNumbers.length > 0,
  });

