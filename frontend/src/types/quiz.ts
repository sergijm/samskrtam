export type QuizType = 'DECLENSIONS' | 'CONJUGATIONS' | 'VOCABULARY';

export interface QuizListItem {
  id: string;
  title: string;
  description: string;
  quizType: QuizType;
  slug: string;
  totalQuestions: number;
}
