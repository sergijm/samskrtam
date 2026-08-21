export type VerseStatus = 'DRAFT' | 'ANALYZING' | 'ANALYZED' | 'FAILED';

export interface WorkSummaryDto {
  id: string;
  slug: string;
  titleRu: string;
  titleEn: string;
  titleSaIast?: string | null;
  titleSaDevanagari?: string | null;
  descriptionRu?: string | null;
  descriptionEn?: string | null;
  author?: string | null;
  createdAt: string;
}

export interface ChapterSummaryDto {
  id: string;
  slug: string;
  titleRu: string;
  titleEn: string;
  titleIast?: string | null;
  titleDevanagari?: string | null;
  orderIndex: number;
  categoryCode: string;
  verseCount: number;
}

export interface WorkTreeDto {
  id: string;
  slug: string;
  titleRu: string;
  titleEn: string;
  descriptionRu?: string | null;
  descriptionEn?: string | null;
  author?: string | null;
  chapters: ChapterSummaryDto[];
}

export interface VerseTreeDto {
  id: string;
  orderIndex: number;
  textIastPreview?: string | null;
  textIast?: string | null;
  textDevanagari?: string | null;
  translationRu?: string | null;
  translationEn?: string | null;
  status: VerseStatus;
}

export interface ChapterVersesDto {
  id: string;
  slug: string;
  titleRu: string;
  titleEn: string;
  titleIast?: string | null;
  titleDevanagari?: string | null;
  orderIndex: number;
  categoryCode: string;
  verses: VerseTreeDto[];
}

export interface SandhiSplit {
  surface: string;
  components: string[];
  ruleNumbers?: number[];
}

export interface VerseWordMorphologyDto {
  caseType?: string | null;
  gender?: string | null;
  numberType?: string | null;
  person?: string | null;
  tense?: string | null;
  mood?: string | null;
  voice?: string | null;
}

export interface VerseWordDerivationDto {
  derivationType?: string | null;
  derivationalSuffix?: string | null;
  derivationalBase?: string | null;
  description?: string | null;
}

export interface VerseWordDto {
  id: string;
  position: number;
  surfaceIast: string;
  surfaceDevanagari: string;
  lemmaIast: string;
  stem?: string | null;
  root?: string | null;
  pos?: string | null;
  formType?: string | null;
  isFinite?: boolean | null;
  morphology?: VerseWordMorphologyDto | null;
  derivation?: VerseWordDerivationDto | null;
  lemmaGlossRu?: string | null;
  lemmaGlossEn?: string | null;
  contextGlossRu: string;
  contextGlossEn: string;
  formationRuleNumbers?: number[];
  analysisConfidence?: string | null;
  ambiguityNotes?: string | null;
  vocabularyWordId?: string | null;
}

export interface VerseAnalysisDto {
  verseId: string;
  translationRu: string;
  translationEn: string;
  sandhiSplits: SandhiSplit[];
  modelName: string;
  analyzedAt: string;
}

export interface VerseDetailDto {
  id: string;
  chapterId?: string | null;
  orderIndex: number;
  textDevanagari?: string | null;
  textIast?: string | null;
  rawText?: string | null;
  status: VerseStatus;
  analysis?: VerseAnalysisDto | null;
  words: VerseWordDto[];
  verseTopicCode?: string | null;
}

// ── Standalone анализ (страница /analysis, verse.chapter_id = null) ──

export interface StandaloneVerseItemDto {
  id: string;
  preview?: string | null;
  status: VerseStatus;
  createdAt: string;
}

// ── Batch verse review (sangraha-service/batch-verse-review.md) ──

export interface VerseBatchItemDto {
  id: string;
  workSlug: string;
  workTitleRu: string;
  workTitleEn: string;
  chapterSlug: string;
  chapterTitleRu: string;
  chapterTitleEn: string;
  verseOrderIndex: number;
  textIastPreview?: string | null;
  status: VerseStatus;
}

export interface VerseBatchResponseDto {
  verses: VerseBatchItemDto[];
}

// ── Классификатор произведений (works_class) ──

export interface WorksClassTreeNodeDto {
  id: string;
  parentId?: string | null;
  code: string;
  titleRu: string;
  titleEn: string;
  titleSaIast: string;
  titleSaDeva?: string | null;
  sortOrder: number;
  workCount: number;
  children: WorksClassTreeNodeDto[];
}

export interface WorksClassGroupDto {
  classification: string;
  classes: WorksClassTreeNodeDto[];
}

// ── Поиск примеров стихов по точной словоформе (урок склонений) ──

export interface VerseWordExamplesResponseDto {
  results: VerseWordExamplesResultDto[];
}

export interface VerseWordExamplesResultDto {
  surfaceIast: string;
  verses: VerseWordExampleItemDto[];
}

export interface VerseWordExampleItemDto {
  verseId: string;
  workSlug: string;
  textIast: string;
  textDevanagari: string;
  translationRu: string | null;
  translationEn: string | null;
  workTitleRu: string;
  workTitleEn: string;
  chapterTitleRu: string;
  chapterTitleEn: string;
  verseOrderIndex: number;
}

// ── Примеры склонений по словоизменительному классу (вкладка «Примеры» урока) ──
// POST /api/v1/sangraha/verses/examples/declensions — один запрос на урок, на вход
// (vowelType, limitPerGroup); caseType/numberType опциональны
// (фильтр по падежу/числу), фронтендом не передаются.

export interface DeclensionExamplesResponseDto {
  groups: Array<{
    caseType: string;
    numberType: string;
    examples: VerseWordExampleItemDto[];
  }>;
}

// ── Примеры спряжений (вкладка «Примеры» урока спряжений) ──
// POST /api/v1/sangraha/verses/examples/conjugations — один запрос на урок,
// на вход (tense, mood, limitPerGroup); tense/mood опциональны.

export interface ConjugationExamplesResponseDto {
  groups: Array<{
    tense: string;
    mood: string;
    examples: VerseWordExampleItemDto[];
  }>;
}

