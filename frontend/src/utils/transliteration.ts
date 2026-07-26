/**
 * Transliteration utility: ASCII schemes (HK/ITRANS/SLP1) ⇄ Devanagari + IAST.
 *
 * Uses @indic-transliteration/sanscript (MIT) for all conversion between schemes.
 * Self-contained, reusable — no dependencies on specific page components.
 *
 * ## Build-vs-Buy decision
 * Chosen: **@indic-transliteration/sanscript** v1.3.3 (MIT).
 * Already installed; used on backend (sangraha/dictionary-service); has TS types.
 * Handles conjuncts (kṣ, jñ), virama, anusvara/visarga correctly.
 */

import Sanscript from '@indic-transliteration/sanscript';

/** Supported ASCII input schemes for typing Sanskrit without a special keyboard. */
export type InputScheme = 'hk' | 'itrans' | 'slp1';

/** Convert ASCII input scheme (HK/ITRANS/SLP1) → Devanagari. */
export function asciiToDevanagari(input: string, scheme: InputScheme): string {
  if (!input || input.trim().length === 0) return '';
  try { return Sanscript.t(input, scheme, 'devanagari'); } catch { return input; }
}

/** Convert ASCII input scheme → IAST (human-readable diacritic preview). */
export function asciiToIast(input: string, scheme: InputScheme): string {
  if (!input || input.trim().length === 0) return '';
  try { return Sanscript.t(input, scheme, 'iast'); } catch { return input; }
}

/** Convert Devanagari → IAST. */
export function devanagariToIast(input: string): string {
  if (!input || input.trim().length === 0) return '';
  try { return Sanscript.t(input, 'devanagari', 'iast'); } catch { return input; }
}

/** Convert IAST (diacritic) → Devanagari (for programmatic use). */
export function iastToDevanagari(input: string): string {
  if (!input || input.trim().length === 0) return '';
  try { return Sanscript.t(input, 'iast', 'devanagari'); } catch { return input; }
}

