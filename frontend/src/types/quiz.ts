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

/**
 * Режим ответа вопроса — поле-диспетчер для рендера (task-frontend-01, frontend-state §5а).
 * Рендер выбирается по `answerMode`, не по `itemType`/`questionType`.
 */
export type AnswerMode = 'FREE_TEXT' | 'SINGLE_CHOICE' | 'MULTI_SELECT' | 'SPAN_SELECT' | 'MATCHING';

/**
 * Слово для подсветки внутри промпта вопроса (двуязычное: английский/русский
 * вариант). Фронт сплитит промпт по токену и делает совпадения жирными.
 */
export interface HighlightToken {
    text: string;
    textRu?: string;
}

/**
 * Расширение SessionQuestion под 4 типа declension-квестов (frontend-state §5а).
 * `answerMode` приходит от quiz-service как есть из QuestItem.answerMode.
 */
export interface SessionQuestion {
    id: string;
    questionNumber: number;
    text: string;
    textRu?: string;
    options: QuestionOption[];
    stem: string;
    caseType: string;
    numberType: string;
    gender: string;
    stemDevanagari?: string;
    stemTranslationRu?: string;
    stemTranslationEn?: string;
    questionType?: 'FORM_BY_CASE' | 'CASE_BY_FORM' | 'ENDING_MATCH' | 'MULTIPLE_CHOICE' | 'MATCHING' | 'FREE_TEXT';
    multiSelect?: boolean;
    formIast?: string;
    formDevanagari?: string;
    caseEnding?: string;
    /** MATCHING questions from the v2 compose flow: left-side rows (word forms). */
    matchRows?: QuestionMatchRow[];
    /** itemType (analytics/i18n header) — не используется для ветвления UI. */
    itemType?: string;
    /** Новое обязательное поле диспетчера. */
    answerMode?: AnswerMode;
    /** Непусто только при answerMode === 'MATCHING'. */
    matching?: MatchingPayload;
    /** Слова промпта для подсветки (жирным) — из curriculum payload. */
    highlights?: HighlightToken[];
}

export interface QuestionMatchRow {
    id: string;
    wordFormIast: string;
    wordFormDevanagari?: string;
    caseType?: string;
    numberType: string;
    person?: number;
    voice?: string;
}

/**
 * Payload для DECLENSION_MATCH: левый и правый списки уже разделены и
 * перемешаны раздельно на бэкенде — фронт не досортировывает.
 */
export interface MatchingPayload {
    left: MatchingItem[];
    right: MatchingItem[];
}

export interface MatchingItem {
    /** pairId — только для сборки ответа, пользователю не показывается. */
    id: string;
    text: string;
}

/**
 * Ответ на MATCHING — отправляется целиком одним вызовом submitAnswer.
 */
export interface MatchingAnswerPayload {
    sessionId: string;
    questionId: string;
    matches: Array<{ leftId: string; rightId: string }>;
}

export interface AnswerResult {
    isCorrect: boolean;
    /** Не используется при answerMode === 'MATCHING'. */
    correctOptionId: string;
    /** Только при MATCHING — по какой паре верен/неверен. */
    correctMatches?: Array<{ leftId: string; rightId: string }>;
    explanation: string;
    questionNumber: number;
    totalQuestions: number;
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
    totalQuestions: number;
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

export interface ComposeQuizRequest {
    topicCode: string;
    /** Optional progress-tag slice: e.g. NOMINATIVE, DUAL, GEN_ABL. */
    progressTagSetId?: string;
    itemType?: string;
    answerMode?: string;
    limit?: number;
    userLocale: string;
}

export interface ComposeQuizResponse {
    sessionId: string;
    totalQuestions: number;
    answeredQuestions: number;
    score: number;
    currentQuestionIndex: number;
    currentQuestionNumber: number;
    questions: SessionQuestion[];
}

export interface SessionQuestion {
    id: string;
    questionNumber: number;
    text: string;
    textRu?: string;
    options: QuestionOption[];
    stem: string;
    caseType: string;
    numberType: string;
    gender: string;
    stemDevanagari?: string;
    stemTranslationRu?: string;
    stemTranslationEn?: string;
    questionType?: 'FORM_BY_CASE' | 'CASE_BY_FORM' | 'ENDING_MATCH' | 'MULTIPLE_CHOICE' | 'MATCHING' | 'FREE_TEXT';
    multiSelect?: boolean;
    formIast?: string;
    formDevanagari?: string;
    caseEnding?: string;
    /** Sentence translation shown as context for CASE_MEANING (syntax) questions. */
    translation?: string;
    /** MATCHING questions from the v2 compose flow: left-side rows (word forms). */
    matchRows?: QuestionMatchRow[];
}

export interface QuestionMatchRow {
    id: string;
    wordFormIast: string;
    wordFormDevanagari?: string;
    caseType?: string;
    numberType: string;
    person?: number;
    voice?: string;
}

export interface QuestionOption {
    id: string;
    formIast: string;
    /** Russian variant of the option text (bilingual curriculum options); absent → render `formIast`. */
    textRu?: string;
    formDevanagari: string;
    optionType?: string;
    caseType?: string;
    caseRu?: string;
    caseEn?: string;
    numberType?: string;
    numberRu?: string;
    numberEn?: string;
    gender?: string;
    genderRu?: string;
    genderEn?: string;
    person?: number;
    voice?: string;
}

export interface MatchSubmission {
    rowId: string;
    optionId: string;
}

export interface AnswerRequest {
    questionId: string;
    selectedOptionId: string;
    selectedFormIast?: string;
    responseTimeMs: number;
    selectedOptionIds?: string[];
    matchSubmissions?: MatchSubmission[];
}

export interface AnswerResponse {
    isCorrect: boolean;
    correctOptionId: string;
    correctAnswerText: string;
    explanationRu: string;
    explanationEn: string;
    /** Только для MATCHING: по какой паре верен/неверен. */
    correctMatches?: Array<{ leftId: string; rightId: string }>;
    questionNumber: number;
    totalQuestions: number;
}

/**
 * Доменные данные ответа, которые QuestionRenderer передаёт в onSubmit.
 */
export type QuestionAnswerPayload =
    | string
    | string[]
    | { rowId: string; optionId: string }[]
    | MatchingAnswerPayload;

export interface LessonListResponse {
    lessons: LessonItemDto[];
}

export interface LessonItemDto extends QuizListItem {
    totalWordsOwn: number;
    learnedWords: number;
    domain?: string;
    domainType?: string;
}

/**
 * Реальная сводка прогресса по области (scope), вычисляется в quiz-service
 * по таблице quiz_item_score. Всегда идёт через quiz-service.
 */
export interface ProgressSummaryDto {
    scope: 'learn-graph' | 'grammar' | 'lexicon' | string;
    totalProgressTags: number;
    masteredProgressTags: number;
    learnedProgressTags: number;
    percent: number;
}




