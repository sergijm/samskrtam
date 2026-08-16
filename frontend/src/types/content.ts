export interface SandhiRuleDto {
    number: number;
    section: string;
    applicability: string;
    text: string;
    example?: string | null;
    reference?: string | null;
    supersedes?: number[];
    defaultFor?: number[];
    appliesWith?: number[];
    category?: string[];
}

export interface SandhiRulesResponse {
    topicCode?: string;
    title: string;
    rules: SandhiRuleDto[];
    categoryGlossary?: Record<string, string>;
}
