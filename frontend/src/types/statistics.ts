import { QuizType, AnswerHistory } from './quiz'; // Assuming QuizType is defined here or in a shared types file
import { PaginatedResponse } from './common'; // Import PaginatedResponse

export interface UserQuizStatisticDto {
  quizId: string;
  quizType: QuizType;
  totalSessions: number;
  totalQuestionsAnswered: number;
  totalCorrectAnswers: number;
  totalScore: number;
  averageScore: number;
  lastCompletedAt: string; // ISO string date
  answerHistoryJson?: string; // New field for storing answer history as JSON string
}
