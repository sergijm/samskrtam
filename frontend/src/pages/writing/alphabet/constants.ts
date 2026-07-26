// ============================================================================
// Static data & styles: Sanskrit (Devanagari) alphabet & numerals reference chart.
// ============================================================================

import React from 'react';

export const RED = '#d21f1f';
export const GRAY = '#9a9a9a';

export interface Varga {
  titleRu: string;
  titleEn: string;
  iastLabel: string;
  descRu: string;
  descEn: string;
  letters: [string, string, string, string, string];
  iast: [string, string, string, string, string];
  semivowel: { dev: string; iast: string } | null;
  sibilant: { dev: string; iast: string } | null;
}

export const VARGAS: Varga[] = [
  {
    titleRu: 'Задне\u00ADязычные', titleEn: 'Velars',
    iastLabel: 'kaṇṭhaḥ',
    descRu: 'заднюю часть языка поднять к заднему нёбу',
    descEn: 'back of the tongue raised toward the soft palate',
    letters: ['क', 'ख', 'ग', 'घ', 'ङ'],
    iast: ['ka', 'kha', 'ga', 'gha', 'ṅa'],
    semivowel: { dev: 'य', iast: 'ya' },
    sibilant: { dev: 'श', iast: 'śa' },
  },
  {
    titleRu: 'Пала\u00ADтальные', titleEn: 'Palatals',
    iastLabel: 'tālu',
    descRu: 'среднюю часть языка соприкоснуть с твёрдым нёбом',
    descEn: 'middle of the tongue touches the hard palate',
    letters: ['च', 'छ', 'ज', 'झ', 'ञ'],
    iast: ['ca', 'cha', 'ja', 'jha', 'ña'],
    semivowel: { dev: 'र', iast: 'ra' },
    sibilant: { dev: 'ष', iast: 'ṣa' },
  },
  {
    titleRu: 'Цере\u00ADбральные', titleEn: 'Retroflexes',
    iastLabel: 'mūrdhā',
    descRu: 'кончик языка загнуть и поднять к твёрдому нёбу',
    descEn: 'tongue tip curled up toward the hard palate',
    letters: ['ट', 'ठ', 'ड', 'ढ', 'ण'],
    iast: ['ṭa', 'ṭha', 'ḍa', 'ḍha', 'ṇa'],
    semivowel: { dev: 'ल', iast: 'la' },
    sibilant: { dev: 'स', iast: 'sa' },
  },
  {
    titleRu: 'Зубные', titleEn: 'Dentals',
    iastLabel: 'dantāḥ',
    descRu: 'язык придвинуть к верхним зубам',
    descEn: 'tongue pressed against the upper teeth',
    letters: ['त', 'थ', 'द', 'ध', 'न'],
    iast: ['ta', 'tha', 'da', 'dha', 'na'],
    semivowel: { dev: 'व', iast: 'va' },
    sibilant: null,
  },
  {
    titleRu: 'Губные', titleEn: 'Labials',
    iastLabel: 'oṣṭhāḥ',
    descRu: 'сомкнуть и разомкнуть губы',
    descEn: 'lips close and open',
    letters: ['प', 'फ', 'ब', 'भ', 'म'],
    iast: ['pa', 'pha', 'ba', 'bha', 'ma'],
    semivowel: null,
    sibilant: null,
  },
];

export const CONJUNCT_EXAMPLES: Array<{ dev: string; iast: string }> = [
  { dev: 'क्त', iast: 'kta' }, { dev: 'क्ष', iast: 'kṣa' }, { dev: 'ज्ञ', iast: 'jña' },
  { dev: 'त्र', iast: 'tra' }, { dev: 'त्त', iast: 'tta' }, { dev: 'द्य', iast: 'dya' },
  { dev: 'श्च', iast: 'śca' }, { dev: 'स्व', iast: 'sva' }, { dev: 'श्र', iast: 'śra' },
];

export interface NumeralItem {
  dev: string;
  word: string;
  value: number | string;
  faint?: boolean;
}

export const NUMERALS: NumeralItem[] = [
  { dev: '॰', word: 'śūnya', value: 0, faint: true },
  { dev: '१', word: 'eka', value: 1 },
  { dev: '२', word: 'dva', value: 2 },
  { dev: '३', word: 'tri', value: 3 },
  { dev: '४', word: 'catur', value: 4 },
  { dev: '५', word: 'pañca', value: 5 },
  { dev: '६', word: 'ṣaṣ', value: 6 },
  { dev: '७', word: 'saptan', value: 7 },
  { dev: '८', word: 'aṣṭan', value: 8 },
  { dev: '९', word: 'navan', value: 9 },
  { dev: '१०', word: 'daśan', value: 10 },
  { dev: '२०', word: 'viṃśati', value: 20 },
  { dev: '३०', word: 'triṃśat', value: 30 },
  { dev: '४०', word: 'catvāriṃśat', value: 40 },
  { dev: '५०', word: 'pañcāśat', value: 50 },
  { dev: '६०', word: 'ṣaṣṭi', value: 60 },
  { dev: '७०', word: 'saptati', value: 70 },
  { dev: '८०', word: 'aśīti', value: 80 },
  { dev: '९०', word: 'navati', value: 90 },
  { dev: '१००', word: 'śata', value: 100 },
  { dev: '१०००', word: 'sahasra', value: 1000 },
];

// ---- shared cell styles ----

export const cellBase: React.CSSProperties = {
  border: '1px solid #cfcfcf',
  textAlign: 'center',
  verticalAlign: 'middle',
  padding: '6px 10px',
};

export const devStyle: React.CSSProperties = { fontSize: '1.7rem', lineHeight: 1.1 };
export const iastStyle: React.CSSProperties = { fontSize: '0.8rem', color: '#444' };

export const sectionTitleStyle: React.CSSProperties = {
  ...cellBase,
  fontWeight: 700,
  background: 'var(--surface-100, #f2f2f2)',
};

export const subHeaderStyle: React.CSSProperties = {
  ...cellBase,
  fontWeight: 400,
  fontSize: '0.78rem',
  background: 'var(--surface-50, #fafafa)',
};
