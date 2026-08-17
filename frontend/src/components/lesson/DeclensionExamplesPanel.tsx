import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { Skeleton } from 'primereact/skeleton';
import { useDeclensionExamples } from '../../hooks/useLessons';
import { useAuthStore } from '../../store/authStore';
import { saveVerseBatchIds } from '../../utils/verseBatchIds';
import { CASE_TYPES, NUMBER_TYPES } from '../../utils/grammarAggregation';
import { FULL_CASE, FULL_CASE_RU, FULL_NUMBER, FULL_NUMBER_RU } from '../../utils/grammarTerms';
import type { DeclensionExamplesResponseDto } from '../../types/content-dtos';

interface DeclensionExamplesPanelProps {
  slug: string;
  enabled: boolean;
}

const DeclensionExamplesPanel: React.FC<DeclensionExamplesPanelProps> = ({ slug, enabled }) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const isAdmin = useAuthStore((s) => s.user?.roles?.includes('ADMIN') ?? false);
  const { data, isLoading, isError } = useDeclensionExamples(slug, enabled);

  // -- Not yet enabled (lazy — first click hasn't happened) --
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

  // -- Empty response: no groups at all --
  const groups = data?.groups ?? [];
  const missingVerseIds = data?.missingVerseIds ?? [];

  // Все уникальные verseId из примеров (вкладка «Примеры» доступна всем,
  // но страница /sangraha/verses — только ADMIN).
  const allVerseIds = [...new Set(groups.flatMap((g) => g.examples.map((e) => e.verseId)))];

  // Кнопка «Открыть все стихи» — только для ADMIN (целевая страница ADMIN-only).
  // Список verseId кладём в localStorage и переходим на /sangraha/verses без
  // query-параметров — страница сама прочитает их оттуда.
  const openAllVersesButton = isAdmin && allVerseIds.length > 0 && (
    <Button
      className="p-button-sm p-button-outlined"
      icon="pi pi-external-link"
      label={t('grammar.examples.openAll', { count: allVerseIds.length })}
      onClick={() => {
        saveVerseBatchIds(allVerseIds);
        navigate('/sangraha/verses');
      }}
    />
  );

  // Кнопка «Проанализировать недостающие примеры» — только для ADMIN
  // (поле missingVerseIds приходит только ADMIN-роли) и только при непустом списке.
  const analyzeMissingButton = missingVerseIds.length > 0 && (
    <Button
      className="p-button-sm p-button-outlined"
      icon="pi pi-sync"
      label={t('grammar.examples.analyzeMissing', { count: missingVerseIds.length })}
      onClick={() =>
        navigate(`/sangraha/verses?${missingVerseIds.map((id) => `id=${id}`).join('&')}`)
      }
    />
  );

  const examplesToolbar = (analyzeMissingButton || openAllVersesButton) && (
    <div className="flex flex-wrap gap-2 mb-3">
      {openAllVersesButton}
      {analyzeMissingButton}
    </div>
  );

  if (groups.length === 0) {
    return (
      <div>
        {examplesToolbar}
        <div className="text-color-secondary p-4 text-center">{t('grammar.examplesNoData')}</div>
      </div>
    );
  }

  // -- Build a lookup map for quick access --
  const groupMap = new Map<string, DeclensionExamplesResponseDto['groups'][0]>();
  for (const g of groups) {
    groupMap.set(`${g.caseType}:${g.numberType}`, g);
  }

  // -- Check if there is any non-empty group --
  let hasAnyExamples = false;
  for (const g of groups) {
    if (g.examples.length > 0) {
      hasAnyExamples = true;
      break;
    }
  }

  if (!hasAnyExamples) {
    return (
      <div>
        {examplesToolbar}
        <div className="text-color-secondary p-4 text-center">{t('grammar.examplesNoData')}</div>
      </div>
    );
  }

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
      {examplesToolbar}
      {/* Iterate CASE_TYPES (rows), then NUMBER_TYPES (cols) — same order as paradigm table */}
      {CASE_TYPES.map(caseType =>
        NUMBER_TYPES.map(numberType => {
          const key = `${caseType}:${numberType}`;
          const group = groupMap.get(key);
          if (!group || group.examples.length === 0) return null;

          return (
            <div key={key} className="mb-4">
              {/* Group header: case + number */}
              <h4 className="text-base font-semibold text-color mb-2" style={{ marginLeft: '1.25rem' }}>
                {caseLabel(caseType)}, {numberLabel(numberType)}
              </h4>

              {/* One common box for all examples of this case+number group */}
              <div className="p-3 border-1 border-200 border-round surface-ground">
                {group.examples.map((ex, index) => (
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
                    {/* IAST (large) */}
                    <div className="text-base font-medium" style={{ fontStyle: 'italic' }}>
                      {ex.textIast}
                    </div>
                    {/* Devanagari (small) */}
                    {ex.textDevanagari && (
                      <div
                        className="text-sm text-color-secondary mt-1"
                        style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}
                      >
                        {ex.textDevanagari}
                      </div>
                    )}
                    {/* Translation */}
                    <div className="text-sm text-color-secondary mt-1">
                      {translation(ex)}
                    </div>
                    {/* Attribution */}
                    <div className="text-xs text-color-secondary mt-1">
                      {`${workTitle(ex)}, ${chapterTitle(ex)}, ${i18n.language === 'ru' ? 'стих' : 'verse'} ${ex.verseOrderIndex}`}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          );
        })
      )}
    </div>
  );
};

export default DeclensionExamplesPanel;