/**
 * Devanagari virtual keyboard layout — structured by phonetic sections.
 *
 * Each section has an i18n titleKey and an array of keys, each with
 * its Devanagari glyph and IAST transliteration (for tooltip).
 */

export interface KeyDef {
  /** Unicode Devanagari glyph inserted on click */
  devanagari: string;
  /** IAST transliteration shown in tooltip */
  iast: string;
  /** Optional CSS class for special styling (e.g. virāma highlight) */
  className?: string;
}

export interface VargaDef {
  /** i18n key for the varga label (e.g. 'keyboard.vargaVelar') */
  titleKey: string;
  keys: KeyDef[];
}

export interface KeyboardSection {
  /** i18n key for the section heading */
  titleKey: string;
  /** If true, section can be collapsed (Accordion) */
  collapsible?: boolean;
  /** If collapsible, start collapsed? */
  collapsedByDefault?: boolean;
  /** Either flat keys or varga-grouped consonant rows */
  keys?: KeyDef[];
  vargas?: VargaDef[];
}

/** Independent vowels */
const VOWELS: KeyDef[] = [
  { devanagari: 'अ', iast: 'a' },
  { devanagari: 'आ', iast: 'ā' },
  { devanagari: 'इ', iast: 'i' },
  { devanagari: 'ई', iast: 'ī' },
  { devanagari: 'उ', iast: 'u' },
  { devanagari: 'ऊ', iast: 'ū' },
  { devanagari: 'ऋ', iast: 'ṛ' },
  { devanagari: 'ॠ', iast: 'ṝ' },
  { devanagari: 'ऌ', iast: 'ḷ' },
  { devanagari: 'ए', iast: 'e' },
  { devanagari: 'ऐ', iast: 'ai' },
  { devanagari: 'ओ', iast: 'o' },
  { devanagari: 'औ', iast: 'au' },
];

/** Consonants grouped by varga */
const CONSONANT_VARGAS: VargaDef[] = [
  {
    titleKey: 'keyboard.vargaVelar',
    keys: [
      { devanagari: 'क', iast: 'ka' },
      { devanagari: 'ख', iast: 'kha' },
      { devanagari: 'ग', iast: 'ga' },
      { devanagari: 'घ', iast: 'gha' },
      { devanagari: 'ङ', iast: 'ṅa' },
    ],
  },
  {
    titleKey: 'keyboard.vargaPalatal',
    keys: [
      { devanagari: 'च', iast: 'ca' },
      { devanagari: 'छ', iast: 'cha' },
      { devanagari: 'ज', iast: 'ja' },
      { devanagari: 'झ', iast: 'jha' },
      { devanagari: 'ञ', iast: 'ña' },
    ],
  },
  {
    titleKey: 'keyboard.vargaRetroflex',
    keys: [
      { devanagari: 'ट', iast: 'ṭa' },
      { devanagari: 'ठ', iast: 'ṭha' },
      { devanagari: 'ड', iast: 'ḍa' },
      { devanagari: 'ढ', iast: 'ḍha' },
      { devanagari: 'ण', iast: 'ṇa' },
    ],
  },
  {
    titleKey: 'keyboard.vargaDental',
    keys: [
      { devanagari: 'त', iast: 'ta' },
      { devanagari: 'थ', iast: 'tha' },
      { devanagari: 'द', iast: 'da' },
      { devanagari: 'ध', iast: 'dha' },
      { devanagari: 'न', iast: 'na' },
    ],
  },
  {
    titleKey: 'keyboard.vargaLabial',
    keys: [
      { devanagari: 'प', iast: 'pa' },
      { devanagari: 'फ', iast: 'pha' },
      { devanagari: 'ब', iast: 'ba' },
      { devanagari: 'भ', iast: 'bha' },
      { devanagari: 'म', iast: 'ma' },
    ],
  },
];

/** Semivowels + sibilants + ha */
const SEMIVOWELS_SIBILANTS: KeyDef[] = [
  { devanagari: 'य', iast: 'ya' },
  { devanagari: 'र', iast: 'ra' },
  { devanagari: 'ल', iast: 'la' },
  { devanagari: 'व', iast: 'va' },
  { devanagari: 'श', iast: 'śa' },
  { devanagari: 'ष', iast: 'ṣa' },
  { devanagari: 'स', iast: 'sa' },
  { devanagari: 'ह', iast: 'ha' },
];

/** Diacritics and special symbols */
const SPECIAL_SYMBOLS: KeyDef[] = [
  { devanagari: 'ं', iast: 'ṃ (anusvāra)' },
  { devanagari: 'ः', iast: 'ḥ (visarga)' },
  { devanagari: 'ँ', iast: 'm̐ (candrabindu)' },
  { devanagari: '्', iast: 'virāma', className: 'kbd-key-virama' },
  { devanagari: 'ऽ', iast: '\' (avagraha)' },
];

/** Punctuation */
const PUNCTUATION: KeyDef[] = [
  { devanagari: '।', iast: 'daṇḍa' },
  { devanagari: '॥', iast: 'double daṇḍa' },
];

/** Digits (0–9) */
const DIGITS: KeyDef[] = [
  { devanagari: '०', iast: '0' },
  { devanagari: '१', iast: '1' },
  { devanagari: '२', iast: '2' },
  { devanagari: '३', iast: '3' },
  { devanagari: '४', iast: '4' },
  { devanagari: '५', iast: '5' },
  { devanagari: '६', iast: '6' },
  { devanagari: '७', iast: '7' },
  { devanagari: '८', iast: '8' },
  { devanagari: '९', iast: '9' },
];

export const keyboardSections: KeyboardSection[] = [
  {
    titleKey: 'keyboard.sectionVowels',
    keys: VOWELS,
  },
  {
    titleKey: 'keyboard.sectionConsonants',
    vargas: CONSONANT_VARGAS,
  },
  {
    titleKey: 'keyboard.sectionSemivowels',
    keys: SEMIVOWELS_SIBILANTS,
  },
  {
    titleKey: 'keyboard.sectionSpecials',
    keys: SPECIAL_SYMBOLS,
  },
  {
    titleKey: 'keyboard.sectionPunctuation',
    keys: PUNCTUATION,
  },
  {
    titleKey: 'keyboard.sectionDigits',
    keys: DIGITS,
    collapsible: true,
    collapsedByDefault: true,
  },
];
