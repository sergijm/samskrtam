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

export interface GrammarQuestionProgress {
  questionId:     string;
  textRu:         string;
  textEn:         string;
  correctAnswerRu?: string;
  correctAnswerEn?: string;
  nSuccess:       number;
  nAll:           number;
  score:          number;
  status:       WordStatus;
  caseType:       string;
  caseRu:         string;
  caseEn:         string;
  numberType:     string;
  numberRu:       string;
  numberEn:       string;
  gender:         string;
  genderRu:       string;
  genderEn:       string;
  caseEnding:     string | null;
}

export interface LessonStatusSummary {
  total:        number;
  /** JSON field name is "new" — reserved word in TS, mapped via backend */
  newCount:     number;
  learning:     number;
  mastered:     number;
  reviewDue:    number;
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
  questions:        GrammarQuestionProgress[];
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

