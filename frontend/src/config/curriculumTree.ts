/**
 * Static curriculum tree configuration for the sidebar menu.
 *
 * Source: docs/frontend/information-architecture/01-curriculum-vs-catalog.md, section 2
 *
 * Structure:
 *   2.1 Grammar (parent) -> 2.1.1-2.1.9
 *   2.2 Writing & Pronunciation
 *   2.3 Vocabulary
 *   + catalog sections (Texts)
 */

export type NodeStatus = 'available' | 'planned';

export interface CurriculumNode {
  id: string;
  /** i18n key */
  titleKey: string;
  status: NodeStatus;
  children?: CurriculumNode[];
  /** Route — only for available leaf nodes */
  route?: string;
  /** PrimeIcons icon (top-level only) */
  icon?: string;
}

export const curriculumTree: CurriculumNode[] = [
  // =========================================================================
  // 2.1 Grammar (parent node)
  // =========================================================================
  {
    id: '2.1',
    titleKey: 'section.grammar',
    status: 'available',
    icon: 'pi-book',
    children: [
      // --- 2.1.1 Nouns (Declension) ---------------------------------
      {
        id: '2.1.1',
        titleKey: 'section.nouns',
        status: 'available',
        children: [
          { id: '2.1.1.1', titleKey: 'nouns.a_masc', status: 'available', route: '/lessons/grammar/declensions-a-masc' },
          { id: '2.1.1.2', titleKey: 'nouns.a_neut', status: 'available', route: '/lessons/grammar/declensions-a-neut' },
          { id: '2.1.1.3', titleKey: 'nouns.aa_fem', status: 'available', route: '/lessons/grammar/declensions-aa-fem' },
          { id: '2.1.1.4', titleKey: 'nouns.i_stems', status: 'available', route: '/lessons/grammar/declensions-i' },
          { id: '2.1.1.5', titleKey: 'nouns.ii_stems', status: 'available', route: '/lessons/grammar/declensions-ii' },
          { id: '2.1.1.6', titleKey: 'nouns.u_stems', status: 'available', route: '/lessons/grammar/declensions-u' },
          { id: '2.1.1.7', titleKey: 'nouns.uu_stems', status: 'available', route: '/lessons/grammar/declensions-uu' },
          { id: '2.1.1.8', titleKey: 'nouns.r_stems', status: 'available', route: '/lessons/grammar/declensions-r' },
          { id: '2.1.1.9', titleKey: 'nouns.cons_an', status: 'planned' },
          { id: '2.1.1.10', titleKey: 'nouns.cons_in_vant_mant', status: 'planned' },
          { id: '2.1.1.11', titleKey: 'nouns.cons_as_is_us', status: 'planned' },
          { id: '2.1.1.12', titleKey: 'nouns.cons_at_ant', status: 'planned' },
          { id: '2.1.1.13', titleKey: 'nouns.cons_simple_final', status: 'planned' },
          { id: '2.1.1.14', titleKey: 'nouns.irregular', status: 'planned' },
        ],
      },
      // --- 2.1.2 Adjectives ------------------------------------------
      {
        id: '2.1.2',
        titleKey: 'section.adjectives',
        status: 'planned',
        children: [
          { id: '2.1.2.1', titleKey: 'adjectives.regular', status: 'planned' },
          { id: '2.1.2.2', titleKey: 'adjectives.comparison', status: 'planned' },
          { id: '2.1.2.3', titleKey: 'adjectives.numerals', status: 'planned' },
        ],
      },
      // --- 2.1.3 Pronouns --------------------------------------------
      {
        id: '2.1.3',
        titleKey: 'section.pronouns',
        status: 'planned',
        children: [
          { id: '2.1.3.1', titleKey: 'pronouns.personal', status: 'planned' },
          { id: '2.1.3.2', titleKey: 'pronouns.demonstrative', status: 'planned' },
          { id: '2.1.3.3', titleKey: 'pronouns.interrogative', status: 'planned' },
          { id: '2.1.3.4', titleKey: 'pronouns.relative', status: 'planned' },
          { id: '2.1.3.5', titleKey: 'pronouns.reflexive', status: 'planned' },
        ],
      },
      // --- 2.1.4 Numerals --------------------------------------------
      {
        id: '2.1.4',
        titleKey: 'section.numerals',
        status: 'planned',
        children: [
          { id: '2.1.4.1', titleKey: 'numerals.cardinal_1_10', status: 'planned' },
          { id: '2.1.4.2', titleKey: 'numerals.cardinal_11_100', status: 'planned' },
          { id: '2.1.4.3', titleKey: 'numerals.ordinal', status: 'planned' },
        ],
      },
      // --- 2.1.5 Verb: Present Tense (10 classes) --------------------
      {
        id: '2.1.5',
        titleKey: 'section.verbs_present',
        status: 'planned',
        children: [
          { id: '2.1.5.1', titleKey: 'verbs.class1', status: 'planned' },
          { id: '2.1.5.2', titleKey: 'verbs.class4', status: 'planned' },
          { id: '2.1.5.3', titleKey: 'verbs.class6', status: 'planned' },
          { id: '2.1.5.4', titleKey: 'verbs.class10', status: 'planned' },
          { id: '2.1.5.5', titleKey: 'verbs.class2', status: 'planned' },
          { id: '2.1.5.6', titleKey: 'verbs.class3', status: 'planned' },
          { id: '2.1.5.7', titleKey: 'verbs.class5', status: 'planned' },
          { id: '2.1.5.8', titleKey: 'verbs.class7', status: 'planned' },
          { id: '2.1.5.9', titleKey: 'verbs.class8', status: 'planned' },
          { id: '2.1.5.10', titleKey: 'verbs.class9', status: 'planned' },
        ],
      },
      // --- 2.1.6 Verb: Other Systems ---------------------------------
      {
        id: '2.1.6',
        titleKey: 'section.verbs_other',
        status: 'planned',
        children: [
          { id: '2.1.6.1', titleKey: 'verbs.imperfect', status: 'planned' },
          { id: '2.1.6.2', titleKey: 'verbs.aorist', status: 'planned' },
          { id: '2.1.6.3', titleKey: 'verbs.perfect', status: 'planned' },
          { id: '2.1.6.4', titleKey: 'verbs.future', status: 'planned' },
          { id: '2.1.6.5', titleKey: 'verbs.optative', status: 'planned' },
          { id: '2.1.6.6', titleKey: 'verbs.imperative', status: 'planned' },
          { id: '2.1.6.7', titleKey: 'verbs.causative', status: 'planned' },
          { id: '2.1.6.8', titleKey: 'verbs.passive', status: 'planned' },
          { id: '2.1.6.9', titleKey: 'verbs.desiderative', status: 'planned' },
        ],
      },
      // --- 2.1.7 Verbal Nouns ----------------------------------------
      {
        id: '2.1.7',
        titleKey: 'section.verb_nominals',
        status: 'planned',
        children: [
          { id: '2.1.7.1', titleKey: 'verbNominals.pres_part', status: 'planned' },
          { id: '2.1.7.2', titleKey: 'verbNominals.past_part', status: 'planned' },
          { id: '2.1.7.3', titleKey: 'verbNominals.absolutive', status: 'planned' },
          { id: '2.1.7.4', titleKey: 'verbNominals.infinitive', status: 'planned' },
          { id: '2.1.7.5', titleKey: 'verbNominals.gerundive', status: 'planned' },
        ],
      },
      // --- 2.1.8 Sandhi ----------------------------------------------
      {
        id: '2.1.8',
        titleKey: 'section.sandhi',
        status: 'available',
        children: [
          { id: '2.1.8.1', titleKey: 'sandhi.rules', status: 'available', route: '/grammar/emeneau-rules' },
          { id: '2.1.8.2', titleKey: 'sandhi.exercises', status: 'available', route: '/grammar/emeneau-exercises' },
          { id: '2.1.8.3', titleKey: 'sandhi.vowel', status: 'planned' },
          { id: '2.1.8.4', titleKey: 'sandhi.consonant', status: 'planned' },
          { id: '2.1.8.5', titleKey: 'sandhi.visarga', status: 'planned' },
          { id: '2.1.8.6', titleKey: 'sandhi.segmentation', status: 'planned' },
        ],
      },
      // --- 2.1.9 Syntax ----------------------------------------------
      {
        id: '2.1.9',
        titleKey: 'section.syntax',
        status: 'planned',
        children: [
          { id: '2.1.9.1', titleKey: 'syntax.adj_noun_agreement', status: 'planned' },
          { id: '2.1.9.2', titleKey: 'syntax.subj_pred_agreement', status: 'planned' },
          { id: '2.1.9.3', titleKey: 'syntax.case_roles', status: 'planned' },
          { id: '2.1.9.4', titleKey: 'syntax.word_order', status: 'planned' },
          { id: '2.1.9.5', titleKey: 'syntax.complex_sentence', status: 'planned' },
        ],
      },
    ],
  },

  // =========================================================================
  // 2.2 Writing & Pronunciation
  // =========================================================================
  {
    id: '2.2',
    titleKey: 'section.writing',
    status: 'planned',
    icon: 'pi-pen-to-square',
    children: [
      { id: '2.2.1', titleKey: 'writing.alphabet', status: 'planned' },
      { id: '2.2.2', titleKey: 'writing.ligatures', status: 'planned' },
      { id: '2.2.3', titleKey: 'writing.transliteration', status: 'planned' },
      { id: '2.2.4', titleKey: 'writing.prosody', status: 'planned' },
    ],
  },

  // =========================================================================
  // 2.3 Vocabulary
  // =========================================================================
  {
    id: '2.3',
    titleKey: 'section.vocabulary',
    status: 'available',
    icon: 'pi-list',
    children: [
      { id: '2.3.1', titleKey: 'vocabulary.sangraha_quizzes', status: 'planned' },
      { id: '2.3.2', titleKey: 'vocabulary.thematic', status: 'planned' },
      { id: '2.3.3', titleKey: 'vocabulary.frequency', status: 'planned' },
      { id: '2.3.4', titleKey: 'vocabulary.basic', status: 'available', route: '/vocabulary/basic' },
      { id: '2.3.5', titleKey: 'vocabulary.lists', status: 'available', route: '/vocabulary/lists' },
    ],
  },

  // =========================================================================
  // Catalog sections
  // =========================================================================
  {
    id: 'catalog.texts',
    titleKey: 'catalog.texts',
    status: 'available',
    icon: 'pi-bookmark',
    route: '/sangraha',
  },
];
