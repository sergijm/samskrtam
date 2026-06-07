export type QuizType = 'DECLENSIONS' | 'CONJUGATIONS' | 'VOCABULARY' | 'A_STEM_DECLENSIONS' | 'AA_STEM_DECLENSIONS' | 'I_STEM_DECLENSIONS' | 'II_STEM_DECLENSIONS' | 'U_STEM_DECLENSIONS' | 'UU_STEM_DECLENSIONS' | 'R_STEM_DECLENSIONS';
export type Difficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
export type SessionStatus = 'IN_PROGRESS' | 'COMPLETED' | 'ABANDONED'; // Add SessionStatus

export interface QuizListItem {
  id: string;
  title: string; // Localized title
  description: string;
  quizType: QuizType;
  slug: string;
  totalQuestions: number;
  difficulty: Difficulty;
  bestScore?: number;
}

export interface QuizSummaryDto {
  id: string;
  title: string; // Localized title
  quizType: QuizType;
  difficulty: Difficulty;
  bestScore?: number;
}

export interface SessionOption {
  id: string;
  text: string;
}

export interface SessionQuestion {
  id: string;
  text: string;
  options: SessionOption[];
}

export interface StartSessionResponse {
  sessionId: string;
  quizId: string;
  quizType: QuizType;
  questions: SessionQuestion[];
  totalQuestions: number;
}

export interface AnswerRequest {
  questionId: string;
  selectedOptionId: string;
  responseTimeMs: number;
}

export interface AnswerResponse {
  isCorrect: boolean;
  correctOptionId: string;
  explanation: string; // Localized explanation
  questionNumber: number;
  totalQuestions: number;
}

// New interfaces for user quiz sessions
export interface QuizSessionSummary {
  sessionId: string;
  quizId: string;
  quizTitle: string;
  quizType: QuizType;
  score: number;
  totalQuestions: number;
  status: SessionStatus;
  startedAt: string; // ISO string
  completedAt?: string; // ISO string, optional
  durationMs?: number; // optional
}

export interface AnswerHistory {
  questionId: string;
  questionText: string;
  selectedAnswerIast: string;
  correctOptionIast: string;
  isCorrect: boolean;
  responseTimeMs: number;
  answeredAt: string; // ISO string
  explanation: string; // Localized explanation
}

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  currentPage: number;
  pageSize: number;
  isFirst: boolean;
  isLast: boolean;
}
