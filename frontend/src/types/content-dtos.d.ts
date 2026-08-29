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
export type Pos = 'NOUN' | 'VERB' | 'ADJECTIVE' | 'PRONOUN' | 'ADVERB' | 'PARTICLE' | 'INDECLINABLE' | 'NUMERAL' | 'CONJUNCTION' | 'INTERJECTION' | 'OTHER';
export type StemType = 'A_STEM' | 'AA_STEM' | 'I_STEM' | 'II_STEM' | 'U_STEM' | 'UU_STEM' | 'R_STEM' | 'IN_STEM' | 'AN_STEM' | 'AS_STEM' | 'IS_STEM' | 'US_STEM' | 'ANT_STEM' | 'VAT_STEM' | 'ROOT_STEM' | 'O_STEM' | 'AU_STEM' | 'PRON_TAD_MASC' | 'PRON_TAD_NEUT' | 'PRON_TAD_FEM' | 'PRON_IDAM_MASC' | 'PRON_IDAM_NEUT' | 'PRON_IDAM_FEM' | 'PRON_ADAS_MASC' | 'PRON_ADAS_NEUT' | 'PRON_ADAS_FEM' | 'PRON_ASMAD' | 'PRON_YUSMAD' | 'PRON_SARVA_MASC' | 'PRON_SARVA_NEUT' | 'PRON_SARVA_FEM' | 'PRON_PURVA_MASC' | 'PRON_PURVA_NEUT' | 'PRON_PURVA_FEM' | 'PRON_VAT_MASC' | 'PRON_VAT_FEM' | 'PRON_UBHA_MASC' | 'PRON_UBHA_FN' | 'PRON_AN' | 'PRON_KATI';

export interface CaseEndingDto {
    id: number;
    stemType: StemType;
    pos: Pos;
    gender: Gender;
    number: NumberType;
    grammaticalCase: CaseType;
    caseEnding: string;
}

export interface VerbalEndingDto {
    id: number;
    ending: string;
    lemmaSuffix: string;
    hasAugment: boolean;
    tenseMood: string;
    personNumber: string;
    pada: string;
    notes?: string;
}

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
// Conjugation Paradigms (presence-indicativus carousel)
// =============================================

export type Voice = 'PARASMAIPADA' | 'ATMANEPADA';

export interface ConjugationFormDto {
    person: number;
    numberType: NumberType;
    sentenceIast: string;
    sentenceDevanagari: string;
    translationRu: string;
}

export interface ConjugationParadigmDto {
    lemmaIast: string;
    lemmaDevanagari: string;
    meaningRu: string;
    voice: Voice;
    forms: ConjugationFormDto[];
}

export interface ConjugationParadigmPageDto {
    index: number;
    totalCount: number;
    paradigm: ConjugationParadigmDto;
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

