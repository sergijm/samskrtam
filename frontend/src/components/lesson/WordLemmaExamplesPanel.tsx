import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';
import { useWordLemmaExamples } from '../../hooks/useLessons';
import type { VerseWordExampleItemDto } from '../../types/sangraha';

interface WordLemmaExamplesPanelProps {
  lemma: string;
  enabled: boolean;
}

const ExampleVerse: React.FC<{ item: VerseWordExampleItemDto; isRu: boolean }> = ({ item, isRu }) => {
  const navigate = useNavigate();
  return (
    <div
      role="button"
      tabIndex={0}
      className="cursor-pointer select-none p-1 hover:surface-hover border-round"
      onClick={() => navigate(`/sangraha/${item.workSlug}/verses/${item.verseId}`)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          navigate(`/sangraha/${item.workSlug}/verses/${item.verseId}`);
        }
      }}
    >
      <div className="text-base font-medium" style={{ fontStyle: 'italic' }}>
        {item.textIast}
      </div>
      {item.textDevanagari && (
        <div
          className="text-sm text-color-secondary mt-1"
          style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}
        >
          {item.textDevanagari}
        </div>
      )}
      <div className="text-sm text-color-secondary mt-1">
        {isRu ? item.translationRu : item.translationEn}
      </div>
      <div className="text-xs text-color-secondary mt-1">
        {`${isRu ? item.workTitleRu : item.workTitleEn}, ${isRu ? item.chapterTitleRu : item.chapterTitleEn}, ${isRu ? 'стих' : 'verse'} ${item.verseOrderIndex}`}
      </div>
    </div>
  );
};

const WordLemmaExamplesPanel: React.FC<WordLemmaExamplesPanelProps> = (
  { lemma, enabled },
) => {
  const { t, i18n } = useTranslation();
  const isRu = i18n.language === 'ru';
  const { data, isLoading, isError } = useWordLemmaExamples(lemma, enabled);

  if (!enabled) {
    return null;
  }

  if (isLoading && !data) {
    return (
      <div className="p-3">
        <Skeleton width="100%" height="20px" className="mb-3" />
        <Skeleton width="100%" height="70px" className="mb-2" />
        <Skeleton width="100%" height="70px" />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="text-color-secondary p-3 text-center">
        {t('vocabulary.examplesError')}
      </div>
    );
  }

  const verses = data?.results?.[0]?.verses ?? [];

  return (
    <div className="p-3 border-top-1 border-200">
      {verses.length === 0 ? (
        <div className="text-color-secondary text-sm">
          {t('vocabulary.noExamples')}
        </div>
      ) : (
        <div className="flex flex-column gap-1">
          {verses.map((ex) => (
            <ExampleVerse key={ex.verseId} item={ex} isRu={isRu} />
          ))}
        </div>
      )}
    </div>
  );
};

export default WordLemmaExamplesPanel;
