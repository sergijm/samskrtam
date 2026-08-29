/**
 * Static reference data: imperative endings of Sanskrit verbs
 * (parasmaipada & ātmanepada).
 *
 * Source: etcetera/grammar/sanskrit_verbs_imperative.json (endings section,
 * copied verbatim into imperativeConjugation.json). The imperative paradigms
 * (example sentences) come from the DB endpoint /conjugation-paradigms — not
 * from this file.
 */

import data from './imperativeConjugation.json';
import type { SriEndingRow, SriVoice } from './presentConjugation';

/** Imperative endings of parasmaipada / ātmanepada by person row. */
export const IMPERATIVE_ENDINGS: Record<SriVoice, SriEndingRow[]> =
  data.endings as Record<SriVoice, SriEndingRow[]>;