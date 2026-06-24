export type WordStatus = 'NOT_STARTED' | 'IN_PROGRESS' | 'LEARNED';

export interface VocabularyWordProgress {
  wordId:       string;
  word:         string;
  wordDevanagari: string | null;
  translationRu: string;
  translationEn: string;
  nSuccess:     number;
  nAll:         number;
  successRate:  number;   // 0-100
  status:       WordStatus;
}

export interface GrammarQuestionProgress {
  questionId:     string;
  textRu:         string;
  textEn:         string;
  correctAnswerRu: string;
  correctAnswerEn: string;
  nSuccess:       number;
  nAll:           number;
  successRate:    number;
  status:         WordStatus;
}

export interface VocabularyLesson {
  quizId:           string;
  slug:             string;
  titleRu:          string;
  titleEn:          string;
  difficulty:       string;
  totalWords:       number;
  learnedWords:     number;
  progressPercent:  number;
  words:            VocabularyWordProgress[];
}

export interface GrammarLesson {
  quizId:           string;
  type:             string;
  titleRu:          string;
  titleEn:          string;
  difficulty:       string;
  totalQuestions:   number;
  learnedQuestions: number;
  progressPercent:  number;
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