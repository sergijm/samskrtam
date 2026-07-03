import api from './axios';
import type {
  WorkSummaryDto,
  WorkTreeDto,
  VerseDetailDto,
  CreateWorkRequest,
  UpdateWorkRequest,
  CreateChapterRequest,
  UpdateChapterRequest,
  CreateVerseRequest,
  UpdateVerseTextRequest,
} from '../types/sangraha';

const BASE = '/api/v1/sangraha';

export const sangrahaApi = {
  // Works
  getAllWorks: () => api.get<WorkSummaryDto[]>(`${BASE}/works`),

  getWorkTree: (workSlug: string) =>
    api.get<WorkTreeDto>(`${BASE}/works/${workSlug}`),

  createWork: (data: CreateWorkRequest) =>
    api.post<WorkSummaryDto>(`${BASE}/works`, data),

  updateWork: (workSlug: string, data: UpdateWorkRequest) =>
    api.put<WorkSummaryDto>(`${BASE}/works/${workSlug}`, data),

  deleteWork: (workSlug: string) => api.delete(`${BASE}/works/${workSlug}`),

  // Chapters
  createChapter: (workSlug: string, data: CreateChapterRequest) =>
    api.post(`${BASE}/works/${workSlug}/chapters`, data),

  updateChapter: (chapterId: string, data: UpdateChapterRequest) =>
    api.put(`${BASE}/chapters/${chapterId}`, data),

  deleteChapter: (chapterId: string) =>
    api.delete(`${BASE}/chapters/${chapterId}`),

  // Verses
  createVerse: (chapterId: string, data: CreateVerseRequest) =>
    api.post(`${BASE}/chapters/${chapterId}/verses`, data),

  getVerseDetail: (verseId: string) =>
    api.get<VerseDetailDto>(`${BASE}/verses/${verseId}`),

  updateVerseText: (verseId: string, data: UpdateVerseTextRequest) =>
    api.put(`${BASE}/verses/${verseId}/text`, data),

  analyzeVerse: (verseId: string) =>
    api.post(`${BASE}/verses/${verseId}/analyze`),

  deleteVerse: (verseId: string) =>
    api.delete(`${BASE}/verses/${verseId}`),
};
