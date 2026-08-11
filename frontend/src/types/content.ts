export interface SandhiRuleDto {
    id: number;
    ruleNumber: number;
    ruleType: string;
    shortDescription?: string;
    whitneyNumber?: string;
    iastExample?: string;
    hkExample?: string;
    notes?: string;
    fullText: string;
}
