export type VerseStatus = 'DRAFT' | 'ANALYZING' | 'ANALYZED' | 'FAILED';

export interface WorkSummaryDto {
  id: string;
  slug: string;
  titleRu: string;
  titleEn: string;
  descriptionRu?: string | null;
  descriptionEn?: string | null;
  author?: string | null;
  createdAt: string;
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

export interface ChapterTreeDto {
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

export interface WorkTreeDto {
  id: string;
  slug: string;
  titleRu: string;
  titleEn: string;
  descriptionRu?: string | null;
  descriptionEn?: string | null;
  author?: string | null;
  chapters: ChapterTreeDto[];
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
  chapterId: string;
  orderIndex: number;
  textDevanagari?: string | null;
  textIast?: string | null;
  rawText?: string | null;
  status: VerseStatus;
  analysis?: VerseAnalysisDto | null;
  words: VerseWordDto[];
    vocabularyQuizSlug?: string | null;
}

export interface CreateWorkRequest {
  title: string;
  description?: string;
}

export interface UpdateWorkRequest {
  titleRu?: string;
  titleEn?: string;
  descriptionRu?: string;
  descriptionEn?: string;
  author?: string;
}

export interface CreateChapterRequest {
  slug: string;
  orderIndex?: number;
  title: string;
}

export interface UpdateChapterRequest {
  slug?: string;
  orderIndex?: number;
  title?: string;
}

export interface CreateVerseRequest {
  orderIndex: number;
  textDevanagari?: string;
  textIast?: string;
}

export interface UpdateVerseTextRequest {
  text: string;
}

export interface UpdateVerseRequest {
  orderIndex: number;
  rawText?: string;
}