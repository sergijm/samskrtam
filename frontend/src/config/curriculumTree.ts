/**
 * Статическая конфигурация дерева курикулума для бокового меню.
 *
 * Источник: docs/frontend/information-architecture/01-curriculum-vs-catalog.md §2
 *
 * Каждый узел: id, titleKey (i18n-ключ в namespace curriculum),
 * status: 'available' | 'planned', children?, route?
 * route — только у листьев со статусом 'available'.
 * Узлы 'planned' — некликабельны, визуально приглушены.
 */

export type NodeStatus = 'available' | 'planned';

export interface CurriculumNode {
  id: string;
  /** i18n ключ, префикс '' */
  titleKey: string;
  status: NodeStatus;
  children?: CurriculumNode[];
  /** Маршрут — только для available листьев */
  route?: string;
  /** Иконка PrimeIcons (только для top-level) */
  icon?: string;
}

/**
 * Все узлы 2.1–2.9, 2.12 из information-architecture.md без свёртки.
 * Каталожные разделы (§3) — отдельные top-level пункты.
 */
export const curriculumTree: CurriculumNode[] = [
  // ─── §2.1 Существительные (склонение) ───────────────────────────────
  {
    id: '2.1',
    titleKey: 'section.nouns',
    status: 'available',
    icon: 'pi-pencil',
    children: [
      // Гласные основы
      { id: '2.1.1', titleKey: 'nouns.a_masc', status: 'available', route: '/lessons/grammar/declensions-a-masc' },
      { id: '2.1.2', titleKey: 'nouns.a_neut', status: 'available', route: '/lessons/grammar/declensions-a-neut' },
      { id: '2.1.3', titleKey: 'nouns.aa_fem', status: 'available', route: '/lessons/grammar/declensions-aa-fem' },
      { id: '2.1.4', titleKey: 'nouns.i_stems', status: 'available', route: '/lessons/grammar/noun-i' },
      { id: '2.1.5', titleKey: 'nouns.ii_stems', status: 'available', route: '/lessons/grammar/noun-ii' },
      { id: '2.1.6', titleKey: 'nouns.u_stems', status: 'available', route: '/lessons/grammar/noun-u' },
      { id: '2.1.7', titleKey: 'nouns.uu_stems', status: 'available', route: '/lessons/grammar/noun-uu' },
      { id: '2.1.8', titleKey: 'nouns.r_stems', status: 'available', route: '/lessons/grammar/noun-r' },
      // Согласные основы
      { id: '2.1.9', titleKey: 'nouns.cons_an', status: 'planned' },
      { id: '2.1.10', titleKey: 'nouns.cons_in_vant_mant', status: 'planned' },
      { id: '2.1.11', titleKey: 'nouns.cons_as_is_us', status: 'planned' },
      { id: '2.1.12', titleKey: 'nouns.cons_at_ant', status: 'planned' },
      { id: '2.1.13', titleKey: 'nouns.cons_simple_final', status: 'planned' },
      { id: '2.1.14', titleKey: 'nouns.irregular', status: 'planned' },
    ],
  },
  // ─── §2.2 Прилагательные ────────────────────────────────────────────
  {
    id: '2.2',
    titleKey: 'section.adjectives',
    status: 'planned',
    icon: 'pi-tag',
    children: [
      { id: '2.2.1', titleKey: 'adjectives.regular', status: 'planned' },
      { id: '2.2.2', titleKey: 'adjectives.comparison', status: 'planned' },
      { id: '2.2.3', titleKey: 'adjectives.numerals', status: 'planned' },
    ],
  },
  // ─── §2.3 Местоимения ───────────────────────────────────────────────
  {
    id: '2.3',
    titleKey: 'section.pronouns',
    status: 'planned',
    icon: 'pi-user',
    children: [
      { id: '2.3.1', titleKey: 'pronouns.personal', status: 'planned' },
      { id: '2.3.2', titleKey: 'pronouns.demonstrative', status: 'planned' },
      { id: '2.3.3', titleKey: 'pronouns.interrogative', status: 'planned' },
      { id: '2.3.4', titleKey: 'pronouns.relative', status: 'planned' },
      { id: '2.3.5', titleKey: 'pronouns.reflexive', status: 'planned' },
    ],
  },
  // ─── §2.4 Числительные ──────────────────────────────────────────────
  {
    id: '2.4',
    titleKey: 'section.numerals',
    status: 'planned',
    icon: 'pi-sort-numeric-down',
    children: [
      { id: '2.4.1', titleKey: 'numerals.cardinal_1_10', status: 'planned' },
      { id: '2.4.2', titleKey: 'numerals.cardinal_11_100', status: 'planned' },
      { id: '2.4.3', titleKey: 'numerals.ordinal', status: 'planned' },
    ],
  },
  // ─── §2.5 Глагол: настоящее время (10 классов) ──────────────────────
  {
    id: '2.5',
    titleKey: 'section.verbs_present',
    status: 'planned',
    icon: 'pi-play',
    children: [
      { id: '2.5.1', titleKey: 'verbs.class1', status: 'planned' },
      { id: '2.5.2', titleKey: 'verbs.class4', status: 'planned' },
      { id: '2.5.3', titleKey: 'verbs.class6', status: 'planned' },
      { id: '2.5.4', titleKey: 'verbs.class10', status: 'planned' },
      { id: '2.5.5', titleKey: 'verbs.class2', status: 'planned' },
      { id: '2.5.6', titleKey: 'verbs.class3', status: 'planned' },
      { id: '2.5.7', titleKey: 'verbs.class5', status: 'planned' },
      { id: '2.5.8', titleKey: 'verbs.class7', status: 'planned' },
      { id: '2.5.9', titleKey: 'verbs.class8', status: 'planned' },
      { id: '2.5.10', titleKey: 'verbs.class9', status: 'planned' },
    ],
  },
  // ─── §2.6 Глагол: остальные системы ─────────────────────────────────
  {
    id: '2.6',
    titleKey: 'section.verbs_other',
    status: 'planned',
    icon: 'pi-forward',
    children: [
      { id: '2.6.1', titleKey: 'verbs.imperfect', status: 'planned' },
      { id: '2.6.2', titleKey: 'verbs.aorist', status: 'planned' },
      { id: '2.6.3', titleKey: 'verbs.perfect', status: 'planned' },
      { id: '2.6.4', titleKey: 'verbs.future', status: 'planned' },
      { id: '2.6.5', titleKey: 'verbs.optative', status: 'planned' },
      { id: '2.6.6', titleKey: 'verbs.imperative', status: 'planned' },
      { id: '2.6.7', titleKey: 'verbs.causative', status: 'planned' },
      { id: '2.6.8', titleKey: 'verbs.passive', status: 'planned' },
      { id: '2.6.9', titleKey: 'verbs.desiderative', status: 'planned' },
    ],
  },
  // ─── §2.7 Именные формы глагола ─────────────────────────────────────
  {
    id: '2.7',
    titleKey: 'section.verb_nominals',
    status: 'planned',
    icon: 'pi-file-edit',
    children: [
      { id: '2.7.1', titleKey: 'verbNominals.pres_part', status: 'planned' },
      { id: '2.7.2', titleKey: 'verbNominals.past_part', status: 'planned' },
      { id: '2.7.3', titleKey: 'verbNominals.absolutive', status: 'planned' },
      { id: '2.7.4', titleKey: 'verbNominals.infinitive', status: 'planned' },
      { id: '2.7.5', titleKey: 'verbNominals.gerundive', status: 'planned' },
    ],
  },
  // ─── §2.8 Сандхи ────────────────────────────────────────────────────
  {
    id: '2.8',
    titleKey: 'section.sandhi',
    status: 'available',
    icon: 'pi-link',
    children: [
      { id: '2.8.1', titleKey: 'sandhi.rules', status: 'available', route: '/grammar/emeneau-rules' },
      { id: '2.8.2', titleKey: 'sandhi.exercises', status: 'available', route: '/grammar/emeneau-exercises' },
      { id: '2.8.3', titleKey: 'sandhi.vowel', status: 'planned' },
      { id: '2.8.4', titleKey: 'sandhi.consonant', status: 'planned' },
      { id: '2.8.5', titleKey: 'sandhi.visarga', status: 'planned' },
      { id: '2.8.6', titleKey: 'sandhi.segmentation', status: 'planned' },
    ],
  },
  // ─── §2.9 Синтаксис ─────────────────────────────────────────────────
  {
    id: '2.9',
    titleKey: 'section.syntax',
    status: 'planned',
    icon: 'pi-sitemap',
    children: [
      { id: '2.9.1', titleKey: 'syntax.adj_noun_agreement', status: 'planned' },
      { id: '2.9.2', titleKey: 'syntax.subj_pred_agreement', status: 'planned' },
      { id: '2.9.3', titleKey: 'syntax.case_roles', status: 'planned' },
      { id: '2.9.4', titleKey: 'syntax.word_order', status: 'planned' },
      { id: '2.9.5', titleKey: 'syntax.complex_sentence', status: 'planned' },
    ],
  },
  // ─── §2.12 Письмо и произношение ────────────────────────────────────
  {
    id: '2.12',
    titleKey: 'section.writing',
    status: 'planned',
    icon: 'pi-pen-to-square',
    children: [
      { id: '2.12.1', titleKey: 'writing.alphabet', status: 'planned' },
      { id: '2.12.2', titleKey: 'writing.ligatures', status: 'planned' },
      { id: '2.12.3', titleKey: 'writing.transliteration', status: 'planned' },
      { id: '2.12.4', titleKey: 'writing.prosody', status: 'planned' },
    ],
  },
  // ─── Каталожные разделы (плоские, без children) ─────────────────────
  {
    id: 'catalog.lexicon',
    titleKey: 'catalog.lexicon',
    status: 'available',
    icon: 'pi-book',
    route: '/vocabulary',
  },
  {
    id: 'catalog.texts',
    titleKey: 'catalog.texts',
    status: 'available',
    icon: 'pi-bookmark',
    route: '/sangraha',
  },
];
