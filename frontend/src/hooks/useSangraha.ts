import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sangrahaApi } from '../api/sangraha';

import type {
  WorkSummaryDto,
  WorkTreeDto,
  ChapterVersesDto,
  VerseDetailDto,
  VerseBatchResponseDto,
  WorksClassGroupDto,
  StandaloneVerseItemDto,
  VerseWordExamplesResponseDto,
  SourceDto,
} from '../types/sangraha';

export const useWorks = (classIds?: string[], sourceCode?: string) =>
  useQuery<WorkSummaryDto[], Error>({
    queryKey: ['sangraha', 'works', classIds ?? [], sourceCode ?? null],
    queryFn: async () => {
      const res = await sangrahaApi.getAllWorks(classIds, sourceCode);
      return res.data;
    },
  });

export const useWorksClasses = () =>
  useQuery<WorksClassGroupDto[], Error>({
    queryKey: ['sangraha', 'works', 'classes'],
    queryFn: async () => {
      const res = await sangrahaApi.getWorksClasses();
      return res.data;
    },
  });

export const useSources = () =>
  useQuery<SourceDto[], Error>({
    queryKey: ['sangraha', 'sources'],
    queryFn: async () => {
      const res = await sangrahaApi.getSources();
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

// ── Примеры стихов по точным словоформам (урок склонений) ──

export const useWordExamples = (surfaceIasts: string[], enabled: boolean) =>
  useQuery<VerseWordExamplesResponseDto, Error>({
    queryKey: ['sangraha', 'word-examples', surfaceIasts],
    queryFn: async () => {
      const res = await sangrahaApi.getWordExamples(surfaceIasts);
      return res.data;
    },
    enabled: enabled && surfaceIasts.length > 0,
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

// ── Standalone анализ (страница /analysis, verse.chapter_id = null) ──

export const useStandaloneVerses = () =>
  useQuery<StandaloneVerseItemDto[], Error>({
    queryKey: ['sangraha', 'analysis', 'list'],
    queryFn: async () => {
      const res = await sangrahaApi.getStandaloneVerses();
      return res.data;
    },
  });

export const useCreateStandaloneAnalysis = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (text: string) => sangrahaApi.createStandaloneAnalysis(text),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'analysis', 'list'] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', data.data.id] });
    },
  });
};

export const useDeleteStandaloneVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (verseId: string) => sangrahaApi.deleteStandaloneVerse(verseId),
    onSuccess: (_data, verseId) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'analysis', 'list'] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', verseId] });
    },
  });
};

// ── Write: произведения / главы / стихи (ADMIN) ──

export const useCreateWork = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Parameters<typeof sangrahaApi.createWork>[0]) =>
      sangrahaApi.createWork(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'works'] });
    },
  });
};

export const useUpdateWork = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ workSlug, data }: { workSlug: string; data: Parameters<typeof sangrahaApi.updateWork>[1] }) =>
      sangrahaApi.updateWork(workSlug, data),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'works'] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'work', vars.workSlug] });
    },
  });
};

export const useCreateChapter = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ workSlug, data }: { workSlug: string; data: Parameters<typeof sangrahaApi.createChapter>[1] }) =>
      sangrahaApi.createChapter(workSlug, data),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'work', vars.workSlug] });
    },
  });
};

export const useUpdateChapter = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ chapterId, data }: { chapterId: string; data: Parameters<typeof sangrahaApi.updateChapter>[1] }) =>
      sangrahaApi.updateChapter(chapterId, data),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'chapter', vars.chapterId] });
    },
  });
};

export const useCreateVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ chapterId, text }: { chapterId: string; text: string }) =>
      sangrahaApi.createVerse(chapterId, { text }),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'chapter', vars.chapterId] });
    },
  });
};

