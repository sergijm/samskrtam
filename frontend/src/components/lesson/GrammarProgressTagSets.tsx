import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { MiniProgressBar } from '../common/MiniProgressBar';
import {
  aggregateByCase,
  aggregateByNumber,
  aggregateByCasePair,
} from '../../utils/grammarAggregation';
import type { GrammarQuestionProgress, WordStatus } from '../../types/lesson';

interface Props {
  items: GrammarQuestionProgress[];
  quizSlug: string;
}

interface RowProgress {
  aggregatedProgress: number;
  status: WordStatus;
}

/**
 * Additional progress slices (progress-tag sets) for the grammar lesson
 * Progress tab, rendered below the case×number grid.
 *
 * Layout: two columns — the left one stacks per-case and per-number rows,
 * the right one holds the per-case-pair rows. Each row is a single line:
 * name (left), mini progress bar, then a start-quiz button (double chevron).
 * The button opens a quiz filtered by the corresponding progressTagSetId.
 */
const GrammarProgressTagSets: React.FC<Props> = ({ items, quizSlug }) => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();

  if (!items || items.length === 0) return null;

  const cases = aggregateByCase(items);
  const numbers = aggregateByNumber(items);
  const pairs = aggregateByCasePair(items);

  const startQuiz = (setId: string) => {
    navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${setId}`);
  };

  const lang = i18n.language;

  return (
    <div className="mt-4">
      <div className="grid">
        <div className="col-12 md:col-6">
          <div className="flex flex-column gap-0">
            {cases.map(agg => (
              <Row
                key={agg.caseType}
                id={agg.caseType}
                name={lang === 'ru' ? agg.caseRu : agg.caseEn}
                progress={agg}
                lang={lang}
                onStart={startQuiz}
              />
            ))}
          </div>
          <div className="flex flex-column gap-0 mt-1">
            {numbers.map(agg => (
              <Row
                key={agg.numberType}
                id={agg.numberType}
                name={lang === 'ru' ? agg.numberRu : agg.numberEn}
                progress={agg}
                lang={lang}
                onStart={startQuiz}
              />
            ))}
          </div>
        </div>
        <div className="col-12 md:col-6">
          <div className="flex flex-column gap-0">
            {pairs.map(agg => (
              <Row
                key={agg.setId}
                id={agg.setId}
                name={lang === 'ru' ? `${agg.caseRuA} ↔ ${agg.caseRuB}` : `${agg.caseEnA} ↔ ${agg.caseEnB}`}
                progress={agg}
                lang={lang}
                onStart={startQuiz}
              />
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};

const Row: React.FC<{
  id: string;
  name: string;
  progress: RowProgress;
  lang: string;
  onStart: (setId: string) => void;
}> = ({ id, name, progress, lang, onStart }) => (
  <div className="flex align-items-center gap-3">
    <span className="flex-1 text-sm">{name}</span>
    <MiniProgressBar value={progress.aggregatedProgress} status={progress.status} width="110px" />
    <Button
      icon="pi pi-angle-double-right"
      severity="secondary"
      text
      rounded
      tooltip={lang === 'ru' ? 'Квиз' : 'Quiz'}
      tooltipOptions={{ position: 'top' }}
      onClick={() => onStart(id)}
    />
  </div>
);

export default GrammarProgressTagSets;