/**
 * Static reference data: imperfect-tense endings of Sanskrit verbs
 * (parasmaipada & ātmanepada).
 *
 * Source: etcetera/grammar/sanskrit_imperfect_endings.json (copied verbatim into
 * imperfectConjugation.json). The imperfect paradigms (example sentences) come
 * from the DB endpoint /conjugation-paradigms — not from this file.
 */

import data from './imperfectConjugation.json';
import type { SriEndingRow, SriVoice } from './presentConjugation';

/** Imperfect endings of parasmaipada / ātmanepada by person row. */
export const IMPERFECT_ENDINGS: Record<SriVoice, SriEndingRow[]> =
  data.endings as Record<SriVoice, SriEndingRow[]>;