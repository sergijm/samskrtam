import React, { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { Skeleton } from 'primereact/skeleton';
import { useConjugationParadigm } from '../../hooks/useLessons';
import ConjugationParadigmTable from './ConjugationParadigmTable';
import { PRESENT_NUMBERS } from '../../data/presentConjugation';

interface ConjugationParadigmCarouselProps {
  slug: string;
  voice: string;
  enabled: boolean;
  onTotalChange?: (total: number) => void;
}

/**
 * Index-based carousel over the verb lemmas of a conjugation lesson.
 * Each page shows ONE verb with its full present-tense paradigm (example
 * sentences per person × number). Mirrors GrammarParadigmCarousel.
 */
const ConjugationParadigmCarousel: React.FC<ConjugationParadigmCarouselProps> = ({
  slug,
  voice,
  enabled,
  onTotalChange,
}) => {
  const { t } = useTranslation();
  const [index, setIndex] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const totalCountSet = useRef(false);
  const prevVoice = useRef(voice);

  const { data: page, isLoading, isError } = useConjugationParadigm(slug, index, voice, enabled);

  // Reset to the first page whenever the voice changes.
  useEffect(() => {
    if (prevVoice.current !== voice) {
      prevVoice.current = voice;
      setIndex(0);
      totalCountSet.current = false;
      setTotalCount(0);
    }
  }, [voice]);

  // Capture totalCount once from the first successful response.
  useEffect(() => {
    if (page && page.totalCount > 0 && !totalCountSet.current) {
      setTotalCount(page.totalCount);
      totalCountSet.current = true;
    }
  }, [page]);

  useEffect(() => {
    onTotalChange?.(totalCount);
  }, [totalCount, onTotalChange]);

  // The number of cells we expect per verb (person × number).
  const expectedCells = 3 * PRESENT_NUMBERS.length;

  if (!enabled) {
    return null;
  }

  if (isLoading && !page) {
    return (
      <div className="text-center p-4">
        <Skeleton width="100%" height="30px" className="mb-3" />
        <Skeleton width="100%" height="200px" />
      </div>
    );
  }

  if (isError || !page || !page.paradigm || totalCount === 0) {
    return <div className="text-color-secondary p-4 text-center">{t('grammar.paradigmsEmpty')}</div>;
  }

  const isFirst = index === 0;
  const isLast = index >= totalCount - 1;

  return (
    <div>
      <div className="flex align-items-center justify-content-between mb-3">
        <Button
          icon="pi pi-chevron-left"
          onClick={() => setIndex(i => i - 1)}
          disabled={isFirst}
          className="p-button-text p-button-rounded"
          aria-label={t('grammar.prevVerb')}
        />
        <span className="text-sm text-color-secondary font-medium">
          {index + 1} / {totalCount}
        </span>
        <Button
          icon="pi pi-chevron-right"
          onClick={() => setIndex(i => i + 1)}
          disabled={isLast}
          className="p-button-text p-button-rounded"
          aria-label={t('grammar.nextVerb')}
        />
      </div>

      {isLoading ? (
        <div className="text-center p-4">
          <Skeleton width="100%" height="30px" className="mb-3" />
          <Skeleton width="100%" height="200px" />
        </div>
      ) : (
        <ConjugationParadigmTable paradigm={page.paradigm} />
      )}

      <div className="text-xs text-color-secondary mt-2">
        {t('grammar.paradigmCount', { count: expectedCells })}
      </div>
    </div>
  );
};

export default ConjugationParadigmCarousel;