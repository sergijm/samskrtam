import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { useAllDeclensionParadigms } from '../../hooks/useLessons';

interface DeclensionEndingWordsTableProps {
  selection: {
    caseType: string;
    numberType: string;
    gender?: string;
    endingText: string;
  };
  slug: string;
  totalCount: number;
  onBack: () => void;
}

/**
 * Highlights the suffix matching `endingText` at the end of `text` (IAST only).
 * A leading '-' in `endingText` is stripped before matching; if the text does not
 * end with the ending, it is returned unchanged (e.g. Devanagari strings, where a
 * character-boundary-safe match is not guaranteed).
 */
const highlightEnding = (text: string, endingText: string): React.ReactNode => {
  const ending = endingText.replace(/^-/, '');
  if (ending && text.endsWith(ending)) {
    return (
      <>
        {text.slice(0, -ending.length)}
        <span className="text-primary font-bold">{text.slice(-ending.length)}</span>
      </>
    );
  }
  return text;
};

const DeclensionEndingWordsTable: React.FC<DeclensionEndingWordsTableProps> = ({ selection, slug, totalCount, onBack }) => {
  const { t, i18n } = useTranslation();
  const { pages, isLoading } = useAllDeclensionParadigms(slug, totalCount, true);

  const rows = useMemo(() => {
    const result: Array<{ formDevanagari: string; formIast: string; translation: string | null }> = [];
    for (const page of pages) {
      for (const form of page.paradigm.forms) {
        if (
          form.caseType === selection.caseType &&
          form.numberType === selection.numberType &&
          (!selection.gender || page.paradigm.gender === selection.gender)
        ) {
          result.push({
            formDevanagari: form.formDevanagari,
            formIast: form.formIast,
            translation: i18n.language === 'ru' ? page.paradigm.translationRu : page.paradigm.translationEn,
          });
        }
      }
    }
    return result;
  }, [pages, selection, i18n.language]);

  if (isLoading && rows.length === 0) {
    return (
      <div className="text-center p-4">
        <Skeleton width="100%" height="30px" className="mb-3" />
        <Skeleton width="100%" height="200px" />
      </div>
    );
  }

  if (rows.length === 0) {
    return <div className="text-color-secondary p-4 text-center">{t('grammar.paradigmsEmpty')}</div>;
  }

  const numberLabel = t(`number.${selection.numberType}`);
  const genderLabel = selection.gender ? t(`gender.${selection.gender}`) : '';
  const axisLabel = genderLabel ? `${numberLabel}, ${genderLabel}` : numberLabel;

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr>
            <th
              className="text-left p-2 border-bottom-1 border-200 font-semibold cursor-pointer hover:surface-100 transition-colors"
              style={{ width: '25%' }}
              onClick={onBack}
            >
              {i18n.language === 'ru' ? 'Падеж' : 'Case'}
            </th>
            <th
              className="text-center p-2 border-bottom-1 border-200 font-semibold cursor-pointer hover:surface-100 transition-colors"
              colSpan={2}
              onClick={onBack}
            >
              {i18n.language === 'ru' ? 'Число/Род' : 'Number/Gender'}: {axisLabel}
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => (
            <tr key={idx}>
              <td
                className="text-left p-2 border-bottom-1 border-100"
                style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}
              >
                {row.formDevanagari}
              </td>
              <td className="text-left p-2 border-bottom-1 border-100 font-bold">
                {highlightEnding(row.formIast, selection.endingText)}
              </td>
              <td className="p-2 border-bottom-1 border-100 text-color-secondary">
                {row.translation}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default DeclensionEndingWordsTable;
