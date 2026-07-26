import React, { useCallback } from 'react';
import { Tooltip } from 'primereact/tooltip';
import './DevanagariKeyboard.css';
import type { KeyDef } from '../../data/devanagariKeyboardLayout';

// ── Flat key data (no sections, no labels) ───────────────────────────

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

const CONSONANTS: KeyDef[] = [
  { devanagari: 'क', iast: 'ka' },
  { devanagari: 'ख', iast: 'kha' },
  { devanagari: 'ग', iast: 'ga' },
  { devanagari: 'घ', iast: 'gha' },
  { devanagari: 'ङ', iast: 'ṅa' },
  { devanagari: 'च', iast: 'ca' },
  { devanagari: 'छ', iast: 'cha' },
  { devanagari: 'ज', iast: 'ja' },
  { devanagari: 'झ', iast: 'jha' },
  { devanagari: 'ञ', iast: 'ña' },
  { devanagari: 'ट', iast: 'ṭa' },
  { devanagari: 'ठ', iast: 'ṭha' },
  { devanagari: 'ड', iast: 'ḍa' },
  { devanagari: 'ढ', iast: 'ḍha' },
  { devanagari: 'ण', iast: 'ṇa' },
  { devanagari: 'त', iast: 'ta' },
  { devanagari: 'थ', iast: 'tha' },
  { devanagari: 'द', iast: 'da' },
  { devanagari: 'ध', iast: 'dha' },
  { devanagari: 'न', iast: 'na' },
  { devanagari: 'प', iast: 'pa' },
  { devanagari: 'फ', iast: 'pha' },
  { devanagari: 'ब', iast: 'ba' },
  { devanagari: 'भ', iast: 'bha' },
  { devanagari: 'म', iast: 'ma' },
];

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

const SPECIALS: KeyDef[] = [
  { devanagari: 'ं', iast: 'ṃ (anusvāra)' },
  { devanagari: 'ः', iast: 'ḥ (visarga)' },
  { devanagari: 'ँ', iast: 'm̐ (candrabindu)' },
  { devanagari: '्', iast: 'virāma', className: 'kbd-key-virama' },
  { devanagari: 'ऽ', iast: '\' (avagraha)' },
];

const PUNCTUATION: KeyDef[] = [
  { devanagari: '।', iast: 'daṇḍa' },
  { devanagari: '॥', iast: 'double daṇḍa' },
];

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

const CONSONANT_GAPS = [5, 10, 15, 20]; // after each varga

// ── Helpers ──────────────────────────────────────────────────────────

function renderKeys(
  keys: KeyDef[],
  onKeyClick: (g: string) => void,
  gapAfter: number[] = [],
) {
  return keys.map((k, i) => (
    <React.Fragment key={k.devanagari}>
      <button
        className={`kbd-key${k.className ? ` ${k.className}` : ''}`}
        data-pr-tooltip={k.iast}
        onClick={() => onKeyClick(k.devanagari)}
        type="button"
      >
        {k.devanagari}
      </button>
      {gapAfter.includes(i + 1) && <span className="kbd-gap" />}
    </React.Fragment>
  ));
}

// ── Component ────────────────────────────────────────────────────────

interface DevanagariKeyboardProps {
  inputRef: React.RefObject<HTMLTextAreaElement | null>;
  onTextChange: (text: string) => void;
}

const DevanagariKeyboard: React.FC<DevanagariKeyboardProps> = ({
  inputRef,
  onTextChange,
}) => {
  const handleKeyClick = useCallback(
    (glyph: string) => {
      const textarea = inputRef.current;
      if (!textarea) return;
      const start = textarea.selectionStart ?? 0;
      const end = textarea.selectionEnd ?? 0;
      const current = textarea.value;
      const newText = current.slice(0, start) + glyph + current.slice(end);
      onTextChange(newText);
      setTimeout(() => {
        textarea.focus();
        const newPos = start + glyph.length;
        textarea.setSelectionRange(newPos, newPos);
      }, 0);
    },
    [inputRef, onTextChange],
  );

  return (
    <div className="devanagari-kbd mt-3">
      <Tooltip target=".kbd-key" mouseTrack position="top" />

      <div className="grid">
        {/* ── Left column: Vowels + Consonants ── */}
        <div className="col-12 md:col-6">
          <div className="kbd-row">{renderKeys(VOWELS, handleKeyClick)}</div>
          <div className="kbd-row">
            {renderKeys(CONSONANTS, handleKeyClick, CONSONANT_GAPS)}
          </div>
        </div>

        {/* ── Right column: everything else ── */}
        <div className="col-12 md:col-6">
          <div className="kbd-row">{renderKeys(SEMIVOWELS_SIBILANTS, handleKeyClick)}</div>
          <div className="kbd-row">{renderKeys(SPECIALS, handleKeyClick)}</div>
          <div className="kbd-row">
            {renderKeys(PUNCTUATION, handleKeyClick)}
            <span className="kbd-gap" />
            {renderKeys(DIGITS, handleKeyClick)}
          </div>
        </div>
      </div>
    </div>
  );
};

export default DevanagariKeyboard;

