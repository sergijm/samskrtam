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

export interface CaseNumberAggregation {
  caseType: string;
  numberType: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

function avgProgress(items: GrammarQuestionProgress[]): number {
  if (items.length === 0) return 0;
  const sum = items.reduce((acc, q) => acc + (q.score || 0), 0);
  return Math.round(sum / items.length);
}

function progressStatus(avg: number): WordStatus {
  if (avg <= 0) return 'NEW';
  if (avg < MASTERY_THRESHOLD) return 'LEARNING';
  return 'MASTERED';
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

    const progress = avgProgress(items);
    const learned = items.filter(q => q.score >= MASTERY_THRESHOLD).length;
    const firstItem = items[0];

    result.push({
      caseType,
      caseRu: firstItem.caseRu,
      caseEn: firstItem.caseEn,
      aggregatedProgress: progress,
      totalCombinations: items.length,
      learnedCombinations: learned,
      status: progressStatus(progress),
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

    const progress = avgProgress(items);
    const learned = items.filter(q => q.score >= MASTERY_THRESHOLD).length;
    const firstItem = items[0];

    result.push({
      numberType,
      numberRu: firstItem.numberRu,
      numberEn: firstItem.numberEn,
      aggregatedProgress: progress,
      totalCombinations: items.length,
      learnedCombinations: learned,
      status: progressStatus(progress),
    });
  }
  return result;
};

export const aggregateByCaseAndNumber = (questions: GrammarQuestionProgress[]): CaseNumberAggregation[] => {
  const grouped = new Map<string, GrammarQuestionProgress[]>();
  for (const q of questions) {
    const key = `${q.caseType}:${q.numberType}`;
    if (!grouped.has(key)) grouped.set(key, []);
    grouped.get(key)!.push(q);
  }

  const result: CaseNumberAggregation[] = [];
  for (const caseType of CASE_TYPES) {
    for (const numberType of NUMBER_TYPES) {
      const items = grouped.get(`${caseType}:${numberType}`);
      if (!items || items.length === 0) continue;

      const progress = avgProgress(items);
      const learned = items.filter(q => q.score >= MASTERY_THRESHOLD).length;

      result.push({
        caseType,
        numberType,
        aggregatedProgress: progress,
        totalCombinations: items.length,
        learnedCombinations: learned,
        status: progressStatus(progress),
      });
    }
  }
  return result;
};

/**
 * Маппинг numberType → progressTagSetId (см. quest-engine.md §2.4).
 */
export const numberToProgressTagSetId = (numberType: string): string =>
  ({ SINGULAR: 'SINGULAR', DUAL: 'DUAL', PLURAL: 'PLURAL' } as Record<string, string>)[numberType] ?? '';

/**
 * Маппинг caseType → progressTagSetId пары омонимичных падежей
 * (ACC_LOC/INS_ABL/GEN_LOC/DAT_ACC, см. quest-engine.md §2.4).
 * Падежи вне пар (NOMINATIVE/VOCATIVE) возвращают '' — квиз без среза.
 */
export const caseToProgressTagSetId = (caseType: string): string => {
  switch (caseType) {
    case 'ACCUSATIVE':
    case 'LOCATIVE':
      return 'ACC_LOC';
    case 'INSTRUMENTAL':
    case 'ABLATIVE':
      return 'INS_ABL';
    case 'GENITIVE':
      return 'GEN_LOC';
    case 'DATIVE':
      return 'DAT_ACC';
    default:
      return '';
  }
};