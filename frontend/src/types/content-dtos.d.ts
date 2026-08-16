export interface EamenauExerciseDto {
    id: number;
    exerciseNumber: number;
    exerciseLetter?: string;
    instructionText: string;
}

export interface EamenauTaskDto {
    id: number;
    taskNumber: number;
    taskText: string;
}

export interface EamenauExerciseDetailDto {
    id: number;
    exerciseNumber: number;
    exerciseLetter?: string;
    instructionText: string;
    tasks: EamenauTaskDto[];
}

export interface SandhiRuleDto {
    id: number;
    ruleNumber: number;
    ruleType: string;
    shortDescription: string;
    whitneyNumber: string;
    iastExample: string;
    hkExample: string;
    notes: string;
    fullText: string;
}

// =============================================
// Declension Paradigms (grammar-lesson-page §2.2)
// =============================================

export type Gender = 'MASCULINE' | 'FEMININE' | 'NEUTER' | 'UNKNOWN' | 'UNSPECIFIED';
export type VowelType = 'A_STEM' | 'AA_STEM' | 'I_STEM' | 'II_STEM' | 'U_STEM' | 'UU_STEM' | 'R_STEM';
export type CaseType = 'NOMINATIVE' | 'ACCUSATIVE' | 'INSTRUMENTAL' | 'DATIVE' | 'ABLATIVE' | 'GENITIVE' | 'LOCATIVE' | 'VOCATIVE';
export type NumberType = 'SINGULAR' | 'DUAL' | 'PLURAL';

export interface DeclensionFormDto {
    declensionStemId: string;
    caseType: CaseType;
    numberType: NumberType;
    formIast: string;
    formDevanagari: string;
}

export interface DeclensionParadigmDto {
    stemId: string;
    stemIast: string | null;
    stemDevanagari: string | null;
    translationRu: string | null;
    translationEn: string | null;
    gender: Gender;
    vowelType: VowelType;
    forms: DeclensionFormDto[];
}

export interface DeclensionParadigmPageDto {
    index: number;
    totalCount: number;
    paradigm: DeclensionParadigmDto;
}

// =============================================
// Declension Examples (grammar-lesson-page §2.2а)
// =============================================

export interface DeclensionExamplesResponseDto {
    groups: Array<{
        caseType: CaseType;
        numberType: NumberType;
        examples: Array<{
            verseId: string;
            workSlug: string;
            textIast: string;
            textDevanagari: string;
            translationRu: string;
            translationEn: string;
            workTitleRu: string;
            workTitleEn: string;
            chapterTitleRu: string;
            chapterTitleEn: string;
            verseOrderIndex: number;
        }>;
    }>;
    /** Только для роли ADMIN (см. content-service/declension-examples.md, шаг 4а).
     *  Для остальных ролей поле отсутствует в JSON — проверять через `?.length > 0`. */
    missingVerseIds?: string[];
}

// =============================================
// Sandhi Rules (curriculum-service sandhi-rules endpoint)
// =============================================

export interface SandhiRuleSummaryDto {
    number: number;
    section: string;
    applicability: string;
    text: string;
    example: string | null;
    reference: string;
    supersedes?: number[];
    defaultFor?: number[];
    appliesWith?: number[];
    category?: string[];
}

export interface SandhiRulesResponse {
    topicCode: string;
    title: string;
    rules: SandhiRuleSummaryDto[];
    categoryGlossary?: Record<string, string>;
}

