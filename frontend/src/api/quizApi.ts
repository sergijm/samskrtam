import api from './axios';
import { QuizListItem } from '../types/quiz'; // Assuming this type exists

export const quizApi = {
  getQuizList: () => api.get<QuizListItem[]>('/api/v1/content/quizzes'), // Changed to content-service endpoint
};
