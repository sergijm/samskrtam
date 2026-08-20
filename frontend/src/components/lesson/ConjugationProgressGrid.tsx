import { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { MiniProgressBar } from '../common/MiniProgressBar';
import type { ConjugationCellProgress, WordStatus } from '../../types/lesson';

interface Props {
  progress: ConjugationCellProgress[];
  quizSlug: string;
}

const VOICES = ['PARASMAIPADA', 'ATMANEPADA'] as const;
const PERSONS = [1, 2, 3] as const;
const NUMBERS = ['SINGULAR', 'DUAL', 'PLURAL'] as const;

const voiceLabel = (v: string) => v === 'PARASMAIPADA' ? 'Parasmaipada' : 'Ātmanepada';

const personLabel = (p: number) =>
  p === 1 ? '1st' : p === 2 ? '2nd' : '3rd';

const numberLabel = (n: string, ru: boolean) => {
  const map: Record<string, string> = ru
    ? { SINGULAR: 'Ед.', DUAL: 'Дв.', PLURAL: 'Мн.' }
    : { SINGULAR: 'Sg.', DUAL: 'Du.', PLURAL: 'Pl.' };
  return map[n] ?? n;
};

function computeAgg(
  items: ConjugationCellProgress[],
): { aggregatedProgress: number; status: WordStatus; total: number; learned: number } {
  if (items.length === 0) return { aggregatedProgress: 0, status: 'NEW', total: 0, learned: 0 };
  const sum = items.reduce((a, b) => a + b.score, 0);
  const avg = Math.round(sum / items.length);
  const learned = items.filter(i => i.score >= 90).length;
  const status: WordStatus = avg <= 0 ? 'NEW' : avg < 90 ? 'LEARNING' : 'MASTERED';
  return { aggregatedProgress: avg, status, total: items.length, learned };
}

export default function ConjugationProgressGrid({ progress, quizSlug }: Props) {
  const { i18n } = useTranslation();
  const navigate = useNavigate();
  const ru = i18n.language === 'ru';

  const cellByKey = useMemo(() => {
    const map = new Map<string, ConjugationCellProgress>();
    for (const p of progress) {
      map.set(`${p.voice}:${p.person}:${p.numberType}`, p);
    }
    return map;
  }, [progress]);

  const handleCellClick = (voice: string, person: number, numberType: string) => {
    navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${voice}|${person}|${numberType}`);
  };

  const handleVoiceClick = (voice: string) => {
    navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${voice}`);
  };

  const handlePersonClick = (person: number) => {
    navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${person}`);
  };

  const handleNumberClick = (numberType: string) => {
    navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${numberType}`);
  };

  if (progress.length === 0) {
    return (
      <div className="text-center p-4 text-color-secondary">
        {ru ? 'Данные о прогрессе отсутствуют' : 'No progress data available'}
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr>
            <th className="text-left p-2 border-bottom-1 border-200 font-semibold" style={{ width: '30%' }} />
            {NUMBERS.map(num => (
              <th
                key={num}
                className="text-center p-2 border-bottom-1 border-200 font-semibold cursor-pointer hover:surface-100 transition-colors"
                onClick={() => handleNumberClick(num)}
              >
                {numberLabel(num, ru)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {VOICES.map(voice => (
            PERSONS.map((person, pi) => {
              const isFirst = pi === 0;
              return (
                <tr key={`${voice}:${person}`}>
                  {isFirst && (
                    <td
                      rowSpan={PERSONS.length}
                      className="px-2 py-1 font-semibold cursor-pointer hover:surface-100 transition-colors align-top"
                      onClick={() => handleVoiceClick(voice)}
                      style={{ verticalAlign: 'top', paddingTop: '0.5rem' }}
                    >
                      {voiceLabel(voice)}
                    </td>
                  )}
                  <td
                    className="px-2 py-1 text-sm cursor-pointer hover:surface-100 transition-colors"
                    onClick={() => handlePersonClick(person)}
                  >
                    {personLabel(person)}
                  </td>
                  {NUMBERS.map(num => {
                    const cell = cellByKey.get(`${voice}:${person}:${num}`);
                    const score = cell?.score ?? 0;
                    const status = cell?.status ?? 'NEW';
                    return (
                      <td
                        key={num}
                        className="text-center px-2 py-1 cursor-pointer hover:surface-100 transition-colors"
                        onClick={() => handleCellClick(voice, person, num)}
                      >
                        <MiniProgressBar value={score} status={status} width="90px" className="justify-content-center" />
                      </td>
                    );
                  })}
                </tr>
              );
            })
          ))}
        </tbody>
      </table>

      <div className="mt-4 pt-3 border-top-1 border-200">
        <div className="grid pl-2">
          <div className="col-12 md:col-6">
            <div className="flex flex-column gap-1">
              {VOICES.map(v => {
                const rows = progress.filter(p => p.voice === v);
                const agg = computeAgg(rows);
                return (
                  <AggRow
                    key={v}
                    id={v}
                    name={voiceLabel(v)}
                    score={agg.aggregatedProgress}
                    status={agg.status}
                    ru={ru}
                    onStart={() => navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${v}`)}
                  />
                );
              })}
            </div>
            <div className="flex flex-column gap-1 mt-3">
              {PERSONS.map(p => {
                const rows = progress.filter(r => r.person === p);
                const agg = computeAgg(rows);
                return (
                  <AggRow
                    key={p}
                    id={String(p)}
                    name={personLabel(p)}
                    score={agg.aggregatedProgress}
                    status={agg.status}
                    ru={ru}
                    onStart={() => navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${p}`)}
                  />
                );
              })}
            </div>
            <div className="flex flex-column gap-1 mt-3">
              {NUMBERS.map(n => {
                const rows = progress.filter(r => r.numberType === n);
                const agg = computeAgg(rows);
                return (
                  <AggRow
                    key={n}
                    id={n}
                    name={numberLabel(n, ru)}
                    score={agg.aggregatedProgress}
                    status={agg.status}
                    ru={ru}
                    onStart={() => navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${n}`)}
                  />
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function AggRow({ name, score, status, onStart }: {
  id: string;
  name: string;
  score: number;
  status: WordStatus;
  ru: boolean;
  onStart: () => void;
}) {
  return (
    <div className="flex align-items-center gap-4" style={{ minHeight: 0 }}>
      <span className="flex-1 text-sm" style={{ lineHeight: '1.25rem' }}>{name}</span>
      <MiniProgressBar value={score} status={status} width="110px" />
      <i
        className="pi pi-angle-double-right cursor-pointer hover:text-primary transition-colors"
        style={{ fontSize: '1rem' }}
        onClick={onStart}
      />
    </div>
  );
}