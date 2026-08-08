import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import type { SessionQuestion } from '../../types/quiz';

interface QuizMatchPanelProps {
  question: SessionQuestion;
  disabled: boolean;
  feedback: unknown;
  onSelectOption: (submissions: { rowId: string; optionId: string }[]) => void;
}

export default function QuizMatchPanel({
  question,
  disabled,
  feedback,
  onSelectOption,
}: QuizMatchPanelProps) {
  const { t } = useTranslation();
  const [pairs, setPairs] = useState<Record<string, string>>({});

  const rows = question.matchRows ?? [];
  const labels = question.options ?? [];

  const assign = (rowId: string, optionId: string) => {
    if (disabled) return;
    setPairs((prev) => ({ ...prev, [rowId]: optionId }));
  };

  const handleSubmit = () => {
    const submissions = rows
      .filter((r) => pairs[r.id])
      .map((r) => ({ rowId: r.id, optionId: pairs[r.id] }));
    onSelectOption(submissions);
  };

  const allAssigned = rows.length > 0 && rows.every((r) => pairs[r.id]);

  return (
    <>
      <h3 className="text-center mb-4">
        {t('quiz.matchPrompt', 'Соедините словоформу с падежом и числом:')}
      </h3>

      <div className="grid mb-4">
        {rows.map((row) => (
          <div key={row.id} className="col-12 md:col-6 flex align-items-center gap-2 mb-2">
            <div
              className="flex-1 text-xl p-2 border-round text-center"
              style={{ fontFamily: '"Noto Sans Devanagari", sans-serif', border: '1px solid var(--surface-border)' }}
            >
              {row.wordFormDevanagari || row.wordFormIast}
            </div>
            <label className="flex align-items-center gap-1 text-sm">
              {t('quiz.case', 'Падеж:')}
              <select
                value={pairs[row.id] ?? ''}
                disabled={disabled}
                onChange={(e) => assign(row.id, e.target.value)}
                className="p-2 border-round"
                style={{ border: '1px solid var(--surface-border)' }}
              >
                <option value="">—</option>
                {labels.map((opt) => (
                  <option key={opt.id} value={opt.id}>
                    {`${opt.caseType ?? ''} ${opt.numberType ?? ''}`.trim() || opt.formIast}
                  </option>
                ))}
              </select>
            </label>
          </div>
        ))}
      </div>

      {!feedback && (
        <div className="flex justify-content-center">
          <Button
            label={t('quiz.submit')}
            icon="pi pi-check"
            className="w-full md:w-6"
            onClick={handleSubmit}
            disabled={disabled || !allAssigned}
          />
        </div>
      )}
    </>
  );
}