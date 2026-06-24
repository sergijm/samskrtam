import { useQuery } from '@tanstack/react-query';
import { lessonApi } from '../api/lessonApi';

// Урок по словарю — содержание + статистика пользователя
export const useVocabularyLesson = (slug: string) =>
  useQuery({
    queryKey: ['lesson', 'vocabulary', slug],
    queryFn: () => lessonApi.getVocabularyLesson(slug).then(res => res.data),

    enabled: !!slug,
  });

// Урок по грамматике — содержание + статистика пользователя
export const useGrammarLesson = (type: string) =>
  useQuery({
    queryKey: ['lesson', 'grammar', type],
    queryFn: () => lessonApi.getGrammarLesson(type).then(res => res.data),
    enabled: !!type,
  });

// История ответов на конкретное слово в уроке
export const useWordHistory = (quizId: string, wordId: string) =>
  useQuery({
    queryKey: ['word-history', quizId, wordId],
    queryFn: () => lessonApi.getWordHistory(quizId, wordId).then(res => res.data),
    enabled: !!wordId && !!quizId,
  });

// История ответов на конкретный вопрос в уроке
export const useQuestionHistory = (quizId: string, questionId: string) =>
  useQuery({
    queryKey: ['question-history', quizId, questionId],
    queryFn: () => lessonApi.getQuestionHistory(quizId, questionId).then(res => res.data),
    enabled: !!questionId && !!quizId,
  });