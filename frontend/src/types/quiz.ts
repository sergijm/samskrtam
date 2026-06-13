export interface QuizListItem {
    id: string;
    title: string;
    titleRu: string;
    titleEn: string;
    description: string;
    descriptionRu: string;
    descriptionEn: string;
    quizType: QuizType;
    slug: string;
    totalQuestions: number;
    wordCount: number; // New field for word count
}

export interface QuizSummaryDto {
    id: string;
    slug: string;
    titleRu: string;
    titleEn: string;
    descriptionRu: string;
    descriptionEn: string;
    quizType: QuizType;
    difficulty: string;
}

export interface StartSessionResponse {
    sessionId: string;
    quizId: string;
    quizType: QuizType;
    questions: SessionQuestion[];
    totalQuestions: number;
    answeredQuestions: number;
    score: number;
}

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

export interface StartOrResumeResponse {
    sessionId: string;
    quizId: string;
    quizType: QuizType;
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

export enum QuizType {
    VOCABULARY = 'VOCABULARY',
    DECLENSIONS = 'DECLENSIONS',
    CONJUGATIONS = 'CONJUGATIONS',
    A_STEM_DECLENSIONS = 'A_STEM_DECLENSIONS',
    AA_STEM_DECLENSIONS = 'AA_STEM_DECLENSIONS',
    I_STEM_DECLENSIONS = 'I_STEM_DECLENSIONS',
    II_STEM_DECLENSIONS = 'II_STEM_DECLENSIONS',
    U_STEM_DECLENSIONS = 'U_STEM_DECLENSIONS',
    UU_STEM_DECLENSIONS = 'UU_STEM_DECLENSIONS',
    R_STEM_DECLENSIONS = 'R_STEM_DECLENSIONS',
}

export interface QuizSessionSummary {
    sessionId: string;
    quizId: string;
    quizTitle: string;
    quizType: QuizType;
    slug: string;
    score: number;
    totalQuestions: number;
    status: SessionStatus;
    startedAt: string;
    completedAt?: string;
    durationMs?: number;
}

export enum SessionStatus {
    IN_PROGRESS = 'IN_PROGRESS',
    COMPLETED = 'COMPLETED',
    ABANDONED = 'ABANDONED',
}

export interface AnswerHistory {
    questionText: string;
    selectedAnswerIast: string;
    correctOptionIast: string;
    isCorrect: boolean;
    responseTimeMs: number;
    answeredAt: string;
    explanationRu: string;
    explanationEn: string;
}

export interface QuizProgress {
    sessionId: string;
    quizId: string;
    answeredQuestions: number;
    totalQuestions: number;
}
