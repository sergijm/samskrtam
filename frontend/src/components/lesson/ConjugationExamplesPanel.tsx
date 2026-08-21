import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';
import { useConjugationExamples } from '../../hooks/useLessons';
import type { ConjugationExamplesResponseDto } from '../../types/sangraha';

interface ConjugationExamplesPanelProps {
  slug: string;
  tense: string | null;
  mood: string | null;
  enabled: boolean;
}

const ConjugationExamplesPanel: React.FC<ConjugationExamplesPanelProps> =
  ({ slug, tense, mood, enabled }) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { data, isLoading, isError } = useConjugationExamples(slug, tense, mood, enabled);

  if (!enabled) {
    return null;
  }

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

  if (isError) {
    return <div className="text-color-secondary p-4 text-center">{t('grammar.examplesNoData')}</div>;
  }

  const groups = data?.groups ?? [];

  if (groups.length === 0) {
    return (
      <div className="text-color-secondary p-4 text-center">{t('grammar.examplesNoData')}</div>
    );
  }

  const translation = (item: ConjugationExamplesResponseDto['groups'][0]['examples'][0]) =>
    i18n.language === 'ru' ? item.translationRu : item.translationEn;

  const workTitle = (item: ConjugationExamplesResponseDto['groups'][0]['examples'][0]) =>
    i18n.language === 'ru' ? item.workTitleRu : item.workTitleEn;

  const chapterTitle = (item: ConjugationExamplesResponseDto['groups'][0]['examples'][0]) =>
    i18n.language === 'ru' ? item.chapterTitleRu : item.chapterTitleEn;

  return (
    <div className="conjugation-examples-panel">
      {groups.map(g => (
        <div key={`${g.tense}:${g.mood}`} className="mb-4">
          <h4 className="text-base font-semibold text-color mb-2" style={{ marginLeft: '1.25rem' }}>
            {g.tense}, {g.mood}
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
      ))}
    </div>
  );
};

export default ConjugationExamplesPanel;