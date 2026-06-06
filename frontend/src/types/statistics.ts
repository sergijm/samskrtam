import { QuizType } from './quiz'; // Assuming QuizType is defined here or in a shared types file

export interface UserQuizStatisticDto {
  quizId: string;
  quizType: QuizType;
  totalSessions: number;
  totalQuestionsAnswered: number;
  totalCorrectAnswers: number;
  totalScore: number;
  averageScore: number;
  lastCompletedAt: string; // ISO string date
}
