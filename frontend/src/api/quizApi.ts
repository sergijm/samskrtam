import api from './axios';
import { QuizListItem } from '../types/quiz'; // Assuming this type exists
import { StartSessionResponse } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/StartSessionResponse';
import { AnswerRequest } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/AnswerRequest';
import { AnswerResponse } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/AnswerResponse';
import { QuizSummaryDto } from '../../shared/quiz-content-dtos/src/main/java/sm/selflearn/samskrtam/content/dto/QuizSummaryDto'; // Import QuizSummaryDto

export const quizApi = {
  getQuizList: (category?: string) => {
    let url = '/api/v1/content/quizzes';
    if (category) {
      url += `?category=${category}`;
    }
    return api.get<QuizListItem[]>(url);
  },

  getQuizBySlug: (slug: string) => api.get<QuizSummaryDto>(`/api/v1/content/quizzes/by-slug/${slug}`),

  startSession: (quizId: string, quizType: string, userLocale: string) => {
    let url = '';
    if (quizType === 'VOCABULARY') {
      url = `/api/v1/quiz/vocabulary/${quizId}/sessions/start`; // quizId is slug for vocabulary
    } else {
      url = `/api/v1/quiz/declensions/sessions/start`; // quizId is actual quizId for grammar
    }
    return api.post<StartSessionResponse>(url, null, {
      params: { quizId },
      headers: { 'X-User-Locale': userLocale },
    });
  },

  submitAnswer: (sessionId: string, quizType: string, answer: AnswerRequest, userLocale: string) => {
    let url = '';
    if (quizType === 'VOCABULARY') {
      // Assuming slug is not needed in submitAnswer for vocabulary, or needs to be passed
      url = `/api/v1/quiz/vocabulary/any-slug/sessions/${sessionId}/answer`; // Placeholder for slug
    } else {
      url = `/api/v1/quiz/declensions/sessions/${sessionId}/answer`;
    }
    return api.post<AnswerResponse>(url, answer, {
      headers: { 'X-User-Locale': userLocale },
    });
  },
};
