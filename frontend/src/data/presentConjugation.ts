/**
 * Static reference data: present-tense endings of Sanskrit verbs
 * (parasmaipada & ātmanepada).
 *
 * Source: etcetera/grammar/sanskrit_present_endings.json (copied verbatim into
 * presentConjugation.json). The present-tense paradigms (example sentences)
 * come from the DB endpoint /conjugation-paradigms — not from this file.
 */

import data from './presentConjugation.json';

export interface SriEndingCell {
  sanskrit: string;
  transliteration: string;
}

export interface SriEndingRow {
  person: number;
  person_name: string;
  singular: SriEndingCell;
  dual: SriEndingCell;
  plural: SriEndingCell;
}

export type SriVoice = 'parasmaipada' | 'atmanepada';

export type SriNumeral = 'singular' | 'dual' | 'plural';

/** Present endings of parasmaipada / ātmanepada by person row. */
export const PRESENT_ENDINGS: Record<SriVoice, SriEndingRow[]> =
  data.endings as Record<SriVoice, SriEndingRow[]>;

/** Persons present in the endings data, stable order (3 → 2 → 1). */
export const PRESENT_PERSONS: number[] = [3, 2, 1];

export const PRESENT_NUMBERS: SriNumeral[] = ['singular', 'dual', 'plural'];

export const findEndingCell = (
  row: SriEndingRow,
  number: SriNumeral,
): SriEndingCell => row[number];