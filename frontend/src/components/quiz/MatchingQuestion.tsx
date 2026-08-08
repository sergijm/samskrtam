import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import type { MatchingAnswerPayload, SessionQuestion } from '../../types/quiz';

interface MatchingQuestionProps {
  question: SessionQuestion;
  disabled?: boolean;
  feedback?: unknown;
  onSubmit: (payload: MatchingAnswerPayload) => void;
}

interface CorrectMatch {
  leftId: string;
  rightId: string;
}

/**
 * MATCHING (DECLENSION_MATCH): два столбца, выбор пары кликом слева → кликом
 * справа. Все left-элементы должны быть сопоставлены, иначе «Проверить» скрыт.
 * После `AnswerResult` подсвечиваем каждую пару по `correctMatches`.
 */
export default function MatchingQuestion({
  question,
  disabled,
  feedback,
  onSubmit,
}: MatchingQuestionProps) {
  const { t } = useTranslation();

  const left = question.matching?.left ?? [];
  const right = question.matching?.right ?? [];

  const [pairs, setPairs] = useState<Record<string, string>>({});
  const [selectedLeftId, setSelectedLeftId] = useState<string | null>(null);

  const correctMatches = useMemo<CorrectMatch[]>(
    () => {
      const r = feedback as { correctMatches?: CorrectMatch[] } | null | undefined;
      return r?.correctMatches ?? [];
    },
    [feedback],
  );

  const pickLeft = (id: string) => {
    if (disabled) return;
    setSelectedLeftId((cur) => (cur === id ? null : id));
  };

  const pickRight = (id: string) => {
    if (disabled) return;
    if (!selectedLeftId) return;
    setPairs((prev) => ({ ...prev, [selectedLeftId]: id }));
    setSelectedLeftId(null);
  };

  const allAssigned = left.length > 0 && left.every((item) => pairs[item.id]);

  const handleSubmit = () => {
    const matches = left.map((item) => ({ leftId: item.id, rightId: pairs[item.id] }));
    const payload: MatchingAnswerPayload = {
      sessionId: '',
      questionId: question.id,
      matches,
    };
    onSubmit(payload);
  };

  const isPairCorrect = (leftId: string, rightId: string) => {
    const matched = correctMatches.find((m) => m.leftId === leftId);
    return !!matched && matched.rightId === rightId;
  };

  const leftRowClass = (itemId: string) => {
    const classes = ['cursor-pointer', 'p-2', 'border-round', 'text-center', 'mb-2'];
    const rightId = pairs[itemId];
    if (feedback && rightId) {
      classes.push(isPairCorrect(itemId, rightId) ? 'bg-green-100' : 'bg-red-100');
    } else if (itemId === selectedLeftId) {
      classes.push('bg-primary-100');
    } else {
      classes.push('bg-white');
    }
    return classes.join(' ');
  };

  const rightRowClass = (itemId: string) => {
    const classes = ['cursor-pointer', 'p-2', 'border-round', 'text-center', 'mb-2'];
    const leftId = Object.keys(pairs).find((k) => pairs[k] === itemId);
    if (feedback && leftId) {
      classes.push(isPairCorrect(leftId, itemId) ? 'bg-green-100' : 'bg-red-100');
    } else {
      classes.push('bg-white');
    }
    return classes.join(' ');
  };

  return (
    <>
      <h3 className="text-center mb-3">{question.text}</h3>
      <p className="text-center text-sm text-color-secondary mb-2">
        {t('quiz.matching.resetHint')}
      </p>
      <div className="grid">
        <div className="col-6">
          {left.map((item) => (
            <div
              key={item.id}
              role="button"
              className={leftRowClass(item.id)}
              onClick={() => pickLeft(item.id)}
              style={{ border: '1px solid var(--surface-border)' }}
            >
              {item.text}
              {pairs[item.id] && (
                <span className="ml-2 pi pi-check" style={{ fontSize: '0.9rem' }} />
              )}
            </div>
          ))}
        </div>
        <div className="col-6">
          {right.map((item) => (
            <div
              key={item.id}
              role="button"
              className={rightRowClass(item.id)}
              onClick={() => pickRight(item.id)}
              style={{ border: '1px solid var(--surface-border)' }}
            >
              {item.text}
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
