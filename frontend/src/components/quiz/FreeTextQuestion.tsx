import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import type { SessionQuestion } from '../../types/quiz';

interface FreeTextQuestionProps {
  question: SessionQuestion;
  disabled?: boolean;
  feedback?: unknown;
  onSubmit: (value: string) => void;
}

/**
 * FREE_TEXT (DECLENSION_FORM): ввод словоформы + отправка. Сравнение ответа —
 * только на бэкенде, фронт лишь не даёт отправить пустую строку.
 */
export default function FreeTextQuestion({
  question,
  disabled,
  onSubmit,
}: FreeTextQuestionProps) {
  const { t } = useTranslation();
  const [value, setValue] = useState('');

  const trimmed = value.trim();
  const canSubmit = trimmed.length > 0 && !disabled;

  const submit = () => {
    if (!canSubmit) return;
    onSubmit(trimmed);
  };

  return (
    <>
      <h3 className="text-center mb-3">{question.text}</h3>
      {question.stemDevanagari && (
        <div
          className="text-center mb-3"
          style={{ fontFamily: '"Noto Sans Devanagari", sans-serif', fontSize: '1.5rem' }}
        >
          {question.stemDevanagari}
        </div>
      )}
      <div className="flex flex-column align-items-center gap-3">
        <InputText
          value={value}
          onChange={(e) => setValue(e.target.value)}
          placeholder={t('quiz.freeText.placeholder')}
          className="w-full md:w-8"
          onKeyDown={(e) => { if (e.key === 'Enter') submit(); }}
          disabled={disabled}
        />
        <Button
          label={t('quiz.submit')}
          icon="pi pi-check"
          className="w-full md:w-6"
          onClick={submit}
          disabled={!canSubmit}
        />
      </div>
    </>
  );
}