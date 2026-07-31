import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { MiniProgressBar } from '../common/MiniProgressBar';
import type { VerseWordMorphologyDto, VerseWordDerivationDto } from '../../types/sangraha';
import type { VocabularyWordProgress } from '../../types/lesson';

// ── Label maps ──

const POS_MAP: Record<string, string> = {
  NOUN: 'n.', VERB: 'v.', ADJECTIVE: 'adj.', PRONOUN: 'pron.',
  ADVERB: 'adv.', NUMERAL: 'num.', INDECLINABLE: 'indecl.',
  PARTICLE: 'part.', CONJUNCTION: 'conj.', INTERJECTION: 'interj.', OTHER: 'other',
};

const GENDER_MAP: Record<string, string> = { MASCULINE: 'm.', FEMININE: 'f.', NEUTER: 'n.' };

const CASE_MAP: Record<string, string> = {
  NOMINATIVE: 'nom.', ACCUSATIVE: 'acc.', INSTRUMENTAL: 'instr.',
  DATIVE: 'dat.', ABLATIVE: 'abl.', GENITIVE: 'gen.',
  LOCATIVE: 'loc.', VOCATIVE: 'voc.',
};

const NUMBER_MAP: Record<string, string> = { SINGULAR: 'sg.', DUAL: 'du.', PLURAL: 'pl.' };
const PERSON_MAP: Record<string, string> = { FIRST: '1', SECOND: '2', THIRD: '3' };

const TENSE_MAP: Record<string, string> = {
  PRESENT: 'pres.', IMPERFECT: 'impf.', FUTURE: 'fut.',
  PERFECT: 'perf.', AORIST: 'aor.', CONDITIONAL: 'cond.', BENEDICTIVE: 'bened.',
};

const MOOD_MAP: Record<string, string> = {
  INDICATIVE: 'ind.', IMPERATIVE: 'imp.', OPTATIVE: 'opt.',
  CONDITIONAL: 'cond.', SUBJUNCTIVE: 'subj.',
};

const VOICE_MAP: Record<string, string> = { ACTIVE: 'act.', MIDDLE: 'mid.', PASSIVE: 'pass.' };

const FORM_TYPE_MAP: Record<string, string> = {
  FINITE: 'fin.', INFINITIVE: 'inf.', ABSOLUTIVE: 'abs.',
  PARTICIPLE: 'pct.', GERUNDIVE: 'ger.', OTHER_NONFINITE: 'non-fin.',
  NOMINAL: 'nom.', ADJECTIVAL: 'adj.', PRONOMINAL: 'pron.', INDECLINABLE: 'indecl.',
};

const DERIV_TYPE_MAP: Record<string, string> = {
  SIMPLE_INFLECTION: 'infl.', ABSOLUTIVE: 'abs.', PARTICIPLE: 'pct.',
  GERUNDIVE: 'ger.', INFINITIVE: 'inf.', CAUSATIVE: 'caus.',
  DESIDERATIVE: 'des.', DENOMINATIVE: 'denom.', COMPOUND_VERB: 'comp.',
  OTHER: 'other',
};

const mapEnum = (map: Record<string, string>, value: string | null | undefined): string | null => {
  if (value == null) return null;
  return map[value] ?? value;
};

// ── Word type matching new API ──

interface Word {
  id: string;
  position?: number;
  surfaceIast: string;
  surfaceDevanagari?: string;
  lemmaIast?: string;
  stem?: string | null;
  root?: string | null;
  pos?: string | null;
  formType?: string | null;
  isFinite?: boolean | null;
  morphology?: VerseWordMorphologyDto | null;
  derivation?: VerseWordDerivationDto | null;
  lemmaGlossRu?: string | null;
  lemmaGlossEn?: string | null;
  contextGlossRu?: string;
  contextGlossEn?: string;
  formationRuleNumbers?: number[];
  analysisConfidence?: string | null;
  ambiguityNotes?: string | null;
  vocabularyWordId?: string | null;
}

interface VerseWordsListProps {
  words: Word[];
  headerActions?: React.ReactNode;
  wordProgressMap?: Record<string, VocabularyWordProgress> | null;
}

// ── Helper: render non-null fields from a record ──

