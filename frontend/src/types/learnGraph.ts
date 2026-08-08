/**
 * Types for the learning map (dashboard /curriculum/learn-graph endpoint),
 * mirroring curriculum-service DTOs (LearnGraphResponse / LearnLayerDto /
 * LearnTopicDto).
 */

export type TopicTypeGroup = 'vocabulary' | 'declension' | 'sandhi' | 'conjugation' | 'syntax' | 'other';

export type TopicStatus = 'mastered' | 'in_progress' | 'recommended' | 'review' | 'available';

export interface LearnGraphTopic {
  id?: string;
  /** stable topic slug, e.g. a-stem-masc — used as the card key */
  code: string;
  titleRu: string;
  titleEn: string;
  /** UI filter/icon group */
  typeGroup: TopicTypeGroup;
  /** route for the "Study / Continue" button */
  route?: string | null;
  /** per-user progress state (currently random on the backend) */
  status?: TopicStatus;
  progressPercent?: number;
  /** prerequisite topic codes (UI hints only) */
  prerequisites?: string[];
}

export interface LearnLayerDto {
  /** L0..L6 for learning levels, or "always" for the evergreen layer */
  id: string;
  /** matching i18n key: learnGraph.layers.<id>.title / .description */
  alwaysAvailable: boolean;
  topics: LearnTopicDto[];
}

export interface LearnGraphResponse {
  layers: LearnLayerDto[];
}