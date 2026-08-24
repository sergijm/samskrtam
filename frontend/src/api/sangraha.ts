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
  LemmaExamplesResponseDto,
  DeclensionExamplesResponseDto,
  ConjugationExamplesResponseDto,
  SourceDto,
} from '../types/sangraha';

const BASE = '/api/v1/sangraha';

export const sangrahaApi = {
// Works (read-only, отфильтрованные по классификатору и источнику)
  getAllWorks: (classIds?: string[], sourceCode?: string) => {
    const params: string[] = [];
    if (classIds && classIds.length > 0) {
      classIds.forEach((id) => params.push(`classId=${encodeURIComponent(id)}`));
    }
    if (sourceCode) {
      params.push(`sourceCode=${encodeURIComponent(sourceCode)}`);
    }
    const qs = params.length > 0 ? `?${params.join('&')}` : '';
    return api.get<WorkSummaryDto[]>(`${BASE}/works${qs}`);
  },

  // Источники произведений (для фильтра слева)
  getSources: () => api.get<SourceDto[]>(`${BASE}/sources`),

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

  // Примеры стихов по леммам (раскрываемые строки урока лексики)
  getLemmaExamples: (lemmas: string[], limitPerLemma = 5) =>
    api.post<LemmaExamplesResponseDto>(`${BASE}/words/examples-by-lemma`, {
      lemmas,
      limitPerLemma,
    }),

  // Примеры склонений по словоизменительному классу — вкладка «Примеры» урока
  // (один запрос на весь урок; caseType/numberType в теле не передаются)
  getDeclensionExamples: (vowelType: string, limitPerGroup = 5) =>
    api.post<DeclensionExamplesResponseDto>(`${BASE}/verses/examples/declensions`, {
      vowelType,
      limitPerGroup,
    }),

  // Примеры спряжений — вкладка «Примеры» урока спряжений
  getConjugationExamples: (limitPerGroup = 5, tense?: string, mood?: string) =>
    api.post<ConjugationExamplesResponseDto>(`${BASE}/verses/examples/conjugations`, {
      tense,
      mood,
      limitPerGroup,
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



