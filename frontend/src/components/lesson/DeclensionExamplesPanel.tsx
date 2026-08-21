import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';
import { useDeclensionExamples } from '../../hooks/useLessons';
import { FULL_CASE, FULL_CASE_RU, FULL_NUMBER, FULL_NUMBER_RU } from '../../utils/grammarTerms';
import type { DeclensionExamplesResponseDto } from '../../types/sangraha';

interface DeclensionExamplesPanelProps {
  slug: string;
  vowelType: string;
  enabled: boolean;
  filterCaseType?: string | null;
  filterNumberType?: string | null;
}

const DeclensionExamplesPanel: React.FC<DeclensionExamplesPanelProps> =
  ({ slug, vowelType, enabled, filterCaseType, filterNumberType }) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { data, isLoading, isError } = useDeclensionExamples(slug, vowelType, enabled);

  // -- Not yet enabled --
  if (!enabled) {
    return null;
  }

  // -- Loading --
  if (isLoading && !data) {
    return (
      <div className="text-center p-4">
        <Skeleton width="100%" height="20px" className="mb-3" />
        <Skeleton width="100%" height="80px" className="mb-2" />
        <Skeleton width="100%" height="80px" className="mb-2" />
        <Skeleton width="100%" height="80px" />
      </div>
    );
  }

  // -- Error --
  if (isError) {
    return <div className="text-color-secondary p-4 text-center">{t('grammar.examplesNoData')}</div>;
  }

  const groups = data?.groups ?? [];

  if (groups.length === 0) {
    return (
      <div className="text-color-secondary p-4 text-center">{t('grammar.examplesNoData')}</div>
    );
  }

  // -- Build lookup + apply filter --
  const groupMap = new Map<string, DeclensionExamplesResponseDto['groups'][0]>();
  for (const g of groups) {
    groupMap.set(`${g.caseType}:${g.numberType}`, g);
  }

  const hasFilter = filterCaseType || filterNumberType;
  const filteredGroups = hasFilter
    ? groups.filter(g =>
        (!filterCaseType || g.caseType === filterCaseType) &&
        (!filterNumberType || g.numberType === filterNumberType)
      )
    : groups;

  const caseLabel = (caseType: string) =>
    i18n.language === 'ru' ? FULL_CASE_RU[caseType] ?? caseType : FULL_CASE[caseType] ?? caseType;

  const numberLabel = (numberType: string) =>
    i18n.language === 'ru' ? FULL_NUMBER_RU[numberType] ?? numberType : FULL_NUMBER[numberType] ?? numberType;

  const translation = (item: DeclensionExamplesResponseDto['groups'][0]['examples'][0]) =>
    i18n.language === 'ru' ? item.translationRu : item.translationEn;

  const workTitle = (item: DeclensionExamplesResponseDto['groups'][0]['examples'][0]) =>
    i18n.language === 'ru' ? item.workTitleRu : item.workTitleEn;

  const chapterTitle = (item: DeclensionExamplesResponseDto['groups'][0]['examples'][0]) =>
    i18n.language === 'ru' ? item.chapterTitleRu : item.chapterTitleEn;

  return (
    <div className="declension-examples-panel">
      {filteredGroups.length === 0 ? (
        <div className="text-color-secondary p-4 text-center">{t('grammar.examplesNoData')}</div>
      ) : (
        filteredGroups.map(g => (
          <div key={`${g.caseType}:${g.numberType}`} className="mb-4">
            <h4 className="text-base font-semibold text-color mb-2" style={{ marginLeft: '1.25rem' }}>
              {caseLabel(g.caseType)}, {numberLabel(g.numberType)}
            </h4>

            <div className="p-3 border-1 border-200 border-round surface-ground">
              {g.examples.map((ex, index) => (
                <div
                  key={ex.verseId}
                  role="button"
                  tabIndex={0}
                  className={`cursor-pointer select-none p-1 ${index > 0 ? 'pt-2 mt-2 border-top-1 border-200' : ''}`}
                  onClick={() => navigate(`/sangraha/${ex.workSlug}/verses/${ex.verseId}`)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault();
                      navigate(`/sangraha/${ex.workSlug}/verses/${ex.verseId}`);
                    }
                  }}
                >
                  <div className="text-base font-medium" style={{ fontStyle: 'italic' }}>
                    {ex.textIast}
                  </div>
                  {ex.textDevanagari && (
                    <div
                      className="text-sm text-color-secondary mt-1"
                      style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}
                    >
                      {ex.textDevanagari}
                    </div>
                  )}
                  <div className="text-sm text-color-secondary mt-1">
                    {translation(ex)}
                  </div>
                  <div className="text-xs text-color-secondary mt-1">
                    {`${workTitle(ex)}, ${chapterTitle(ex)}, ${i18n.language === 'ru' ? 'стих' : 'verse'} ${ex.verseOrderIndex}`}
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))
      )}
    </div>
  );
};

export default DeclensionExamplesPanel;