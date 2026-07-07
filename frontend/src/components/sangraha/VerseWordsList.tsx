import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

// ── Словари грамматических терминов (латинские сокращения, 1–5 букв + точка) ──

const GENDER_MAP: Record<string, string> = {
  MASCULINE: 'm.',
  FEMININE: 'f.',
  NEUTER: 'n.',
};

const CASE_MAP: Record<string, string> = {
  NOMINATIVE: 'nom.',
  ACCUSATIVE: 'acc.',
  INSTRUMENTAL: 'instr.',
  DATIVE: 'dat.',
  ABLATIVE: 'abl.',
  GENITIVE: 'gen.',
  LOCATIVE: 'loc.',
  VOCATIVE: 'voc.',
};

const NUMBER_MAP: Record<string, string> = {
  SINGULAR: 'sg.',
  DUAL: 'du.',
  PLURAL: 'pl.',
};

const PERSON_MAP: Record<string, string> = {
  FIRST: '1',
  SECOND: '2',
  THIRD: '3',
};

const TENSE_MAP: Record<string, string> = {
  PRESENT: 'pres.',
  IMPERFECT: 'impf.',
  FUTURE: 'fut.',
  PERFECT: 'perf.',
  AORIST: 'aor.',
  PLUPERFECT: 'plup.',
  CONDITIONAL: 'cond.',
  BENEDICTIVE: 'bened.',
  FUTURE_PERFECT: 'fut.pf.',
};

const MOOD_MAP: Record<string, string> = {
  INDICATIVE: 'ind.',
  IMPERATIVE: 'imp.',
  OPTATIVE: 'opt.',
  CONDITIONAL: 'cond.',
  SUBJUNCTIVE: 'subj.',
};

const VOICE_MAP: Record<string, string> = {
  ACTIVE: 'act.',
  MIDDLE: 'mid.',
  PASSIVE: 'pass.',
  ATMANEPADA: 'Ātm.',
  PARASMAIPADA: 'Par.',
};

const mapEnum = (map: Record<string, string>, value: string | null | undefined): string | null => {
  if (value == null) return null;
  return map[value] ?? value;
};

// ── Интерфейс ──

interface Word {
  id: string;
  position?: number;
  surfaceIast: string;
  surfaceDevanagari?: string;
  stem?: string;
  root?: string;
  pos?: string;
  gender?: string;
  caseType?: string;
  numberType?: string;
  person?: string | null;
  tense?: string | null;
  mood?: string | null;
  voice?: string | null;
  glossRu?: string;
  glossEn?: string;
  formationRuleNumbers?: number[];
}

interface VerseWordsListProps {
  words: Word[];
}

// ── Компонент ──

const VerseWordsList = ({ words }: VerseWordsListProps) => {
  const { t } = useTranslation();

  if (!words || words.length === 0) return null;

  const allFormationRuleNumbers = words
    .flatMap(w => w.formationRuleNumbers ?? [])
    .filter((v, i, a) => a.indexOf(v) === i);

  return (
    <div className="mb-4">
      <label className="flex justify-content-between align-items-center mb-1 font-semibold">
        <span>{t('sangraha.fields.words')}</span>
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
      </label>
      <div className="p-3 border-1 border-round surface-border surface-ground">
        {words.map((w) => (
          <div key={w.id} className="flex align-items-center gap-2 mb-1 flex-wrap">
            <span className="font-medium">{w.surfaceIast}</span>

            {w.surfaceDevanagari && (
              <span className="text-color-secondary font-italic">{w.surfaceDevanagari}</span>
            )}

            <span className="text-color-secondary">({w.pos || '-'})</span>

            {w.stem && <span className="text-sm text-color-secondary">stem: {w.stem}</span>}

            {w.root && w.root !== '.' && (
              <span className="text-sm text-color-secondary">√{w.root}</span>
            )}

            {w.gender && (
              <span className="text-sm text-color-secondary">{mapEnum(GENDER_MAP, w.gender)}</span>
            )}

            {w.caseType && (
              <span className="text-sm text-color-secondary">{mapEnum(CASE_MAP, w.caseType)}</span>
            )}

            {w.numberType && (
              <span className="text-sm text-color-secondary">{mapEnum(NUMBER_MAP, w.numberType)}</span>
            )}

            {w.person && (
              <span className="text-sm text-color-secondary">{mapEnum(PERSON_MAP, w.person)}</span>
            )}

            {w.tense && (
              <span className="text-sm text-color-secondary">{mapEnum(TENSE_MAP, w.tense)}</span>
            )}

            {w.mood && (
              <span className="text-sm text-color-secondary">{mapEnum(MOOD_MAP, w.mood)}</span>
            )}

            {w.voice && (
              <span className="text-sm text-color-secondary">{mapEnum(VOICE_MAP, w.voice)}</span>
            )}

            {w.glossRu && <span className="text-sm">— {w.glossRu}</span>}

            {w.glossEn && !w.glossRu && <span className="text-sm">— {w.glossEn}</span>}

            {w.formationRuleNumbers && w.formationRuleNumbers.length > 0 && (
              <span className="text-sm text-color-secondary">
                [{w.formationRuleNumbers.join(', ')}]
              </span>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};

export default VerseWordsList;
