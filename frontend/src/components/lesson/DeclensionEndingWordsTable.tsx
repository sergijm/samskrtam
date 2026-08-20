import React, { useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';
import { useAllDeclensionParadigms } from '../../hooks/useLessons';
import { useWordExamples } from '../../hooks/useSangraha';
import { IconButton } from '../common/buttons';
import { saveVerseBatchIds } from '../../utils/verseBatchIds';
import type { VerseWordExampleItemDto } from '../../types/sangraha';

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
  const navigate = useNavigate();
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

  // Уникальные словоформы (IAST) — запрашиваем примеры одним батчем.
  const surfaceIasts = useMemo(
    () => [...new Set(rows.map((row) => row.formIast))],
    [rows],
  );
  const { data: examplesData, isLoading: examplesLoading } = useWordExamples(surfaceIasts, surfaceIasts.length > 0);

  // surfaceIast → примеры (стихи) из санграхи.
  const examplesByForm = useMemo(() => {
    const map = new Map<string, VerseWordExampleItemDto[]>();
    for (const result of examplesData?.results ?? []) {
      map.set(result.surfaceIast, result.verses);
    }
    return map;
  }, [examplesData]);

  // Все verseId, отображаемые в колонке — сохраняются в localStorage для страницы
  // /sangraha/verses (аналогично вкладке «Примеры», DeclensionExamplesPanel).
  const openAllSangrahaVerses = () => {
    const ids = [...examplesByForm.values()]
      .flat()
      .map((example) => example.verseId);
    saveVerseBatchIds(ids);
    navigate('/sangraha/verses');
  };

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

  return (
    <div className="overflow-x-auto">
      <div className="flex align-items-center mb-2">
        <IconButton
          iconName="pi-arrow-left"
          className="p-button-rounded p-button-text"
          onClick={onBack}
        />
      </div>
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr>
            <th className="text-left p-2 border-bottom-1 border-100 font-semibold">
              {i18n.language === 'ru' ? 'Деванагари' : 'Devanagari'}
            </th>
            <th className="text-left p-2 border-bottom-1 border-100 font-semibold">IAST</th>
            <th className="p-2 border-bottom-1 border-100 font-semibold">
              {i18n.language === 'ru' ? 'Перевод' : 'Translation'}
            </th>
            <th className="text-left p-2 border-bottom-1 border-100 font-semibold">
              {t('endings.examplesFromSangraha')}
              <i
                className="pi pi-external-link ml-1 cursor-pointer"
                title={t('endings.openAllSangrahaVerses')}
                onClick={openAllSangrahaVerses}
              />
            </th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, idx) => {
            const examples = examplesByForm.get(row.formIast) ?? [];
            return (
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
                <td className="p-2 border-bottom-1 border-100 text-color-secondary">
                  {examplesLoading && examples.length === 0 ? (
                    <Skeleton width="100%" height="16px" />
                  ) : (
                    examples.map((example) => (
                      <div key={example.verseId} className="mb-1">
                        <div className="text-sm" style={{ fontStyle: 'italic' }}>
                          {example.textIast}
                        </div>
                        {example.translationRu && (
                          <div className="text-xs text-color-secondary">{example.translationRu}</div>
                        )}
                      </div>
                    ))
                  )}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default DeclensionEndingWordsTable;
