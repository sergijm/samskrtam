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

/** Semantic case pairs served as progress-tag slices (mirror ProgressTagSetId). */
export interface CasePair {
  setId: string;
  caseTypeA: string;
  caseTypeB: string;
}

export const CASE_PAIRS: CasePair[] = [
  { setId: 'GEN_LOC', caseTypeA: 'GENITIVE', caseTypeB: 'LOCATIVE' },
  { setId: 'GEN_ABL', caseTypeA: 'GENITIVE', caseTypeB: 'ABLATIVE' },
  { setId: 'DAT_ACC', caseTypeA: 'DATIVE', caseTypeB: 'ACCUSATIVE' },
  { setId: 'INS_ABL', caseTypeA: 'INSTRUMENTAL', caseTypeB: 'ABLATIVE' },
  { setId: 'INS_LOC', caseTypeA: 'INSTRUMENTAL', caseTypeB: 'LOCATIVE' },
  { setId: 'ACC_LOC', caseTypeA: 'ACCUSATIVE', caseTypeB: 'LOCATIVE' },
  { setId: 'DAT_GEN', caseTypeA: 'DATIVE', caseTypeB: 'GENITIVE' },
  { setId: 'ABL_LOC', caseTypeA: 'ABLATIVE', caseTypeB: 'LOCATIVE' },
];

export interface CasePairAggregation {
  setId: string;
  caseTypeA: string;
  caseTypeB: string;
  caseRuA: string;
  caseRuB: string;
  caseEnA: string;
  caseEnB: string;
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

export const aggregateByCasePair = (questions: GrammarQuestionProgress[]): CasePairAggregation[] => {
  const result: CasePairAggregation[] = [];
  for (const pair of CASE_PAIRS) {
    const items = questions.filter(
      q => q.caseType === pair.caseTypeA || q.caseType === pair.caseTypeB,
    );
    if (items.length === 0) continue;

    const progress = avgProgress(items);
    const learned = items.filter(q => q.score >= MASTERY_THRESHOLD).length;
    const a = items.find(q => q.caseType === pair.caseTypeA);
    const b = items.find(q => q.caseType === pair.caseTypeB);

    result.push({
      setId: pair.setId,
      caseTypeA: pair.caseTypeA,
      caseTypeB: pair.caseTypeB,
      caseRuA: a?.caseRu ?? pair.caseTypeA,
      caseRuB: b?.caseRu ?? pair.caseTypeB,
      caseEnA: a?.caseEn ?? pair.caseTypeA,
      caseEnB: b?.caseEn ?? pair.caseTypeB,
      aggregatedProgress: progress,
      totalCombinations: items.length,
      learnedCombinations: learned,
      status: progressStatus(progress),
    });
  }
  return result;
};