import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sangrahaApi } from '../api/sangraha';
import type {
  WorkSummaryDto,
  WorkTreeDto,
  VerseDetailDto,
  CreateWorkRequest,
  CreateChapterRequest,
  CreateVerseRequest,
  UpdateVerseTextRequest,
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

export const useVerseDetail = (verseId: string) =>
  useQuery<VerseDetailDto, Error>({
    queryKey: ['sangraha', 'verse', verseId],
    queryFn: async () => {
      const res = await sangrahaApi.getVerseDetail(verseId);
      return res.data;
    },
    enabled: !!verseId,
  });

export const useCreateWork = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: CreateWorkRequest) => sangrahaApi.createWork(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sangraha', 'works'] }),
  });
};

export const useDeleteWork = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (workId: string) => sangrahaApi.deleteWork(workId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sangraha', 'works'] }),
  });
};

export const useCreateChapter = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ workId, data }: { workId: string; data: CreateChapterRequest }) =>
      sangrahaApi.createChapter(workId, data),
    onSuccess: (_data, variables) =>
      qc.invalidateQueries({ queryKey: ['sangraha', 'work'] }),
  });
};

export const useDeleteChapter = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (chapterId: string) => sangrahaApi.deleteChapter(chapterId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sangraha', 'work'] }),
  });
};

export const useCreateVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ chapterId, data }: { chapterId: string; data: CreateVerseRequest }) =>
      sangrahaApi.createVerse(chapterId, data),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sangraha', 'work'] }),
  });
};

export const useDeleteVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (verseId: string) => sangrahaApi.deleteVerse(verseId),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sangraha', 'work'] }),
  });
};

export const useUpdateVerseText = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ verseId, data }: { verseId: string; data: UpdateVerseTextRequest }) =>
      sangrahaApi.updateVerseText(verseId, data),
    onSuccess: (_data, variables) =>
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', variables.verseId] }),
  });
};

export const useAnalyzeVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (verseId: string) => sangrahaApi.analyzeVerse(verseId),
    onSuccess: (_data, verseId) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', verseId] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'work'] });
    },
  });
};
