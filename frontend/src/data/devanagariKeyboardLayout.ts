/**
 * Devanagari virtual keyboard layout for react-simple-keyboard.
 *
 * Each row is a space-separated string of button labels.
 * Patterned after Lexilogos Sanskrit keyboard UX:
 *   33 consonants, independent vowels, anusvara, visarga,
 *   explicit virama (्), avagraha, danda, digits.
 */

/** "default" layout rows for react-simple-keyboard. */
export const devanagariLayout: { default: string[] } = {
  default: [
  // Row 1 — independent vowels
    'अ आ इ ई उ ऊ ऋ ॠ ऌ ए ऐ ओ औ',
  // Row 2 — velar + palatal consonants
    'क ख ग घ ङ च छ ज झ ञ',
  // Row 3 — retroflex + dental consonants
    'ट ठ ड ढ ण त थ द ध न',
    // Row 4 — labial + semivowels
    'प फ ब भ म य र ल व',
  // Row 5 — sibilants + ha + diacritics
    'श ष स ह ं ः ् ऽ । ॥',
    // Row 6 — digits
    '० १ २ ३ ४ ५ ६ ७ ८ ९',
    ],
};

