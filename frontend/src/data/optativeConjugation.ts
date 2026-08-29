/**
 * Static reference data: optative endings of Sanskrit verbs
 * (parasmaipada & ātmanepada).
 *
 * Source: etcetera/grammar/sanskrit_verbs_optative.json (endings section,
 * copied verbatim into optativeConjugation.json). The optative paradigms
 * (example sentences) come from the DB endpoint /conjugation-paradigms — not
 * from this file.
 */

import data from './optativeConjugation.json';
import type { SriEndingRow, SriVoice } from './presentConjugation';

/** Optative endings of parasmaipada / ātmanepada by person row. */
export const OPTATIVE_ENDINGS: Record<SriVoice, SriEndingRow[]> =
  data.endings as Record<SriVoice, SriEndingRow[]>;