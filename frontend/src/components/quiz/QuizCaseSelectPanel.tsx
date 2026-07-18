import { useTranslation } from 'react-i18next';
import { RadioButton } from 'primereact/radiobutton';
import type { SessionQuestion } from '../../types/quiz';

interface QuizCaseSelectPanelProps {
  question: SessionQuestion;
  selectedOptionId: string | null;
  disabled: boolean;
  feedback: unknown;
  onSelectOption: (optionId: string) => void;
}

export default function QuizCaseSelectPanel({
  question,
  selectedOptionId,
  disabled,
  feedback,
  onSelectOption,
}: QuizCaseSelectPanelProps) {
  const { i18n } = useTranslation();

  const getCaseLabel = (option: SessionQuestion['options'][number]) =>
    i18n.language === 'ru' ? option.caseRu : option.caseEn;

  const getNumberLabel = (option: SessionQuestion['options'][number]) =>
    i18n.language === 'ru' ? option.numberRu : option.numberEn;

  const getGenderLabel = (option: SessionQuestion['options'][number]) =>
    i18n.language === 'ru' ? option.genderRu : option.genderEn;

  return (
    <>
      <div className="text-2xl font-bold text-center mb-5">
        <span style={{ color: 'var(--primary-color)', fontWeight: 'bold', fontSize: '2.5rem' }}>
          {question.formIast}
        </span>
        {question.formDevanagari && (
          <>
            <br style={{ lineHeight: '1.5' }} />
            <span
              className="mt-2"
              style={{
                fontFamily: '"Noto Sans Devanagari", sans-serif',
                fontSize: '2rem',
                color: 'var(--text-color-secondary)',
              }}
            >
              {question.formDevanagari}
            </span>
          </>
        )}
      </div>

      <div className="flex flex-column gap-3 align-items-center">
        {question.options.map((option) => {
          const caseLabel = getCaseLabel(option);
          const numberLabel = getNumberLabel(option);
          const genderLabel = getGenderLabel(option);
          const labelParts = [caseLabel, numberLabel, genderLabel].filter(Boolean);
          const label = labelParts.join(' · ');

          const isSelected = selectedOptionId === option.id;
          const isCorrectOption =
            feedback && option.id === (feedback as { correctOptionId: string }).correctOptionId;
          const isWrongSelected =
            feedback && isSelected && !(feedback as { isCorrect: boolean }).isCorrect;

          return (
            <div
              key={option.id}
              className={`field-radiobutton p-3 w-full md:w-8 border-round ${
                isSelected ? 'surface-hover' : ''
              } ${
                isCorrectOption ? 'border-1 border-green-500' : ''
              } ${
                isWrongSelected ? 'border-1 border-red-500' : ''
              }`}
              style={{ maxWidth: '500px' }}
            >
              <RadioButton
                inputId={`option-${option.id}`}
                value={option.id}
                checked={isSelected}
                disabled={disabled}
                onChange={() => onSelectOption(option.id)}
              />
              <label
                htmlFor={`option-${option.id}`}
                className="ml-3 text-xl"
                style={{ cursor: disabled ? 'default' : 'pointer' }}
              >
                {label || option.formIast}
              </label>
            </div>
          );
        })}
      </div>
    </>
  );
}
