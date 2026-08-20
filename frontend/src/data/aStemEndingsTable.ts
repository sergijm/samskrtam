/**
 * Static reference tables: case endings for Sanskrit noun stems.
 *
 * Designed as a reusable data module — the same structure works for any stem type
 * (-a, -ā, -i, -u, consonant, etc.). Each table declares its own columns,
 * so a single DeclensionEndingsReferenceTable component can render all variants.
 */

export interface EndingsCell {
  /** IAST transliteration of the ending, e.g. "-s", "-ā/-bhyām". */
  text: string;
  /** Number of rows this cell spans vertically (rowSpan). 0 = skip (covered by previous cell). */
  rowSpan?: number;
  /** Number of columns this cell spans horizontally (colSpan). 0 = skip (covered by previous cell). */
  colSpan?: number;
  /** If true, the cell is an identity marker ("= stem" or "= N.") resolved via i18n. */
  isIdentity?: boolean;
  /** If true AND isIdentity, use dual/plural identity label ("= N.") instead of singular ("= stem"). */
  identityDuPl?: boolean;
  /** Optional grade hint for background colour: "strong" (guṇa/vṛddhi) or "zero". */
  grade?: 'strong' | 'zero';
}

export interface ColumnDescriptor {
  key: string;
  label: string;
}

export interface EndingsRow {
  caseKey: string;
  cells: Record<string, EndingsCell>;
}

export interface EndingsTableData {
  titleKey: string;
  columns: ColumnDescriptor[];
  rows: EndingsRow[];
}

/** Maps an endings-table column key to the (numberType, optional gender) axis it encodes. */
export const ENDINGS_COLUMN_TO_NUMBER_GENDER: Record<string, { numberType: string; gender?: string }> = {
  sgM: { numberType: 'SINGULAR', gender: 'MASCULINE' },
  sgN: { numberType: 'SINGULAR', gender: 'NEUTER' },
  duMN: { numberType: 'DUAL' },
  plM: { numberType: 'PLURAL', gender: 'MASCULINE' },
  plN: { numberType: 'PLURAL', gender: 'NEUTER' },
  // -ā stems (feminine)
  sg: { numberType: 'SINGULAR', gender: 'FEMININE' },
  du: { numberType: 'DUAL' },
  pl: { numberType: 'PLURAL' },
  // -ī/-ū stems (feminine, distinct singular columns per vowel length)
  sgI: { numberType: 'SINGULAR', gender: 'FEMININE' },
  sgU: { numberType: 'SINGULAR', gender: 'FEMININE' },
  // -ṛ stems (m/f merged, n separate)
  sgMF: { numberType: 'SINGULAR' },
  duMF: { numberType: 'DUAL' },
  plMF: { numberType: 'PLURAL' },
};

/** Maps an EndingsRow.caseKey (lowercase) to the uppercase CASE_TYPES value. */
export const CASE_KEY_TO_CASE_TYPE: Record<string, string> = {
  nominative: 'NOMINATIVE',
  accusative: 'ACCUSATIVE',
  instrumental: 'INSTRUMENTAL',
  dative: 'DATIVE',
  ablative: 'ABLATIVE',
  genitive: 'GENITIVE',
  locative: 'LOCATIVE',
  vocative: 'VOCATIVE',
};

// ============================================================================
// -a stems (masculine & neuter)
// ============================================================================

const A_STEM_COLS: ColumnDescriptor[] = [
  { key: 'sgM', label: 'sg. m' },
  { key: 'sgN', label: 'sg. n' },
  { key: 'duMN', label: 'du. (m/n)' },
  { key: 'plM', label: 'pl. m' },
  { key: 'plN', label: 'pl. n' },
];

