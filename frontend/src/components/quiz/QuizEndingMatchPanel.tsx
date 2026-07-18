import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Checkbox } from 'primereact/checkbox';
import { Button } from 'primereact/button';
import type { SessionQuestion } from '../../types/quiz';

interface QuizEndingMatchPanelProps {
  question: SessionQuestion;
  disabled: boolean;
  feedback: unknown;
  onSelectOption: (optionIds: string[]) => void;
}

export default function QuizEndingMatchPanel({
  question,
  disabled,
  feedback,
  onSelectOption,
}: QuizEndingMatchPanelProps) {
  const { t, i18n } = useTranslation();
  const [checkedIds, setCheckedIds] = useState<string[]>([]);

  const getCaseLabel = (option: SessionQuestion['options'][number]) =>
    i18n.language === 'ru' ? option.caseRu : option.caseEn;

  const getNumberLabel = (option: SessionQuestion['options'][number]) =>
    i18n.language === 'ru' ? option.numberRu : option.numberEn;

  const getGenderLabel = (option: SessionQuestion['options'][number]) =>
    i18n.language === 'ru' ? option.genderRu : option.genderEn;

  const toggleOption = (id: string) => {
    if (disabled) return;
    setCheckedIds((prev) =>
      prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id],
    );
  };

  const handleSubmit = () => {
    onSelectOption(checkedIds);
  };

  const fb = feedback as { correctOptionId?: string; isCorrect?: boolean } | null;

  return (
    <>
      <div className="text-2xl font-bold text-center mb-5">
        <span style={{ color: 'var(--text-color-secondary)', fontSize: '1.25rem' }}>
          {t('quiz.endingMatchPrompt', 'Выберите все формы, соответствующие окончанию:')}
        </span>
        <br style={{ lineHeight: '1.5' }} />
        <span
          style={{
            color: 'var(--primary-color)',
            fontWeight: 'bold',
            fontSize: '3rem',
            fontFamily: '"Noto Sans Devanagari", sans-serif',
          }}
        >
          {question.caseEnding}
        </span>
      </div>

      <div className="flex flex-column gap-3 align-items-center mb-4">
        {question.options.map((option) => {
          const caseLabel = getCaseLabel(option);
          const numberLabel = getNumberLabel(option);
          const genderLabel = getGenderLabel(option);
          const labelParts = [caseLabel, numberLabel, genderLabel].filter(Boolean);
          const label = labelParts.join(' · ');

          const isChecked = checkedIds.includes(option.id);

          return (
            <div
              key={option.id}
              className={`field-checkbox p-3 w-full md:w-8 border-round ${
                isChecked ? 'surface-hover' : ''
              }`}
              style={{ maxWidth: '500px', cursor: disabled ? 'default' : 'pointer' }}
              onClick={() => toggleOption(option.id)}
            >
              <Checkbox
                inputId={`ending-opt-${option.id}`}
                checked={isChecked}
                disabled={disabled}
                onChange={() => toggleOption(option.id)}
              />
              <label
                htmlFor={`ending-opt-${option.id}`}
                className="ml-3 text-xl"
                style={{ cursor: disabled ? 'default' : 'pointer' }}
              >
                {label || option.formIast}
              </label>
            </div>
          );
        })}
      </div>

      {!feedback && (
        <div className="flex justify-content-center">
          <Button
            label={t('quiz.submit')}
            icon="pi pi-check"
            className="w-full md:w-6"
            onClick={handleSubmit}
            disabled={disabled}
          />
        </div>
      )}
    </>
  );
}
