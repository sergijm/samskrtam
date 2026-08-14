export interface SandhiRuleDto {
    number: number;
    section: string;
    applicability: string;
    text: string;
    example?: string | null;
    reference?: string | null;
    dependsOn?: number[];
    dependsOnNote?: string | null;
}