function nonNullEntries<T extends Record<string, string | null | undefined>>(
  map: T,
  prefix: Record<string, string>,
  suffix?: Record<string, string>,
): string[] {
  const out: string[] = [];
  for (const [key, label] of Object.entries(prefix)) {
    const val = map[key];
    if (val != null) {
      const suffixStr = suffix?.[key] ?? '';
      out.push(`${label}${val}${suffixStr}`);
    }
  }
  return out;
}

// ── Component ──

const VerseWordsList = ({ words, headerActions, wordProgressMap }: VerseWordsListProps) => {
  const { t, i18n } = useTranslation();
  const [expandedId, setExpandedId] = useState<string | null>(null);

  if (!words || words.length === 0) return null;

  const allFormationRuleNumbers = words
    .flatMap(w => w.formationRuleNumbers ?? [])
    .filter((v, i, a) => a.indexOf(v) === i);

  const showProgress = wordProgressMap != null;
  const lang = i18n.language;

  const toggleExpand = (id: string) => {
    setExpandedId(prev => (prev === id ? null : id));
  };

  const renderMorphologyChips = (m: VerseWordMorphologyDto) => {
    const items: string[] = [];
    if (m.caseType) items.push(mapEnum(CASE_MAP, m.caseType) ?? m.caseType);
    if (m.gender) items.push(mapEnum(GENDER_MAP, m.gender) ?? m.gender);
    if (m.numberType) items.push(mapEnum(NUMBER_MAP, m.numberType) ?? m.numberType);
    if (m.person) items.push(mapEnum(PERSON_MAP, m.person) ?? m.person);
    if (m.tense) items.push(mapEnum(TENSE_MAP, m.tense) ?? m.tense);
    if (m.mood) items.push(mapEnum(MOOD_MAP, m.mood) ?? m.mood);
    if (m.voice) items.push(mapEnum(VOICE_MAP, m.voice) ?? m.voice);
    return items;
  };

  return (
    <div className="mb-4">
      <label className="flex justify-content-between align-items-center mb-1 font-semibold">
        <span>{t('sangraha.fields.words')}</span>
        <span className="flex align-items-center gap-2">
          {headerActions}
          {allFormationRuleNumbers.length > 0 && (
            <Link
              to={`/grammar/emeneau-rules?${allFormationRuleNumbers.map(r => `rule=${r}`).join('&')}`}
              className="text-sm text-primary"
              target="_blank"
              rel="noopener noreferrer"
            >
              {allFormationRuleNumbers.join(', ')}
            </Link>
          )}
        </span>
      </label>
      <div className="p-3 border-1 border-round surface-border surface-ground">
        {words.map((w) => {
          const progress = showProgress && w.vocabularyWordId
            ? wordProgressMap[w.vocabularyWordId]
            : undefined;
          const isExpanded = expandedId === w.id;

          // Short gloss for row (use contextGlossRu/En)
          const gloss = lang === 'ru'
            ? (w.contextGlossRu ?? w.contextGlossEn)
            : (w.contextGlossEn ?? w.contextGlossRu);

          // Morphology chips from nested object
          const morphChips = w.morphology ? renderMorphologyChips(w.morphology) : [];

          return (
            <div key={w.id}>
              {/* ── Main row ── */}
              <div
                className="flex align-items-center gap-2 mb-1 flex-wrap cursor-pointer hover:surface-hover border-round p-1"
                onClick={() => toggleExpand(w.id)}
              >
                <i className={`pi text-xs text-color-secondary ${isExpanded ? 'pi-chevron-down' : 'pi-chevron-right'}`} />

                <Link
                  to={`/dictionary?q=${encodeURIComponent(w.lemmaIast || w.stem || w.surfaceIast)}`}
                  className="font-medium hover:underline"
                  style={{ color: 'inherit' }}
                  target="_blank"
                  rel="noopener noreferrer"
                  onClick={(e) => e.stopPropagation()}
                >
                  {w.surfaceIast}
                </Link>

                {w.surfaceDevanagari && (
                  <span className="text-color-secondary font-italic">{w.surfaceDevanagari}</span>
                )}

                <span className="text-color-secondary">
                  ({mapEnum(POS_MAP, w.pos) || '-'}
                  {w.formType ? ` / ${mapEnum(FORM_TYPE_MAP, w.formType) || w.formType}` : ''})
                </span>

                {morphChips.length > 0 && (
                  <span className="text-sm text-color-secondary">
                    {morphChips.join(', ')}
                  </span>
                )}

                {gloss && <span className="text-sm">— {gloss}</span>}

                {w.formationRuleNumbers && w.formationRuleNumbers.length > 0 && (
                  <span className="text-sm text-color-secondary">
                    [{w.formationRuleNumbers.join(', ')}]
                  </span>
                )}
    
                {/* Progress column */}
                {showProgress && (
                  <div className="ml-auto" style={{ minWidth: '80px', maxWidth: '100px' }}>
                    {progress ? (
                      <MiniProgressBar
                        value={progress.score ?? 0}
                        status={progress.status}
                        showValue={false}
                        height="8px"
                      />
                    ) : (
                      <span className="text-xs text-color-secondary">—</span>
                    )}
                  </div>
                )}
              </div>

              {/* ── Expanded detail panel ── */}
              {isExpanded && (
                <div className="ml-4 mb-2 p-3 border-1 border-round surface-border bg-white">
                  {/* Surface */}
                  <div className="mb-2">
                    <span className="font-semibold text-sm">{t('sangraha.fields.surfaceForm')}: </span>
                    <span className="text-sm">{w.surfaceDevanagari}</span>
                    <span className="text-sm text-color-secondary ml-2">({w.surfaceIast})</span>
                  </div>

                  {/* Lexical */}
                  <div className="flex flex-wrap gap-3 mb-2 text-sm">
                    {w.lemmaIast && (
                      <div>
                        <span className="font-semibold">{t('sangraha.fields.lemma')}: </span>
                        {w.lemmaIast}
                      </div>
                    )}
                    {w.root && (
                      <div>
                        <span className="font-semibold">{t('sangraha.fields.root')}: </span>
                        √{w.root}
                      </div>
                    )}
                    {w.stem && (
                      <div>
                        <span className="font-semibold">{t('sangraha.fields.stem')}: </span>
                        {w.stem}
                      </div>
                    )}
                    <div>
                      <span className="font-semibold">POS: </span>
                      {w.pos || '-'}
                    </div>
                    <div>
                      <span className="font-semibold">{t('sangraha.fields.formType')}: </span>
                      {w.formType || '-'}
                      {w.isFinite !== null && w.isFinite !== undefined && (
                        <span className="text-color-secondary ml-1">
                          ({w.isFinite ? t('sangraha.fields.finite') : t('sangraha.fields.nonFinite')})
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Morphology detail */}
                  {w.morphology && morphChips.length > 0 && (
                    <div className="mb-2 text-sm">
                      <span className="font-semibold">{t('sangraha.fields.morphology')}: </span>
                      <span className="text-color-secondary">{morphChips.join(', ')}</span>
                    </div>
                  )}

                  {/* Derivation detail */}
                  {w.derivation && (
                    <div className="mb-2 text-sm">
                      <span className="font-semibold">{t('sangraha.fields.derivation')}: </span>
                      {w.derivation.derivationType && (
                        <span>{mapEnum(DERIV_TYPE_MAP, w.derivation.derivationType) || w.derivation.derivationType} </span>
                      )}
                      {w.derivation.derivationalSuffix && (
                        <span className="text-color-secondary">
                          {t('sangraha.fields.suffix')}: {w.derivation.derivationalSuffix}{' '}
                        </span>
                      )}
                      {w.derivation.derivationalBase && (
                        <span className="text-color-secondary">
                          {t('sangraha.fields.base')}: {w.derivation.derivationalBase}{' '}
                        </span>
                      )}
                      {w.derivation.description && (
                        <span className="text-color-secondary">({w.derivation.description})</span>
                      )}
                    </div>
                  )}

                  {/* Glosses: dictionary vs contextual */}
                  <div className="flex flex-wrap gap-3 mb-2 text-sm">
                    <div>
                      <span className="font-semibold">{t('sangraha.fields.lemmaGloss')}: </span>
                      <span className="text-color-secondary">
                        {lang === 'ru' ? (w.lemmaGlossRu || '-') : (w.lemmaGlossEn || '-')}
                      </span>
                    </div>
                    <div>
                      <span className="font-semibold">{t('sangraha.fields.contextGloss')}: </span>
                      <span>{gloss || '-'}</span>
                    </div>
                  </div>

                  {/* Formation rules */}
                  {w.formationRuleNumbers && w.formationRuleNumbers.length > 0 && (
                    <div className="text-sm text-color-secondary">
                      {t('sangraha.fields.formationRules')}: [{w.formationRuleNumbers.join(', ')}]
                    </div>
                  )}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default VerseWordsList;
