import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { LessonType } from '../../types/quiz';
import type { SessionQuestion } from '../../types/quiz';

interface QuizQuestionPanelProps {
  question: SessionQuestion;
  selectedOptionId: string | null;
  disabled: boolean;
  lessonType: LessonType;
  feedback: unknown;
  onSelectOption: (optionId: string) => void;
}

export default function QuizQuestionPanel({
  question,
  selectedOptionId,
  disabled,
  lessonType,
  feedback,
  onSelectOption,
}: QuizQuestionPanelProps) {
  const { t } = useTranslation();

  return (
    <>
      <h2 className="text-center mb-4">
        {t('quiz.question', { current: 1, total: 1 })}
      </h2>
      <div className="text-2xl font-bold text-center mb-5">
        {lessonType === LessonType.VOCABULARY ? (
          <span style={{ color: 'var(--primary-color)', fontWeight: 'bold', fontSize: '2.5rem' }}>
            {question.text}
          </span>
        ) : (
          <>
            {question.stem && (
              <>
                <span style={{ color: 'var(--primary-color)', fontWeight: 'bold', fontSize: '2.5rem' }}>
                  {question.text}
                </span>
                <br style={{ lineHeight: '1.5' }} />
              </>
            )}
            {question.caseType && question.numberType && (
              <span style={{ fontSize: '1.5rem', fontStyle: 'italic' }}>
                {question.caseType}, {question.numberType}
              </span>
            )}
          </>
        )}
      </div>

      <div className="grid">
        {question.options.map((option) => (
          <div key={option.id} className="col-12 md:col-6">
            <Button
              label={option.formIast}
              className={`w-full text-xl p-3 mb-3 ${
                selectedOptionId === option.id ? 'p-button-primary' : 'p-button-outlined'
              } ${
                feedback && option.id === (feedback as { correctOptionId: string }).correctOptionId
                  ? 'p-button-success'
                  : ''
              } ${
                feedback && selectedOptionId === option.id && !(feedback as { isCorrect: boolean }).isCorrect
                  ? 'p-button-danger'
                  : ''
              }`}
              onClick={() => onSelectOption(option.id)}
              disabled={disabled}
            />
          </div>
        ))}
      </div>
    </>
  );
}