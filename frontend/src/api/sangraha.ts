import api from './axios';
import type {
  WorkSummaryDto,
  WorkTreeDto,
  ChapterVersesDto,
  VerseDetailDto,
  VerseBatchResponseDto,
} from '../types/sangraha';

const BASE = '/api/v1/sangraha';

export const sangrahaApi = {
  // Works (read-only)
  getAllWorks: () => api.get<WorkSummaryDto[]>(`${BASE}/works`),

  getWorkTree: (workSlug: string) =>
    api.get<WorkTreeDto>(`${BASE}/works/${workSlug}`),

  // Chapters: single chapter with its verses
  getChapterVerses: (chapterId: string) =>
    api.get<ChapterVersesDto>(`${BASE}/chapters/${chapterId}/verses`),

  // Verses (read-only + analyze + vocabulary-quiz)
  getVerseDetail: (verseId: string) =>
    api.get<VerseDetailDto>(`${BASE}/verses/${verseId}`),

  analyzeVerse: (verseId: string, data?: { text: string }) =>
    api.post(`${BASE}/verses/${verseId}/analyze`, data),

  analyzeAllVerses: (chapterId: string) =>
    api.post<{ chapterId: string; verseIds: string[] }>(`${BASE}/chapters/${chapterId}/verses/analyze-all`),

    getOrCreateVocabularyQuiz: (verseId: string) =>
    api.post<{ quizSlug: string; quizId: string; quizStatus: string }>(`${BASE}/verses/${verseId}/vocabulary-quiz`),

  // Batch verse review (sangraha-service/batch-verse-review.md)
  // Axios сериализует массивы как `id[]=...`, а бэкенд ждёт повторяющийся `id=...` —
  // query-строку собираем вручную.
  getVersesBatch: (ids: string[]) =>
    api.get<VerseBatchResponseDto>(`${BASE}/verse?${ids.map((id) => `id=${encodeURIComponent(id)}`).join('&')}`),

  analyzeVerses: (verseIds: string[]) =>
    api.post<{ verseIds: string[] }>(`${BASE}/verse/analysis`, { verseIds }),
};



