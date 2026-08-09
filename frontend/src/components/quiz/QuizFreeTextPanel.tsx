import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import type { SessionQuestion } from '../../types/quiz';

interface QuizFreeTextPanelProps {
  question: SessionQuestion;
  disabled: boolean;
  currentQuestionIndex: number;
  totalQuestions: number;
  feedback: unknown;
  onFreeTextSubmit: (text: string) => void;
}

/**
 * Render a FREE_TEXT question as a text input. The user types the expected form
 * (e.g. the dative dual of 'putra') instead of picking from options, which are
 * intentionally absent for free-text questions in the v2 compose flow.
 */
export default function QuizFreeTextPanel({
  question,
  disabled,
  currentQuestionIndex,
  totalQuestions,
  feedback,
  onFreeTextSubmit,
}: QuizFreeTextPanelProps) {
  const { t } = useTranslation();
  const [value, setValue] = useState('');

  const submit = () => {
    const trimmed = value.trim();
    if (!trimmed) return;
    onFreeTextSubmit(trimmed);
  };

  const answered = !!feedback;

  return (
    <div className="text-center">
      <div className="text-center text-sm text-color-secondary mb-3">
        {currentQuestionIndex + 1} / {totalQuestions}
      </div>
      <div className="text-2xl font-bold mt-4" style={{ color: 'var(--primary-color)' }}>
        {question.text}
      </div>

      <div className="flex flex-column align-items-center gap-3 mt-5">
        <InputText
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !answered) submit();
          }}
          disabled={disabled || answered}
          placeholder={t('quiz.freeTextPlaceholder')}
          autoFocus
          className="text-center w-full"
          style={{ fontFamily: '"Noto Sans Devanagari", sans-serif', fontSize: '1.75rem', maxWidth: '360px' }}
        />
        <Button
          label={t('quiz.submit')}
          icon="pi pi-check"
          onClick={submit}
          disabled={disabled || answered || !value.trim()}
        />
      </div>
    </div>
  );
}