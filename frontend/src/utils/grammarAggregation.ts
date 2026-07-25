import type { WordStatus, GrammarQuestionProgress } from '../types/lesson';

export const NUMBER_TYPES = ['SINGULAR', 'DUAL', 'PLURAL'] as const;

export const  CASE_TYPES = [
  'NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE',
  'ABLATIVE', 'GENITIVE', 'LOCATIVE', 'VOCATIVE',
] as const;

export const MASTERY_THRESHOLD = 90;

export interface CaseAggregation {
  caseType: string;
  caseRu: string;
  caseEn: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

export interface NumberAggregation {
  numberType: string;
  numberRu: string;
  numberEn: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

export const aggregateByCase = (questions: GrammarQuestionProgress[]): CaseAggregation[] => {
  const grouped = new Map<string, GrammarQuestionProgress[]>();
  for (const q of questions) {
    const ct = q.caseType;
    if (!grouped.has(ct)) grouped.set(ct, []);
    grouped.get(ct)!.push(q);
  }

  const result: CaseAggregation[] = [];
  for (const caseType of CASE_TYPES) {
    const items = grouped.get(caseType);
    if (!items || items.length === 0) continue;

    const total = items.length;
    const learned = items.filter(q => q.score >= MASTERY_THRESHOLD).length;
    const progress = total > 0 ? Math.round((learned / total) * 100) : 0;
    const firstItem = items[0];
    const status: WordStatus = progress >= MASTERY_THRESHOLD ? 'MASTERED' : 'LEARNING';

    result.push({
      caseType,
      caseRu: firstItem.caseRu,
      caseEn: firstItem.caseEn,
      aggregatedProgress: progress,
      totalCombinations: total,
      learnedCombinations: learned,
      status,
    });
  }
  return result;
};

export const aggregateByNumber = (questions: GrammarQuestionProgress[]): NumberAggregation[] => {
  const grouped = new Map<string, GrammarQuestionProgress[]>();
  for (const q of questions) {
    const nt = q.numberType;
    if (!grouped.has(nt)) grouped.set(nt, []);
    grouped.get(nt)!.push(q);
  }

  const result: NumberAggregation[] = [];
  for (const numberType of NUMBER_TYPES) {
    const items = grouped.get(numberType);
    if (!items || items.length === 0) continue;

    const total = items.length;
    const learned = items.filter(q => q.score >= MASTERY_THRESHOLD).length;
    const progress = total > 0 ? Math.round((learned / total) * 100) : 0;
    const firstItem = items[0];
    const status: WordStatus = progress >= MASTERY_THRESHOLD ? 'MASTERED' : 'LEARNING';

    result.push({
      numberType,
      numberRu: firstItem.numberRu,
      numberEn: firstItem.numberEn,
      aggregatedProgress: progress,
      totalCombinations: total,
      learnedCombinations: learned,
      status,
    });
  }
  return result;
};

