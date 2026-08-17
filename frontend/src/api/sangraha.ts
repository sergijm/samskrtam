import api from './axios';
import type {
  WorkSummaryDto,
  WorkTreeDto,
  ChapterVersesDto,
  VerseDetailDto,
  VerseBatchResponseDto,
  WorksClassGroupDto,
  StandaloneVerseItemDto,
  VerseWordExamplesResponseDto,
} from '../types/sangraha';

const BASE = '/api/v1/sangraha';

export const sangrahaApi = {
// Works (read-only, отфильтрованные по классификатору)
  getAllWorks: (classIds?: string[]) =>
    api.get<WorkSummaryDto[]>(
      classIds && classIds.length > 0
        ? `${BASE}/works?${classIds.map((id) => `classId=${encodeURIComponent(id)}`).join('&')}`
        : `${BASE}/works`,
    ),

  // Классификатор произведений: дерево по classification (для дропдаунов)
  getWorksClasses: () => api.get<WorksClassGroupDto[]>(`${BASE}/works/classes`),

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

  // Кнопка «Изучить»: экспорт пачки лемм стиха в curriculum-service + код урока
  studyVerse: (verseId: string) =>
    api.post<{ verseTopicCode: string }>(`${BASE}/verses/${verseId}/study`),

  // Batch verse review (sangraha-service/batch-verse-review.md)
  getVersesBatch: (ids: string[]) =>
    api.post<VerseBatchResponseDto>(`${BASE}/verse`, { verseIds: ids }),

  // Примеры стихов по точным словоформам (урок склонений)
  getWordExamples: (surfaceIasts: string[]) =>
    api.post<VerseWordExamplesResponseDto>(`${BASE}/words/examples`, {
      surfaceIasts,
    }),

  analyzeVerses: (verseIds: string[]) =>
    api.post<{ verseIds: string[] }>(`${BASE}/verse/analysis`, { verseIds }),

  // ── Standalone анализ (страница /analysis, verse.chapter_id = null) ──
  // Каждое нажатие «Анализировать» создаёт новую запись в verses и запускает анализ.

  createStandaloneAnalysis: (text: string) =>
    api.post<VerseDetailDto>(`${BASE}/analysis`, { text }),

  getStandaloneVerses: () =>
    api.get<StandaloneVerseItemDto[]>(`${BASE}/analysis`),

  deleteStandaloneVerse: (verseId: string) =>
    api.delete<void>(`${BASE}/analysis/${verseId}`),
};



