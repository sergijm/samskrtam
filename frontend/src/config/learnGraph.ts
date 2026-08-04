/**
 * Learning graph configuration (static snapshot of docs/services/curriculum.md §2).
 *
 * Layers are computed by topological ordering of TopicPrerequisite — here kept as a
 * static snapshot. Each topic is always available; prerequisites are only UI hints
 * ("recommend first") — no blocking anywhere.
 *
 * NOTE: `status`/`progressPercent` are DEMO values until the progress API is ready —
 * they only illustrate the UI states (mastered / in progress / recommended / review).
 */

export type TopicStatus = 'mastered' | 'in_progress' | 'recommended' | 'review' | 'available';

export type TypeGroup = 'vocabulary' | 'declension' | 'sandhi' | 'conjugation' | 'syntax' | 'other';

export interface LearnGraphTopic {
  id: string;
  /** friendly filter group, also drives the icon and tooltip */
  typeGroup: TypeGroup;
  /** route for "Study →" / Continue CTA; falls back to /grammar */
  route?: string;
  /** DEMO status (until progress API) */
  status?: TopicStatus;
  /** DEMO progress for in_progress topics */
  progressPercent?: number;
  /** prerequisite topic ids (within previous layers) */
  prerequisites?: string[];
}

export interface LearnGraphLayer {
  id: string;
  /** i18n key prefix: learnGraph.layers.<id>.title / .description */
  titleKey: string;
  descriptionKey: string;
  topics: LearnGraphTopic[];
  /** "Без зависимостей" layer — always available, shown with distinct style */
  alwaysAvailable?: boolean;
}

export const learnGraphLayers: LearnGraphLayer[] = [
  {
    id: 'l0',
    titleKey: 'learnGraph.layers.l0.title',
    descriptionKey: 'learnGraph.layers.l0.description',
    topics: [
      { id: 'basicVocabulary', typeGroup: 'vocabulary', route: '/vocabulary/basic', status: 'mastered' },
      { id: 'sandhiVowels', typeGroup: 'sandhi', route: '/grammar/emeneau-rules', status: 'review' },
    ],
  },
  {
    id: 'l1',
    titleKey: 'learnGraph.layers.l1.title',
    descriptionKey: 'learnGraph.layers.l1.description',
    topics: [
      {
        id: 'declensionAStems',
        typeGroup: 'declension',
        route: '/lessons/grammar/declensions-a-masc',
        status: 'in_progress',
        progressPercent: 62,
      },
      { id: 'sandhiConsonants', typeGroup: 'sandhi', route: '/grammar/emeneau-rules', prerequisites: ['sandhiVowels'] },
    ],
  },
  {
    id: 'l2',
    titleKey: 'learnGraph.layers.l2.title',
    descriptionKey: 'learnGraph.layers.l2.description',
    topics: [
      { id: 'declensionIU', typeGroup: 'declension', prerequisites: ['declensionAStems'] },
      {
        id: 'pronounsPersonal',
        typeGroup: 'declension',
        route: '/lessons/grammar/pronouns-personal',
        status: 'recommended',
        prerequisites: ['declensionAStems'],
      },
      {
        id: 'conjugationParasmaipada',
        typeGroup: 'conjugation',
        prerequisites: ['declensionAStems', 'basicVocabulary'],
      },
    ],
  },
  {
    id: 'l3',
    titleKey: 'learnGraph.layers.l3.title',
    descriptionKey: 'learnGraph.layers.l3.description',
    topics: [
      { id: 'pronounsOther', typeGroup: 'declension', prerequisites: ['pronounsPersonal'] },
      { id: 'numerals1_4', typeGroup: 'other', prerequisites: ['pronounsOther'] },
      { id: 'conjugationAtmanepada', typeGroup: 'conjugation', prerequisites: ['conjugationParasmaipada'] },
      { id: 'conjugationLangLrt', typeGroup: 'conjugation', prerequisites: ['conjugationParasmaipada'] },
      {
        id: 'participlesPresent',
        typeGroup: 'conjugation',
        prerequisites: ['conjugationParasmaipada', 'declensionAStems'],
      },
      { id: 'absolutives', typeGroup: 'conjugation', prerequisites: ['conjugationParasmaipada'] },
      { id: 'infinitives', typeGroup: 'conjugation', prerequisites: ['conjugationParasmaipada'] },
      { id: 'vocabularyExpansion', typeGroup: 'vocabulary', prerequisites: ['basicVocabulary'] },
    ],
  },
  {
    id: 'l4',
    titleKey: 'learnGraph.layers.l4.title',
    descriptionKey: 'learnGraph.layers.l4.description',
    topics: [
      { id: 'karaka', typeGroup: 'syntax' },
      { id: 'agreement', typeGroup: 'syntax' },
      { id: 'compoundsSplit', typeGroup: 'syntax' },
      { id: 'taddhita', typeGroup: 'syntax' },
      { id: 'syllableWeight', typeGroup: 'other' },
    ],
  },
  {
    id: 'l5',
    titleKey: 'learnGraph.layers.l5.title',
    descriptionKey: 'learnGraph.layers.l5.description',
    topics: [
      { id: 'relativeClause', typeGroup: 'syntax', prerequisites: ['pronounsOther', 'karaka'] },
      { id: 'participleClause', typeGroup: 'syntax', prerequisites: ['participlesPresent', 'karaka'] },
      { id: 'compoundsType', typeGroup: 'syntax', prerequisites: ['compoundsSplit'] },
      { id: 'chandas', typeGroup: 'other', prerequisites: ['syllableWeight'] },
      { id: 'idioms', typeGroup: 'vocabulary', prerequisites: ['basicVocabulary'] },
    ],
  },
  {
    id: 'l6',
    titleKey: 'learnGraph.layers.l6.title',
    descriptionKey: 'learnGraph.layers.l6.description',
    topics: [
      {
        id: 'sentenceTranslation',
        typeGroup: 'syntax',
        prerequisites: ['karaka', 'relativeClause', 'participleClause'],
      },
    ],
  },
  {
    id: 'always',
    titleKey: 'learnGraph.layers.always.title',
    descriptionKey: 'learnGraph.layers.always.description',
    alwaysAvailable: true,
    topics: [
      { id: 'mixedReview', typeGroup: 'other' },
      { id: 'errorCorrection', typeGroup: 'other' },
    ],
  },
];

/** topicId -> layerId, used to jump to the layer of a prerequisite topic */
export function topicLayerIndex(layers: LearnGraphLayer[]): Record<string, string> {
  const index: Record<string, string> = {};
  for (const layer of layers) {
    for (const topic of layer.topics) {
      index[topic.id] = layer.id;
    }
  }
  return index;
}
