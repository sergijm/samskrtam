import { LessonType, AnswerHistory } from './quiz'; // Assuming LessonType is defined here or in a shared types file
import { PaginatedResponse } from './common'; // Import PaginatedResponse

export interface UserQuizStatisticDto {
  quizId: string;
  lessonType: LessonType;
  totalSessions: number;
  totalQuestionsAnswered: number;
  totalCorrectAnswers: number;
  totalScore: number;
  averageScore: number;
  lastCompletedAt: string; // ISO string date
  answerHistoryJson?: string; // New field for storing answer history as JSON string
}
