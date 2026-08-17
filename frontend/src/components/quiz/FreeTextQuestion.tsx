import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { InputText } from 'primereact/inputtext';
import { Button } from 'primereact/button';
import type { SessionQuestion } from '../../types/quiz';
import { promptText } from '../../utils/grammarTerms';
import { asciiToDevanagari, asciiToIast } from '../../utils/transliteration';

interface FreeTextQuestionProps {
  question: SessionQuestion;
  disabled?: boolean;
  feedback?: unknown;
  currentQuestionIndex: number;
  totalQuestions: number;
  onSubmit: (value: string) => void;
}

/**
 * FREE_TEXT (DECLENSION_FORM): ввод словоформы в SLP1 с live-превью в IAST и
 * деванагари (алгоритм — utils/transliteration, тот же, что на
 * /writing/transliteration). На бэкенд уходит IAST-конверсия ввода.
 */
export default function FreeTextQuestion({
  question,
  disabled,
  currentQuestionIndex,
  totalQuestions,
  onSubmit,
}: FreeTextQuestionProps) {
  const { t, i18n } = useTranslation();
  const [value, setValue] = useState('');

  const trimmed = value.trim();
  const canSubmit = trimmed.length > 0 && !disabled;

  const devanagariPreview = asciiToDevanagari(value, 'slp1');
  const iastPreview = asciiToIast(value, 'slp1');

  const submit = () => {
    if (!canSubmit) return;
    onSubmit(asciiToIast(trimmed, 'slp1'));
  };

  return (
    <>
      <div className="text-center text-sm text-color-secondary mb-3">
        {currentQuestionIndex + 1} / {totalQuestions}
      </div>
      <h3 className="text-center mb-3">{promptText(question, i18n.language)}</h3>
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
        {devanagariPreview && (
          <div
            className="text-center w-full md:w-8"
            style={{ fontFamily: '"Noto Sans Devanagari", sans-serif', fontSize: '1.75rem' }}
          >
            {devanagariPreview}
          </div>
        )}
        {iastPreview && (
          <div className="text-center w-full md:w-8 text-color-secondary">{iastPreview}</div>
        )}
        <p className="m-0 text-sm text-color-secondary">{t('quiz.freeText.slp1Hint')}</p>
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
