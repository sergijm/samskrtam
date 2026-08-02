import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sangrahaApi } from '../api/sangraha';

import type {
  WorkSummaryDto,
  WorkTreeDto,
  ChapterVersesDto,
  VerseDetailDto,
  VerseBatchResponseDto,
} from '../types/sangraha';

export const useWorks = () =>
  useQuery<WorkSummaryDto[], Error>({
    queryKey: ['sangraha', 'works'],
    queryFn: async () => {
      const res = await sangrahaApi.getAllWorks();
      return res.data;
    },
  });

export const useWorkTree = (workSlug: string) =>
  useQuery<WorkTreeDto, Error>({
    queryKey: ['sangraha', 'work', workSlug],
    queryFn: async () => {
      const res = await sangrahaApi.getWorkTree(workSlug);
      return res.data;
    },
    enabled: !!workSlug,
  });

export const useChapterVerses = (chapterId: string) =>
  useQuery<ChapterVersesDto, Error>({
    queryKey: ['sangraha', 'chapter', chapterId],
    queryFn: async () => {
      const res = await sangrahaApi.getChapterVerses(chapterId);
      return res.data;
    },
    enabled: !!chapterId,
  });

export const useVerseDetail = (verseId: string) =>
  useQuery<VerseDetailDto, Error>({
    queryKey: ['sangraha', 'verse', verseId],
    queryFn: async () => {
      const res = await sangrahaApi.getVerseDetail(verseId);
      return res.data;
    },
    enabled: !!verseId,
  });

export const useAnalyzeVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ verseId, text }: { verseId: string; text?: string }) =>
      sangrahaApi.analyzeVerse(verseId, text ? { text } : undefined),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', vars.verseId] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'work'] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'chapter'] });
    },
  });
};

export const useGetOrCreateVocabularyQuiz = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (verseId: string) => sangrahaApi.getOrCreateVocabularyQuiz(verseId),
    onSuccess: (_data, verseId) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', verseId] });
    },
  });
};

export const useAnalyzeAllVerses = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (chapterId: string) => sangrahaApi.analyzeAllVerses(chapterId),
    onSuccess: (_data, chapterId) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'chapter', chapterId] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'work'] });
    },
  });
};

// ── Batch verse review (sangraha-service/batch-verse-review.md) ──

export const useVersesBatch = (ids: string[]) =>
  useQuery<VerseBatchResponseDto, Error>({
    queryKey: ['sangraha', 'verse-batch', ids],
    queryFn: async () => {
      const res = await sangrahaApi.getVersesBatch(ids);
      return res.data;
    },
    enabled: ids.length > 0,
  });

export const useAnalyzeVerses = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (verseIds: string[]) => sangrahaApi.analyzeVerses(verseIds),
    // Инвалидация без конкретных ids — задевает любой открытый список стихов.
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse-batch'] });
    },
  });
};

