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
