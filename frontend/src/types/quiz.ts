import type { LessonType } from './quizEnums';
import { Difficulty } from './quizEnums';

// Re-exports from split files (so consumers don't break)
export { LessonType, Difficulty, SessionStatus, isDeclensionsQuiz, isVocabularyQuiz, getQuizCategory } from './quizEnums';
export type { QuizSessionSummary, AnswerHistory, QuizProgress, PaginatedResponse } from './quizSession';

/* ═══════════════ core quiz interfaces ═══════════════ */
export interface QuizListItem {
    id: string;
    title: string;
    titleRu: string;
    titleEn: string;
    description: string;
    descriptionRu: string;
    descriptionEn: string;
    lessonType: LessonType;
    slug: string;
    totalQuestions: number;
    wordCount: number;
}

export interface QuizSummaryDto {
    id: string;
    slug: string;
    titleRu: string;
    titleEn: string;
    descriptionRu: string;
    descriptionEn: string;
    lessonType: LessonType;
    difficulty: Difficulty;
}

export interface StartSessionResponse {
    sessionId: string;
    quizId: string;
    lessonType: LessonType;
    questions: SessionQuestion[];
    totalQuestions: number;
    answeredQuestions: number;
    score: number;
}

export interface ResumeSessionResponse {
    sessionId: string;
    quizId: string;
    lessonType: LessonType;
    questions: SessionQuestion[];
    totalQuestions: number;
    answeredQuestions: number;
    score: number;
    currentQuestionIndex: number;
}

export interface StartOrResumeResponse {
    sessionId: string;
    quizId: string;
    lessonType: LessonType;
    questions: SessionQuestion[];
    totalQuestions: number;
    answeredQuestions: number;
    score: number;
    currentQuestionIndex: number;
    currentQuestionNumber: number;
    quizTitleRu: string;
    quizTitleEn: string;
    quizDescriptionRu: string;
    quizDescriptionEn: string;
    slug: string;
}

export interface SessionQuestion {
    id: string;
    questionNumber: number;
    text: string;
    options: QuestionOption[];
    stem: string;
    caseType: string;
    numberType: string;
    gender: string;
    stemDevanagari?: string;
    stemTranslationRu?: string;
    stemTranslationEn?: string;
}

export interface QuestionOption {
    id: string;
    formIast: string;
    formDevanagari: string;
}

export interface AnswerRequest {
    questionId: string;
    selectedOptionId: string;
    selectedFormIast?: string;
    responseTimeMs: number;
}

export interface AnswerResponse {
    isCorrect: boolean;
    correctOptionId: string;
    correctAnswerText: string;
    explanationRu: string;
    explanationEn: string;
    questionNumber: number;
    totalQuestions: number;
}

export interface LessonListResponse {
    lessons: LessonItemDto[];
}

export interface LessonItemDto extends QuizListItem {
        totalWordsOwn: number;
    learnedWords: number;
}




