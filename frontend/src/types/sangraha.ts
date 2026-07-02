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
  status: VerseStatus;
}

export interface ChapterTreeDto {
  id: string;
  slug: string;
  titleRu: string;
  titleEn: string;
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
}

export interface VerseWordDto {
  id: string;
  position: number;
  surfaceIast: string;
  surfaceDevanagari: string;
  lemmaIast: string;
  stem: string;
  root?: string | null;
  pos?: string | null;
  gender?: string | null;
  caseType?: string | null;
  numberType?: string | null;
  person?: string | null;
  tense?: string | null;
  mood?: string | null;
  voice?: string | null;
  glossRu: string;
  glossEn: string;
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
  status: VerseStatus;
  analysis?: VerseAnalysisDto | null;
  words: VerseWordDto[];
}

export interface CreateWorkRequest {
  slug: string;
  titleRu: string;
  titleEn: string;
  descriptionRu?: string;
  descriptionEn?: string;
  author?: string;
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
  orderIndex: number;
  titleRu: string;
  titleEn: string;
}

export interface UpdateChapterRequest {
  slug?: string;
  orderIndex?: number;
  titleRu?: string;
  titleEn?: string;
}

export interface CreateVerseRequest {
  orderIndex: number;
  textDevanagari?: string;
  textIast?: string;
}

export interface UpdateVerseTextRequest {
  textDevanagari?: string;
  textIast?: string;
}