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

