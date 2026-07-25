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
  /** Number of rows this cell spans vertically (rowSpan). 0 = skip (covered by previous row). */
  rowSpan?: number;
  /** If true, the cell is an identity marker ("= stem" or "= N.") resolved via i18n. */
  isIdentity?: boolean;
  /** If true AND isIdentity, use dual/plural identity label ("= N.") instead of singular ("= stem"). */
  identityDuPl?: boolean;
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
      plM: { text: '-as' },
      plN: { text: '-āni', rowSpan: 2 },
    },
  },
  {
    caseKey: 'accusative',
    cells: {
      sgM: { text: '-m' },
      sgN: { text: '-m' },
      duMN: { text: '-e' },
      plM: { text: '-an' },
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
      sgM: { text: '-ād' },
      sgN: { text: '-ād' },
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
      sgM: { text: '-e/' },
      sgN: { text: '-e/' },
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
      du: { text: '-e/', rowSpan: 2 },
      pl: { text: '-s', rowSpan: 2 },
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
      sg: { text: '-e/' },
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
// Map: VowelType → EndingsTableData
// ============================================================================

export { type VowelType } from '../types/content-dtos';

export const vowelTypeToEndingsTable: Partial<Record<string, EndingsTableData>> = {
  A_STEM: aStemEndings,
  AA_STEM: aaStemEndings,
};