const A_STEM_ROWS: EndingsRow[] = [
  {
    caseKey: 'nominative',
    cells: {
      sgM: { text: '-s' },
      sgN: { text: '-m' },
      duMN: { text: '-au' },
      plM: { text: '-ās' },
      plN: { text: '-āni', rowSpan: 2 },
    },
  },
  {
    caseKey: 'accusative',
    cells: {
      sgM: { text: '-m' },
      sgN: { text: '-m' },
      duMN: { text: '-e' },
      plM: { text: '-ān' },
      plN: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'instrumental',
    cells: {
      sgM: { text: '-e/-na' },
      sgN: { text: '-e/-na' },
      duMN: { text: '-ā/-bhyām', rowSpan: 3 },
      plM: { text: '-ais' },
      plN: { text: '-ais' },
    },
  },
  {
    caseKey: 'dative',
    cells: {
      sgM: { text: '-āya' },
      sgN: { text: '-āya' },
      duMN: { text: '', rowSpan: 0 },
      plM: { text: '-e/-bhyas', rowSpan: 2 },
      plN: { text: '-e/-bhyas', rowSpan: 2 },
    },
  },
  {
    caseKey: 'ablative',
    cells: {
      sgM: { text: '-āt' },
      sgN: { text: '-āt' },
      duMN: { text: '', rowSpan: 0 },
      plM: { text: '', rowSpan: 0 },
      plN: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'genitive',
    cells: {
      sgM: { text: '-sya' },
      sgN: { text: '-sya' },
      duMN: { text: '-ay/-os', rowSpan: 2 },
      plM: { text: '-ān/-ām' },
      plN: { text: '-ān/-ām' },
    },
  },
  {
    caseKey: 'locative',
    cells: {
      sgM: { text: '-e' },
      sgN: { text: '-e' },
      duMN: { text: '', rowSpan: 0 },
      plM: { text: '-e/-ṣu' },
      plN: { text: '-e/-ṣu' },
    },
  },
  {
    caseKey: 'vocative',
    cells: {
      sgM: { text: '', isIdentity: true },
      sgN: { text: '', isIdentity: true },
      duMN: { text: '', isIdentity: true, identityDuPl: true },
      plM: { text: '', isIdentity: true, identityDuPl: true },
      plN: { text: '', isIdentity: true, identityDuPl: true },
    },
  },
];

export const aStemEndings: EndingsTableData = {
  titleKey: 'endings.titleAStem',
  columns: A_STEM_COLS,
  rows: A_STEM_ROWS,
};

// ============================================================================
// -ā stems (feminine only, no m/n split)
// ============================================================================

const AA_STEM_COLS: ColumnDescriptor[] = [
  { key: 'sg', label: 'sg.' },
  { key: 'du', label: 'du.' },
  { key: 'pl', label: 'pl.' },
];

const AA_STEM_ROWS: EndingsRow[] = [
  {
    caseKey: 'nominative',
    cells: {
      sg: { text: '—' },
      du: { text: '-e', rowSpan: 2 },
      pl: { text: '-ās', rowSpan: 2 },
    },
  },
  {
    caseKey: 'accusative',
    cells: {
      sg: { text: '-m' },
      du: { text: '', rowSpan: 0 },
      pl: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'instrumental',
    cells: {
      sg: { text: '-ā' },
      du: { text: '-bhyām', rowSpan: 3 },
      pl: { text: '-bhis' },
    },
  },
  {
    caseKey: 'dative',
    cells: {
      sg: { text: '-ai' },
      du: { text: '', rowSpan: 0 },
      pl: { text: '-bhyas', rowSpan: 2 },
    },
  },
  {
    caseKey: 'ablative',
    cells: {
      sg: { text: '-ās', rowSpan: 2 },
      du: { text: '', rowSpan: 0 },
      pl: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'genitive',
    cells: {
      sg: { text: '', rowSpan: 0 },
      du: { text: '-os', rowSpan: 2 },
      pl: { text: '-ām' },
    },
  },
  {
    caseKey: 'locative',
    cells: {
      sg: { text: '-ām' },
      du: { text: '', rowSpan: 0 },
      pl: { text: '-su' },
    },
  },
  {
    caseKey: 'vocative',
    cells: {
      sg: { text: '-e' },
      du: { text: '', isIdentity: true, identityDuPl: true },
      pl: { text: '', isIdentity: true, identityDuPl: true },
    },
  },
];

export const aaStemEndings: EndingsTableData = {
  titleKey: 'endings.titleAAStem',
  columns: AA_STEM_COLS,
  rows: AA_STEM_ROWS,
};

// ============================================================================
// -i and -u stems (masculine & feminine, distinct columns; neuter separate)
// ============================================================================

const I_U_STEM_COLS: ColumnDescriptor[] = [
  { key: 'sgM', label: 'sg. m' },
  { key: 'sgF', label: 'sg. f' },
  { key: 'sgN', label: 'sg. n' },
  { key: 'duMF', label: 'du. (m/f)' },
  { key: 'duN', label: 'du. n' },
  { key: 'plM', label: 'pl. m' },
  { key: 'plF', label: 'pl. f' },
  { key: 'plN', label: 'pl. n' },
];

const I_U_STEM_ROWS: EndingsRow[] = [
  {
    caseKey: 'nominative',
    cells: {
      sgM: { text: '-s' }, sgF: { text: '-s' }, sgN: { text: '', isIdentity: true },
      duMF: { text: '-ī/-ū', rowSpan: 2 }, duN: { text: '-nī', rowSpan: 2 },
      plM: { text: '-as' }, plF: { text: '-as' }, plN: { text: '-īni/-ūni', rowSpan: 2 },
    },
  },
  {
    caseKey: 'accusative',
    cells: {
      sgM: { text: '-m' }, sgF: { text: '-m' }, sgN: { text: '-m' },
      duMF: { text: '', rowSpan: 0 }, duN: { text: '', rowSpan: 0 },
      plM: { text: '-īn/-ūn' }, plF: { text: '-īs/-ūs' }, plN: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'instrumental',
    cells: {
      sgM: { text: '-nā' }, sgF: { text: '-ā' }, sgN: { text: '-nā' },
      duMF: { text: '-bhyām', rowSpan: 3 }, duN: { text: '-bhyām', rowSpan: 3 },
      plM: { text: '-bhis' }, plF: { text: '-bhis' }, plN: { text: '-bhis' },
    },
  },
  {
    caseKey: 'dative',
    cells: {
      sgM: { text: '-e' }, sgF: { text: '-e' }, sgN: { text: '-ne' },
      duMF: { text: '', rowSpan: 0 }, duN: { text: '', rowSpan: 0 },
      plM: { text: '-bhyas', rowSpan: 2 }, plF: { text: '-bhyas', rowSpan: 2 }, plN: { text: '-bhyas', rowSpan: 2 },
    },
  },
  {
    caseKey: 'ablative',
    cells: {
      sgM: { text: '-s', rowSpan: 2 }, sgF: { text: '-s', rowSpan: 2 }, sgN: { text: '-nas', rowSpan: 2 },
      duMF: { text: '', rowSpan: 0 }, duN: { text: '', rowSpan: 0 },
      plM: { text: '', rowSpan: 0 }, plF: { text: '', rowSpan: 0 }, plN: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'genitive',
    cells: {
      sgM: { text: '', rowSpan: 0 }, sgF: { text: '', rowSpan: 0 }, sgN: { text: '', rowSpan: 0 },
      duMF: { text: '-os', rowSpan: 2 }, duN: { text: '-nos', rowSpan: 2 },
      plM: { text: '-īnām/-ūnām' }, plF: { text: '-īnām/-ūnām' }, plN: { text: '-īnām/-ūnām' },
    },
  },
  {
    caseKey: 'locative',
    cells: {
      sgM: { text: '-au' }, sgF: { text: '-au' }, sgN: { text: '-ni' },
      duMF: { text: '', rowSpan: 0 }, duN: { text: '', rowSpan: 0 },
      plM: { text: '-ṣu' }, plF: { text: '-ṣu' }, plN: { text: '-ṣu' },
    },
  },
  {
    caseKey: 'vocative',
    cells: {
      sgM: { text: '—' }, sgF: { text: '—' }, sgN: { text: '', isIdentity: true },
      duMF: { text: '', isIdentity: true, identityDuPl: true }, duN: { text: '', isIdentity: true, identityDuPl: true },
      plM: { text: '', isIdentity: true, identityDuPl: true }, plF: { text: '', isIdentity: true, identityDuPl: true }, plN: { text: '', isIdentity: true, identityDuPl: true },
    },
  },
];

export const iuStemEndings: EndingsTableData = {
  titleKey: 'endings.titleIUStem',
  columns: I_U_STEM_COLS,
  rows: I_U_STEM_ROWS,
};

// ============================================================================
// -ī and -ū stems (feminine only; no gender split, only vowel-length columns)
// ============================================================================

const II_UU_STEM_COLS: ColumnDescriptor[] = [
  { key: 'sgI', label: 'sg. -ī' },
  { key: 'sgU', label: 'sg. -ū' },
  { key: 'du', label: 'du.' },
  { key: 'pl', label: 'pl.' },
];

const II_UU_STEM_ROWS: EndingsRow[] = [
  {
    caseKey: 'nominative',
    cells: {
      sgI: { text: '—' }, sgU: { text: '-s' },
      du: { text: '-au', rowSpan: 2 }, pl: { text: '-as' },
    },
  },
  {
    caseKey: 'accusative',
    cells: {
      sgI: { text: '-m' }, sgU: { text: '-m' },
      du: { text: '', rowSpan: 0 }, pl: { text: '-īs/-ūs' },
    },
  },
  {
    caseKey: 'instrumental',
    cells: {
      sgI: { text: '-ā' }, sgU: { text: '-ā' },
      du: { text: '-bhyām', rowSpan: 3 }, pl: { text: '-bhis' },
    },
  },
  {
    caseKey: 'dative',
    cells: {
      sgI: { text: '-āi' }, sgU: { text: '-āi' },
      du: { text: '', rowSpan: 0 }, pl: { text: '-bhyas', rowSpan: 2 },
    },
  },
  {
    caseKey: 'ablative',
    cells: {
      sgI: { text: '-ās', rowSpan: 2 }, sgU: { text: '-ās', rowSpan: 2 },
      du: { text: '', rowSpan: 0 }, pl: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'genitive',
    cells: {
      sgI: { text: '', rowSpan: 0 }, sgU: { text: '', rowSpan: 0 },
      du: { text: '-os', rowSpan: 2 }, pl: { text: '(-n)-ām' },
    },
  },
  {
    caseKey: 'locative',
    cells: {
      sgI: { text: '-ām' }, sgU: { text: '-ām' },
      du: { text: '', rowSpan: 0 }, pl: { text: '-ṣu' },
    },
  },
  {
    caseKey: 'vocative',
    cells: {
      sgI: { text: '-i' }, sgU: { text: '-u' },
      du: { text: '', isIdentity: true, identityDuPl: true }, pl: { text: '', isIdentity: true, identityDuPl: true },
    },
  },
];

export const iiUuStemEndings: EndingsTableData = {
  titleKey: 'endings.titleIIUuStem',
  columns: II_UU_STEM_COLS,
  rows: II_UU_STEM_ROWS,
};

// ============================================================================
// -ṛ stems (masculine / feminine / neuter) — guṇa-grade names of relation
// ============================================================================

const R_STEM_COLS: ColumnDescriptor[] = [
  { key: 'sgMF', label: 'sg. (m/f)' },
  { key: 'sgN',  label: 'sg. n' },
  { key: 'duMF', label: 'du. (m/f)' },
  { key: 'duN',  label: 'du. n' },
  { key: 'plMF', label: 'pl. (m/f)' },
  { key: 'plN',  label: 'pl. n' },
];

const R_STEM_ROWS: EndingsRow[] = [
  {
    caseKey: 'nominative',
    cells: {
      sgMF: { text: '\u0101/', grade: 'strong' }, sgN: { text: '\u1e5b/', grade: 'zero' },
      duMF: { text: 'au', grade: 'strong', rowSpan: 2 }, duN: { text: '\u1e5b\u1e47/\u012b', grade: 'zero' },
      plMF: { text: 'as', grade: 'strong' }, plN: { text: '\u1e5b\u1e47/\u012b', grade: 'zero' },
    },
  },
  {
    caseKey: 'accusative',
    cells: {
      sgMF: { text: 'am', grade: 'strong' }, sgN: { text: '\u1e5b/', grade: 'zero' },
      duMF: { text: '', rowSpan: 0 }, duN: { text: '\u1e5b\u1e47/\u012b', grade: 'zero' },
      plMF: { text: '\u1e5dn / \u1e5d\u1e63', grade: 'zero' }, plN: { text: '\u1e5b\u1e47/\u012b', grade: 'zero' },
    },
  },
  {
    caseKey: 'instrumental',
    cells: {
      sgMF: { text: '\u0101', grade: 'zero' }, sgN: { text: '\u1e5b\u1e47/\u0101', grade: 'zero' },
      duMF: { text: 't\u1e5b/bhy\u0101m', grade: 'zero', rowSpan: 3 }, duN: { text: 't\u1e5b/bhy\u0101m', grade: 'zero', rowSpan: 3 },
      plMF: { text: '\u1e5b/bhis', grade: 'zero' }, plN: { text: '\u1e5b/bhis', grade: 'zero' },
    },
  },
  {
    caseKey: 'dative',
    cells: {
      sgMF: { text: 'e', grade: 'zero' }, sgN: { text: '\u1e5b\u1e47/e', grade: 'zero' },
      duMF: { text: '', rowSpan: 0 }, duN: { text: '', rowSpan: 0 },
      plMF: { text: '\u1e5b/bhyas', grade: 'zero', rowSpan: 2 }, plN: { text: '\u1e5b/bhyas', grade: 'zero', rowSpan: 2 },
    },
  },
  {
    caseKey: 'ablative',
    cells: {
      sgMF: { text: '/ur', rowSpan: 2 }, sgN: { text: '\u1e5b\u1e47/as', grade: 'zero' },
      duMF: { text: '', rowSpan: 0 }, duN: { text: '', rowSpan: 0 },
      plMF: { text: '', rowSpan: 0 }, plN: { text: '', rowSpan: 0 },
    },
  },
  {
    caseKey: 'genitive',
    cells: {
      sgMF: { text: '', rowSpan: 0 }, sgN: { text: '\u1e5b\u1e47/as', grade: 'zero' },
      duMF: { text: 'tr/os', grade: 'zero' }, duN: { text: '\u1e5b\u1e47/os', grade: 'zero' },
      plMF: { text: '\u1e5d\u1e47/\u0101m', grade: 'zero' }, plN: { text: '\u1e5d\u1e47/\u0101m', grade: 'zero' },
    },
  },
  {
    caseKey: 'locative',
    cells: {
      sgMF: { text: 'i' }, sgN: { text: '\u1e5b\u1e47/i', grade: 'zero' },
      duMF: { text: 'tr/os', grade: 'zero' }, duN: { text: '\u1e5b\u1e47/os', grade: 'zero' },
      plMF: { text: '\u1e63u', grade: 'zero' }, plN: { text: '\u1e63u', grade: 'zero' },
    },
  },
  {
    caseKey: 'vocative',
    cells: {
      sgMF: { text: 'ar/' }, sgN: { text: '\u1e5b/', grade: 'zero' },
      duMF: { text: '', isIdentity: true, identityDuPl: true, colSpan: 2 }, duN: { text: '', colSpan: 0 },
      plMF: { text: '', isIdentity: true, identityDuPl: true, colSpan: 2 }, plN: { text: '', colSpan: 0 },
    },
  },
];

export const rStemEndings: EndingsTableData = {
  titleKey: 'endings.titleRStem',
  columns: R_STEM_COLS,
  rows: R_STEM_ROWS,
};

// ============================================================================
// Map: VowelType → EndingsTableData
// ============================================================================

export { type VowelType } from '../types/content-dtos';

export const vowelTypeToEndingsTable: Partial<Record<string, EndingsTableData>> = {
  A_STEM: aStemEndings,
  AA_STEM: aaStemEndings,
  I_STEM: iuStemEndings,
  U_STEM: iuStemEndings,
  II_STEM: iiUuStemEndings,
  UU_STEM: iiUuStemEndings,
  R_STEM: rStemEndings,
};
