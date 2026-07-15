export enum LessonType {
  DECLENSIONS = 'DECLENSIONS',
  CONJUGATIONS = 'CONJUGATIONS',
  VOCABULARY = 'VOCABULARY',
  VOCABULARY_BASIC = 'VOCABULARY_BASIC',
  VOCABULARY_TEXTS = 'VOCABULARY_TEXTS',
}

export enum Difficulty {
  BEGINNER = 'BEGINNER',
  INTERMEDIATE = 'INTERMEDIATE',
  ADVANCED = 'ADVANCED',
}

export enum SessionStatus {
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  ABANDONED = 'ABANDONED',
}

/* ---------- helpers ---------- */

export const isDeclensionsQuiz = (lt: LessonType): boolean =>
  [LessonType.DECLENSIONS].includes(lt);

export const isVocabularyQuiz = (lt: LessonType): boolean =>
  [LessonType.VOCABULARY, LessonType.VOCABULARY_BASIC, LessonType.VOCABULARY_TEXTS].includes(lt);

export const getQuizCategory = (
  lt: LessonType,
): 'declensions' | 'conjugations' | 'vocabulary' | 'other' => {
  if (isDeclensionsQuiz(lt)) return 'declensions';
  if (isVocabularyQuiz(lt)) return 'vocabulary';
  if (lt === LessonType.CONJUGATIONS) return 'conjugations';
  return 'other';
};
