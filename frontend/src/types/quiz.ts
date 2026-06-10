import { PaginatedResponse } from './common';

// Changed from type to enum for better import compatibility
export enum QuizType {
  DECLENSIONS = 'DECLENSIONS',
  CONJUGATIONS = 'CONJUGATIONS',
  VOCABULARY = 'VOCABULARY',
  A_STEM_DECLENSIONS = 'A_STEM_DECLENSIONS',
  AA_STEM_DECLENSIONS = 'AA_STEM_DECLENSIONS',
  I_STEM_DECLENSIONS = 'I_STEM_DECLENSIONS',
  II_STEM_DECLENSIONS = 'II_STEM_DECLENSIONS',
  U_STEM_DECLENSIONS = 'U_STEM_DECLENSIONS',
  UU_STEM_DECLENSIONS = 'UU_STEM_DECLENSIONS',
  R_STEM_DECLENSIONS = 'R_STEM_DECLENSIONS',
}
export type Difficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';
// Changed from type to enum for better import compatibility
export enum SessionStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  ABANDONED = 'ABANDONED',
}

export interface QuizListItem {
  id: string;
  title: string; // Keep existing title for backward compatibility or default
  titleRu: string; // New field for Russian title
  titleEn: string; // New field for English title
  description: string; // Keep existing description for backward compatibility or default
  descriptionRu: string; // New field for Russian description
  descriptionEn: string; // New field for English description
  quizType: QuizType;
  slug: string;
  totalQuestions: number;
  difficulty: Difficulty;
  bestScore?: number;
}

export interface QuizSummaryDto {
  id: string;
  title: string; // Keep existing title for backward compatibility or default
  titleRu: string; // New field for Russian title
  titleEn: string; // New field for English title
  descriptionRu: string; // New field for Russian description
  descriptionEn: string; // New field for English description
  quizType: QuizType;
  difficulty: Difficulty;
  bestScore?: number;
}

export interface SessionOption {
  id: string;
  formIast: string; // Changed from text to formIast
  formDevanagari: string; // New field
}

export interface SessionQuestion {
  id: string;
  questionNumber: number; // New field for the order of the question
  text: string;
  options: SessionOption[];
  // New fields for structured question data
  stem?: string;
  caseType?: string;
  numberType?: string;
}

export interface StartSessionResponse {
  sessionId: string;
  quizId: string;
  quizType: QuizType;
  questions: SessionQuestion[];
  totalQuestions: number;
}

// New interface for resuming a session
export interface ResumeSessionResponse {
  sessionId: string;
  quizId: string;
  quizType: QuizType;
  questions: SessionQuestion[];
  totalQuestions: number;
  answeredQuestions: number;
  score: number;
  currentQuestionIndex: number;
}

export interface AnswerRequest {
  questionId: string;
  selectedOptionId: string;
  selectedFormIast?: string; // New optional field
  responseTimeMs: number;
}

export interface AnswerResponse {
  isCorrect: boolean;
  correctOptionId: string;
  explanationRu: string; // Russian explanation
  explanationEn: string; // English explanation
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
  selectedAnswerIast?: string; // Made optional
  correctOptionIast?: string; // Made optional
  isCorrect?: boolean; // Made optional
  responseTimeMs?: number; // Made optional
  answeredAt?: string; // Made optional
  explanationRu?: string; // New field for Russian explanation
  explanationEn?: string; // New field for English explanation
}

// New interface for quiz progress
export interface QuizProgress {
  sessionId?: string; // New field to store the session ID, optional as it might not be found
  answeredQuestions: number;
  totalQuestions: number;
  found: boolean;
}
