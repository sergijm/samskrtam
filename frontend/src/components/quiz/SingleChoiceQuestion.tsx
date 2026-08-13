import { Button } from 'primereact/button';
import { useTranslation } from 'react-i18next';
import type { SessionQuestion, AnswerResult } from '../../types/quiz';
import { promptText, optionText } from '../../utils/grammarTerms';

interface SingleChoiceQuestionProps {
  question: SessionQuestion;
  disabled?: boolean;
  feedback?: unknown;
  selectedOptionId?: string | null;
  onSubmit: (optionId: string) => void;
}

/**
 * SINGLE_CHOICE — рендер списка вариантов (DECLENSION_FORM_CHOICE, CASE_RECOGNITION).
 * Подсветка — по `AnswerResult` (корректный/ошибочный выбранный вариант).
 */
export default function SingleChoiceQuestion({
  question,
  disabled,
  feedback,
  selectedOptionId,
  onSubmit,
}: SingleChoiceQuestionProps) {
  const result = feedback as AnswerResult | null | undefined;
  const { i18n } = useTranslation();

  return (
    <>
      <h3 className="text-center mb-3">{promptText(question, i18n.language)}</h3>
      <div className="grid">
        {question.options.map((option) => {
          const isSelected = selectedOptionId === option.id;
          const isWrongSelection = !!result && !result.isCorrect && isSelected;
          const isCorrect = !!result && option.id === result.correctOptionId;
          return (
            <div key={option.id} className="col-12 md:col-6">
              <Button
                label={optionText(option, i18n.language)}
                className={`w-full text-xl p-3 mb-3 ${
                  isSelected ? 'p-button-primary' : 'p-button-outlined'
                } ${isWrongSelection ? 'p-button-danger' : ''} ${
                  isCorrect ? 'p-button-success' : ''
                }`}
                onClick={() => onSubmit(option.id)}
                disabled={!!disabled}
              />
            </div>
          );
        })}
      </div>
    </>
  );
}