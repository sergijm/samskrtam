import api from './axios';
import type {
  WorkSummaryDto,
  WorkTreeDto,
  ChapterVersesDto,
  VerseDetailDto,
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

    getOrCreateVocabularyQuiz: (verseId: string) =>
    api.post<{ quizSlug: string; quizId: string; quizStatus: string }>(`${BASE}/verses/${verseId}/vocabulary-quiz`),
};

