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
export interface MwWordSearchDto {
    id: number;
    slp1Spelling: string;
    slp1Normalized: string;
    iastSpelling?: string;
    key1: string;
}

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
