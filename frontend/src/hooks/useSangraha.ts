import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { sangrahaApi } from '../api/sangraha';
import type {
  WorkSummaryDto,
  WorkTreeDto,
  VerseDetailDto,
  CreateWorkRequest,
  UpdateWorkRequest,
  CreateChapterRequest,
  CreateVerseRequest,
  UpdateVerseTextRequest,
  UpdateVerseRequest,
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

export const useUpdateWork = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ workSlug, data }: { workSlug: string; data: UpdateWorkRequest }) =>
      sangrahaApi.updateWork(workSlug, data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'works'] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'work'] });
    },
  });
};

export const useDeleteWork = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (workSlug: string) => sangrahaApi.deleteWork(workSlug),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['sangraha', 'works'] }),
  });
};

export const useCreateChapter = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ workSlug, data }: { workSlug: string; data: CreateChapterRequest }) =>
      sangrahaApi.createChapter(workSlug, data),
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

export const useUpdateVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ verseId, data }: { verseId: string; data: UpdateVerseRequest }) =>
      sangrahaApi.updateVerse(verseId, data),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', variables.verseId] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'work'] });
    },
  });
};

export const useAnalyzeVerse = () => {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ verseId, data }: { verseId: string; data: { text: string } }) =>
      sangrahaApi.analyzeVerse(verseId, data),
    onSuccess: (_data, variables) => {
      qc.invalidateQueries({ queryKey: ['sangraha', 'verse', variables.verseId] });
      qc.invalidateQueries({ queryKey: ['sangraha', 'work'] });
    },
  });
};

