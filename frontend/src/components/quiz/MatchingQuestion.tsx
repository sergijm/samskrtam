import { useState, type CSSProperties } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import type { MatchingAnswerPayload, SessionQuestion } from '../../types/quiz';
import {
  lookup,
  FULL_CASE,
  FULL_CASE_RU,
  FULL_NUMBER,
  FULL_NUMBER_RU,
} from '../../utils/grammarTerms';
import HighlightedPrompt from './HighlightedPrompt';

interface RightItem {
  id: string;
  caseType: string;
  numberType: string;
}

interface MatchingQuestionProps {
  question: SessionQuestion;
  disabled?: boolean;
  feedback?: unknown;
  onSubmit: (payload: MatchingAnswerPayload) => void;
}

const tileStyle: CSSProperties = {
  height: 72,
  display: 'flex',
  flexDirection: 'column',
  alignItems: 'center',
  justifyContent: 'center',
  textAlign: 'center',
  border: '1px solid var(--surface-border)',
  borderRadius: 'var(--border-radius)',
  padding: '0.5rem 0.75rem',
  overflow: 'hidden',
};

/**
 * MATCHING (DECLENSION_MATCH) — сопоставление по порядку.
 * Две колонки плиток одинаковой высоты: слева словоформы (эталон), справа метки
 * «падеж, число» — их перетаскивают драг-энд-дропом. Первая плитка справа
 * сопоставляется первой слева и так далее; проверка — по индексам.
 */
export default function MatchingQuestion({
  question,
  disabled,
  feedback,
  onSubmit,
}: MatchingQuestionProps) {
  const { t, i18n } = useTranslation();
  const isRu = i18n.language === 'ru';

  const left = question.matchRows ?? [];

  const buildRight = (q: SessionQuestion): RightItem[] =>
    (q.options ?? [])
      .filter((o) => o.caseType && o.numberType)
      .map((o) => ({ id: o.id, caseType: o.caseType!, numberType: o.numberType! }))
      .sort(() => Math.random() - 0.5);

  const [questionId, setQuestionId] = useState(question.id);
  const [right, setRight] = useState<RightItem[]>(() => buildRight(question));
  if (question.id !== questionId) {
    setQuestionId(question.id);
    setRight(buildRight(question));
  }

  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [overIndex, setOverIndex] = useState<number | null>(null);

  const locked = !!disabled || !!feedback;
  const allAssigned = left.length > 0 && right.length === left.length;

  const labelText = (r: RightItem) => {
    const c = lookup(r.caseType, isRu ? FULL_CASE_RU : FULL_CASE);
    const n = lookup(r.numberType, isRu ? FULL_NUMBER_RU : FULL_NUMBER);
    return [c, n].filter(Boolean).join(', ') || `${r.caseType} ${r.numberType}`;
  };

  const handleDrop = (toIndex: number) => {
    if (locked || dragIndex === null || dragIndex === toIndex) return;
    setRight((prev) => {
      const next = [...prev];
      const [moved] = next.splice(dragIndex, 1);
      next.splice(toIndex, 0, moved);
      return next;
    });
    setDragIndex(null);
    setOverIndex(null);
  };

  const handleSubmit = () => {
    const matches = left
      .map((row, i) => ({ leftId: row.id, rightId: right[i]?.id }))
      .filter((m) => m.rightId);
    const payload: MatchingAnswerPayload = { sessionId: '', questionId: question.id, matches };
    onSubmit(payload);
  };

  const feedbackIsCorrect = (feedback as { isCorrect?: boolean } | null | undefined)?.isCorrect;
  const feedbackTint =
    feedbackIsCorrect === true ? '2px solid var(--green-500)' : feedbackIsCorrect === false ? '2px solid var(--red-500)' : '1px solid var(--surface-border)';

  return (
    <>
      <h3 className="text-center mb-3">
        <HighlightedPrompt question={question} lang={i18n.language} />
      </h3>
      <p className="text-center text-sm text-color-secondary mb-2">
        {t('quiz.matching.reorderHint')}
      </p>
      <div className="grid">
        <div className="col-6">
          {left.map((row) => (
            <div
              key={row.id}
              className="mb-2"
              style={{ ...tileStyle, ...(feedback ? { border: feedbackTint } : {}) }}
            >
              {row.wordFormDevanagari ? (
                <>
                  <span
                    className="text-2xl"
                    style={{ fontFamily: '"Noto Sans Devanagari", sans-serif', lineHeight: 1.3 }}
                  >
                    {row.wordFormDevanagari}
                  </span>
                  <span className="text-sm text-color-secondary" style={{ fontStyle: 'italic' }}>
                    {row.wordFormIast}
                  </span>
                </>
              ) : (
                <span className="text-xl">{row.wordFormIast}</span>
              )}
            </div>
          ))}
        </div>

        <div className="col-6">
          {right.map((item, index) => (
            <div
              key={item.id}
              draggable={!locked}
              onDragStart={(e) => {
                e.dataTransfer.setData('text/plain', String(index));
                e.dataTransfer.effectAllowed = 'move';
                setDragIndex(index);
              }}
              onDragOver={(e) => {
                if (locked) return;
                e.preventDefault();
                e.dataTransfer.dropEffect = 'move';
                setOverIndex(index);
              }}
              onDrop={(e) => {
                if (locked) return;
                e.preventDefault();
                handleDrop(index);
              }}
              onDragEnd={() => {
                setDragIndex(null);
                setOverIndex(null);
              }}
              className="mb-2"
              style={{
                ...tileStyle,
                cursor: locked ? 'default' : 'grab',
                opacity: dragIndex === index ? 0.4 : 1,
                ...(overIndex === index && dragIndex !== null && dragIndex !== index
                  ? { borderTop: '3px solid var(--primary-color)' }
                  : {}),
                ...(feedback ? { border: feedbackTint } : {}),
              }}
            >
              <span className="text-xl font-bold">{labelText(item)}</span>
            </div>
          ))}
        </div>
      </div>

      {!feedback && allAssigned && (
        <div className="flex justify-content-center mt-3">
          <Button
            label={t('quiz.matching.checkButton')}
            icon="pi pi-check"
            className="w-full md:w-6"
            onClick={handleSubmit}
            disabled={!!disabled}
          />
        </div>
      )}
    </>
  );
}
