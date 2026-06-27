// Все типы как enum (доступны и во время компиляции, и во время выполнения)
export enum LessonType {
    DECLENSIONS = 'DECLENSIONS',
    A_STEM_DECLENSIONS = 'A_STEM_DECLENSIONS',
    AA_STEM_DECLENSIONS = 'AA_STEM_DECLENSIONS',
    I_STEM_DECLENSIONS = 'I_STEM_DECLENSIONS',
    II_STEM_DECLENSIONS = 'II_STEM_DECLENSIONS',
    U_STEM_DECLENSIONS = 'U_STEM_DECLENSIONS',
    UU_STEM_DECLENSIONS = 'UU_STEM_DECLENSIONS',
    R_STEM_DECLENSIONS = 'R_STEM_DECLENSIONS',
    CONJUGATIONS = 'CONJUGATIONS',
    VOCABULARY = 'VOCABULARY',
    VOCABULARY_BASIC = 'VOCABULARY_BASIC',
    VOCABULARY_TEXTS = 'VOCABULARY_TEXTS',
}

export enum Difficulty {
    BEGINNER = 'BEGINNER',
    INTERMEDIATE = 'INTERMEDIATE',
    ADVANCED = 'ADVANCED',
}

export enum SessionStatus {
    IN_PROGRESS = 'IN_PROGRESS',
    COMPLETED = 'COMPLETED',
    ABANDONED = 'ABANDONED',
}


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
    wordCount: number; // New field for word count
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

export interface QuizSummary {
  id:         string;
  titleRu:    string;
  titleEn:    string;
  lessonType:   LessonType;
  difficulty: Difficulty;
  bestScore?: number;
}

export interface QuizSession {
  sessionId:  string;
  quizId:     string;
  questions:  SessionQuestion[];
}


export interface AnswerResult {
  isCorrect:       boolean;
  correctOptionId: string;
  explanation:     string;
  questionNumber:  number;
  totalQuestions:  number;
}
// Helper functions to check quiz types (similar to Java enum methods)
export const isDeclensionsQuiz = (lessonType: LessonType): boolean => {
  return [
    'DECLENSIONS',
    'A_STEM_DECLENSIONS',
    'AA_STEM_DECLENSIONS',
    'I_STEM_DECLENSIONS',
    'II_STEM_DECLENSIONS',
    'U_STEM_DECLENSIONS',
    'UU_STEM_DECLENSIONS',
    'R_STEM_DECLENSIONS'
  ].includes(lessonType);
};

export interface LessonListResponse {
    lessons: LessonItemDto[];
}

export interface LessonItemDto extends QuizListItem {
    totalWordsOwn: number;
    learnedWords: number;
}

export const isVocabularyQuiz = (lessonType: LessonType): boolean => {
  return [
    'VOCABULARY',
    'VOCABULARY_BASIC',
    'VOCABULARY_TEXTS'
  ].includes(lessonType);
};

// Alternative approach with more specific type checking
export const getQuizCategory = (lessonType: LessonType): 'declensions' | 'conjugations' | 'vocabulary' | 'other' => {
  if (isDeclensionsQuiz(lessonType)) {
    return 'declensions';
  }
  if (isVocabularyQuiz(lessonType)) {
    return 'vocabulary';
  }
  if (lessonType === 'CONJUGATIONS') {
    return 'conjugations';
  }
  return 'other';
};

