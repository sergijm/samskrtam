/**
 * Mock-данные стартовой страницы «Лексика».
 *
 * Значения вымышленные, но реалистичные. Заменяются API-реализацией
 * `LexiconService` (см. src/services/lexiconService.ts) без изменения UI.
 */

import { LexiconDashboardData } from '../types/lexicon';

export const mockLexiconDashboard: LexiconDashboardData = {
  summary: {
    totalWords: 2000,
    masteredCount: 684,
  },

  today: {
    reviewDue: 12,
    newWords: 8,
    weakWords: 3,
  },

  frequencyBands: [
    { id: 'f1', from: 1, to: 100, wordCount: 100, masteredCount: 89 },
    { id: 'f2', from: 101, to: 250, wordCount: 150, masteredCount: 96 },
    { id: 'f3', from: 251, to: 500, wordCount: 250, masteredCount: 78 },
    { id: 'f4', from: 501, to: 1000, wordCount: 500, masteredCount: 181 },
    { id: 'f5', from: 1001, to: 1500, wordCount: 500, masteredCount: 120 },
    { id: 'f6', from: 1501, to: 2000, wordCount: 500, masteredCount: 120 },
  ],

  topics: [
    { id: 'nature', nameKey: 'lexicon.topics.nature', wordCount: 140, masteredCount: 52 },
    { id: 'animals', nameKey: 'lexicon.topics.animals', wordCount: 84, masteredCount: 32 },
    { id: 'human', nameKey: 'lexicon.topics.human', wordCount: 96, masteredCount: 30 },
    { id: 'family', nameKey: 'lexicon.topics.family', wordCount: 70, masteredCount: 25 },
    { id: 'food', nameKey: 'lexicon.topics.food', wordCount: 110, masteredCount: 48 },
    { id: 'home', nameKey: 'lexicon.topics.home', wordCount: 90, masteredCount: 33 },
    { id: 'travel', nameKey: 'lexicon.topics.travel', wordCount: 80, masteredCount: 18 },
    { id: 'time', nameKey: 'lexicon.topics.time', wordCount: 75, masteredCount: 40 },
    { id: 'emotions', nameKey: 'lexicon.topics.emotions', wordCount: 65, masteredCount: 15 },
    { id: 'society', nameKey: 'lexicon.topics.society', wordCount: 95, masteredCount: 21 },
    { id: 'knowledge', nameKey: 'lexicon.topics.knowledge', wordCount: 88, masteredCount: 27 },
    { id: 'actions', nameKey: 'lexicon.topics.actions', wordCount: 120, masteredCount: 55 },
    { id: 'quantity', nameKey: 'lexicon.topics.quantity', wordCount: 60, masteredCount: 38 },
    { id: 'colors', nameKey: 'lexicon.topics.colors', wordCount: 45, masteredCount: 30 },
    { id: 'plants', nameKey: 'lexicon.topics.plants', wordCount: 72, masteredCount: 12 },
  ],

  pos: [
    { id: 'nouns', nameKey: 'lexicon.pos.nouns', wordCount: 720 },
    { id: 'verbs', nameKey: 'lexicon.pos.verbs', wordCount: 410 },
    { id: 'adjectives', nameKey: 'lexicon.pos.adjectives', wordCount: 260 },
    { id: 'adverbs', nameKey: 'lexicon.pos.adverbs', wordCount: 95 },
    { id: 'pronouns', nameKey: 'lexicon.pos.pronouns', wordCount: 60 },
    { id: 'other', nameKey: 'lexicon.pos.other', wordCount: 215 },
  ],

  sources: [
    { id: 'gita', title: 'Bhagavad Gītā', devanagari: 'भगवद्गीता', wordCount: 420, masteredCount: 187 },
    { id: 'panchatantra', title: 'Pañcatantra', devanagari: 'पञ्चतन्त्र', wordCount: 620, masteredCount: 231 },
    { id: 'hitopadesa', title: 'Hitopadeśa', devanagari: 'हितोपदेश', wordCount: 480, masteredCount: 145 },
    { id: 'subhasita', title: 'Subhāṣitāṇi', devanagari: 'सुभाषिताणि', wordCount: 250, masteredCount: 96 },
  ],

  collections: [
    { id: 'hard', nameKey: 'lexicon.collections.hardWords', wordCount: 42 },
    { id: 'fromToday', nameKey: 'lexicon.collections.fromToday', wordCount: 18 },
    { id: 'exam', nameKey: 'lexicon.collections.exam', wordCount: 87 },
  ],

  quickStart: [
    { id: 'new10', titleKey: 'lexicon.quickStart.new10', metaKey: 'lexicon.quickStart.new10Meta' },
    { id: 'mistakes', titleKey: 'lexicon.quickStart.mistakes', metaKey: 'lexicon.quickStart.mistakesMeta' },
    { id: 'top100', titleKey: 'lexicon.quickStart.top100', metaKey: 'lexicon.quickStart.top100Meta' },
  ],
};
