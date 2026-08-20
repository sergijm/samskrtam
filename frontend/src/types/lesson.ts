export type WordStatus = 'NEW' | 'LEARNING' | 'REVIEW' | 'MASTERED';

export interface VocabularyWordProgress {
  wordId:       string;
  word:         string;
  wordDevanagari: string | null;
  translationRu: string;
  translationEn: string;
  nSuccess:     number;
  nAll:         number;
  score:        number;   // 0-100, exponential score
  status:       WordStatus;
}

export interface LessonStatusSummary {
  total:        number;
  /** JSON field name is "new" — reserved word in TS, mapped via backend */
  newCount:     number;
  learning:     number;
  mastered:     number;
  reviewDue:    number;
}

export interface CaseAggregation {
  caseType: string;
  caseRu: string;
  caseEn: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

export interface NumberAggregation {
  numberType: string;
  numberRu: string;
  numberEn: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

export interface CaseNumberAggregation {
  caseType: string;
  numberType: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

export interface PairAggregation {
  setId: string;
  caseTypeA: string;
  caseTypeB: string;
  caseRuA: string;
  caseRuB: string;
  caseEnA: string;
  caseEnB: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

export interface VocabularyLesson {
  lessonId:         string;
  slug:             string;
  titleRu:          string;
  titleEn:          string;
  difficulty:       string;
  totalWords:       number;
  learnedWords:     number;
  progressPercent:  number;
    statusSummary?:   LessonStatusSummary;
  words:            VocabularyWordProgress[];
}

export interface GrammarLesson {
  lessonId:         string;
  type:             string;
  titleRu:          string;
  titleEn:          string;
  difficulty:       string;
  totalQuestions:   number;
  learnedQuestions: number;
  progressPercent:  number;
  statusSummary?:   LessonStatusSummary;
  caseAggregations?: CaseAggregation[];
  numberAggregations?: NumberAggregation[];
  grid?:            CaseNumberAggregation[];
  pairAggregations?: PairAggregation[];
  conjugationProgress?: ConjugationCellProgress[];
}

export interface ConjugationCellProgress {
  voice: string;
  person: number;
  numberType: string;
  score: number;
  status: WordStatus;
}

export interface AnswerHistoryEntry {
  answeredAt:      string;   // ISO datetime
  correctAnswer:   string;
  userAnswer:      string;
  isCorrect:       boolean;
}

export interface WordAnswerHistory {
  wordId:   string;
  word:     string;
  quizId:   string;
  entries:  AnswerHistoryEntry[];
  page:     number;
  total:    number;
}

export interface QuestionAnswerHistory {
  questionId:   string;
  textRu:       string;
  quizId:       string;
  entries:      AnswerHistoryEntry[];
  page:         number;
  total:        number;
}

