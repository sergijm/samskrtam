import { useQuery } from '@tanstack/react-query';
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

export const useQuestionHistory = (slug: string, caseType: string, numberType: string) =>
  useQuery({
    queryKey: ['question-history', slug, caseType, numberType],
    queryFn: () => lessonApi.getQuestionHistory(slug, caseType, numberType).then(res => res.data),
    enabled: !!slug && !!caseType && !!numberType,
  });