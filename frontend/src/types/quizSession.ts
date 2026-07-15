import type { LessonType, SessionStatus } from './quizEnums';

/* ---------- session list / summary ---------- */

export interface QuizSessionSummary {
  sessionId: string;
  quizId: string;
  slug: string;
  quizTitle: string;
  lessonType: LessonType;
  score: number;
  totalQuestions: number;
  status: SessionStatus;
  startedAt: string;
  completedAt?: string;
  durationMs?: number;
}

/* ---------- answer history ---------- */

export interface AnswerHistory {
  questionId: string;
  questionNumber: number;
  questionText: string;
  selectedAnswerIast?: string;
  correctOptionIast?: string;
  isCorrect?: boolean;
  explanationRu?: string;
  explanationEn?: string;
  responseTimeMs?: number;
  answeredAt?: string;
}

/* ---------- progress ---------- */

export interface QuizProgress {
  sessionId: string;
  answeredQuestions: number;
  totalQuestions: number;
  found: boolean;
}

/* ---------- generic pagination ---------- */

export interface PaginatedResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
}
