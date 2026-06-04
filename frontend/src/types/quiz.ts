export type QuizType = 'DECLENSIONS' | 'CONJUGATIONS' | 'VOCABULARY';
export type Difficulty = 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED';

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
