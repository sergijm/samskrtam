export interface SandhiRuleInfo {
    ruleNumber: number;
    shortDescription: string;
}

export interface SolutionDto {
    id: number;
    solutionText: string;
    stepByStep?: string;
    sandhiRules: SandhiRuleInfo[];
}

export interface SolutionUpdateRequestDto {
    stepByStep: string;
    ruleNumbers: string;
}

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
    solution?: SolutionDto;
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

// Monier-Williams Dictionary Types
export interface MwDictionaryEntryDto {
    recordId: string;
    key1: string;
    key1Display: string;
    key2: string;
    homonymNum: string;
    eCode: string;
    page: number;
    columnNum: number;
    isSupplement: boolean;
    mainTranslation: string;
    lexicalInfo: any[]; // Define more specific types if needed
    sanskritWords: any[];
    homonyms: any[];
    abbreviations: any[];
    literarySources: any[];
    infoTags: any[];
    rawBody: string;
    displayTitle: string;
}

export interface MwEntryDto {
    entries: MwDictionaryEntryDto[];
}

// Frisch Dictionary Types (exact IAST lemma lookup)
export interface FrischSenseDto {
    genders?: any;
    number_note?: string;
    is_proper_noun?: boolean;
    cs?: string;
    ru?: string;
    en?: string;
}

export interface FrischEntryDto {
    pos?: any[];
    senses?: FrischSenseDto[];
    genders?: any[];
    is_root?: boolean;
    entry_id?: number;
    gloss_cs?: string;
    gloss_en?: string;
    gloss_ru?: string;
    lemma_iast?: string;
    verb_class?: number;
    verb_forms?: any[];
    grammar_note?: string;
    parent_lemma?: string;
    raw_headline?: string;
    derived_stems?: any[];
    homonym_index?: number;
    related_forms?: any[];
    is_related_form?: boolean;
    parent_entry_id?: number;
    cross_references?: any[];
}

// Apte Dictionary Types (lookup by SLP1 key derived from IAST lemma)
export interface ApteEntryDto {
    id?: number;
    headwordDevanagari?: string;
    bodyText?: string;
    rawMarkup?: string;
    homonymNum?: number;
}
