import React, { useState, useEffect, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Button } from 'primereact/button';
import { Skeleton } from 'primereact/skeleton';
import { useDeclensionParadigm } from '../../hooks/useLessons';
import { CASE_TYPES } from '../../utils/grammarAggregation';
import type { DeclensionFormDto, DeclensionParadigmDto } from '../../types/content-dtos';

interface GrammarParadigmCarouselProps {
  slug: string;
  enabled: boolean;
}

const findForm = (forms: DeclensionFormDto[], caseType: string, numberType: string): DeclensionFormDto | undefined =>
  forms.find(f => f.caseType === caseType && f.numberType === numberType);

const deriveColumns = (forms: DeclensionFormDto[]): string[] => {
  const present = new Set(forms.map(f => f.numberType));
  return (['SINGULAR', 'DUAL', 'PLURAL'] as const).filter(n => present.has(n));
};

const renderTable = (
  paradigm: DeclensionParadigmDto,
  quizSlug: string,
  t: ReturnType<typeof useTranslation>['t'],
  i18n: ReturnType<typeof useTranslation>['i18n'],
  navigate: ReturnType<typeof useNavigate>,
) => {
  const columns = deriveColumns(paradigm.forms);
  const translation = i18n.language === 'ru' ? paradigm.translationRu : paradigm.translationEn;
  const genderLabel = t(`gender.${paradigm.gender}`);
  const stemIast = paradigm.stemIast;
  const stemDevanagari = paradigm.stemDevanagari;

  const handleCellClick = (caseType: string, numberType: string) => {
    navigate(
      `/quiz/grammar/${quizSlug}?filterScope=CASE_NUMBER_GENDER&filterCaseType=${caseType}&filterNumberType=${numberType}&filterGender=${paradigm.gender}`
    );
  };

  return (
    <>
      {/* Stem header */}
      <div className="mb-3">
        {stemIast ? (
          <span className="text-2xl font-bold">{stemIast}</span>
        ) : (
          <span className="text-2xl font-bold text-color-secondary">{t('grammar.paradigmsEmpty')}</span>
        )}
        {stemDevanagari && (
          <span className="text-base text-color-secondary ml-3" style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}>
            {stemDevanagari}
          </span>
        )}
        {translation && (
          <span className="text-base text-color-secondary ml-3">
            — {translation}
          </span>
        )}
        {/* gender badge below stem header */}
        <div className="text-xs text-color-secondary mt-1">{genderLabel}</div>
      </div>

      {/* Paradigm table */}
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-sm">
          <thead>
            <tr>
              <th className="text-left p-2 border-bottom-1 border-200 font-semibold" style={{ width: '25%' }}>
                {i18n.language === 'ru' ? 'Падеж' : 'Case'}
              </th>
              {columns.map(num => (
                <th key={num} className="text-center p-2 border-bottom-1 border-200 font-semibold">
                  {i18n.language === 'ru'
                    ? (num === 'SINGULAR' ? 'Ед.ч.' : num === 'DUAL' ? 'Дв.ч.' : 'Мн.ч.')
                    : (num === 'SINGULAR' ? 'Sg.' : num === 'DUAL' ? 'Du.' : 'Pl.')}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {CASE_TYPES.map(caseType => (
              <tr key={caseType}>
                <td className="p-2 border-bottom-1 border-100 text-color-secondary">
                  {i18n.language === 'ru'
                    ? (() => {
                        const map: Record<string, string> = {
                          NOMINATIVE: 'Им.', ACCUSATIVE: 'Вин.', INSTRUMENTAL: 'Тв.',
                          DATIVE: 'Дат.', ABLATIVE: 'Отл.', GENITIVE: 'Род.',
                          LOCATIVE: 'Мест.', VOCATIVE: 'Зв.',
                        };
                        return map[caseType] || caseType;
                      })()
                    : (() => {
                        const map: Record<string, string> = {
                          NOMINATIVE: 'Nom.', ACCUSATIVE: 'Acc.', INSTRUMENTAL: 'Ins.',
                          DATIVE: 'Dat.', ABLATIVE: 'Abl.', GENITIVE: 'Gen.',
                          LOCATIVE: 'Loc.', VOCATIVE: 'Voc.',
                        };
                        return map[caseType] || caseType;
                      })()}
                </td>
                {columns.map(num => {
                  const form = findForm(paradigm.forms, caseType, num);
                  if (!form) {
                    return (
                      <td key={num} className="text-center p-2 border-bottom-1 border-100 text-color-secondary">
                        —
                      </td>
                    );
                  }
                  return (
                    <td
                      key={num}
                      className="text-center p-2 border-bottom-1 border-100 cursor-pointer hover:surface-100 transition-colors"
                      onClick={() => handleCellClick(caseType, num)}
                      title={i18n.language === 'ru' ? 'Начать квиз по этой форме' : 'Start quiz for this form'}
                    >
                      <div className="font-bold text-base">{form.formIast}</div>
                      {form.formDevanagari && (
                        <div
                          className="text-xs text-color-secondary"
                          style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}
                        >
                          {form.formDevanagari}
                        </div>
                      )}
                    </td>
                  );
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
};

const GrammarParadigmCarousel: React.FC<GrammarParadigmCarouselProps> = ({ slug, enabled }) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [index, setIndex] = useState(0);
  const [totalCount, setTotalCount] = useState(0);
  const totalCountSet = useRef(false);

  const { data: page, isLoading, isError } = useDeclensionParadigm(slug, index, enabled);

  // Capture totalCount once from the first successful response
  useEffect(() => {
    if (page && page.totalCount > 0 && !totalCountSet.current) {
      setTotalCount(page.totalCount);
      totalCountSet.current = true;
    }
  }, [page]);

  const goPrev = () => {
    if (index > 0) setIndex(i => i - 1);
  };
  const goNext = () => {
    if (index < totalCount - 1) setIndex(i => i + 1);
  };

  // -- Empty state: not yet loaded or no data --
  if (!enabled) {
    return null;
  }

  // -- Loading state --
  if (isLoading && !page) {
    return (
      <div className="text-center p-4">
        <Skeleton width="100%" height="30px" className="mb-3" />
        <Skeleton width="100%" height="200px" />
      </div>
    );
  }

  // -- Error or empty --
  if (isError || !page || totalCount === 0) {
    return <div className="text-color-secondary p-4 text-center">{t('grammar.paradigmsEmpty')}</div>;
  }

  const isFirst = index === 0;
  const isLast = index >= totalCount - 1;

  return (
    <div>
      {/* Carousel toolbar: arrows + counter */}
      <div className="flex align-items-center justify-content-between mb-3">
        <Button
          icon="pi pi-chevron-left"
          onClick={goPrev}
          disabled={isFirst}
          className="p-button-text p-button-rounded"
          aria-label={i18n.language === 'ru' ? 'Предыдущая основа' : 'Previous stem'}
        />
        <span className="text-sm text-color-secondary font-medium">
          {index + 1} / {totalCount}
        </span>
        <Button
          icon="pi pi-chevron-right"
          onClick={goNext}
          disabled={isLast}
          className="p-button-text p-button-rounded"
          aria-label={i18n.language === 'ru' ? 'Следующая основа' : 'Next stem'}
        />
      </div>

      {/* Skeleton overlay during index transitions */}
      {isLoading ? (
        <div className="text-center p-4">
          <Skeleton width="100%" height="30px" className="mb-3" />
          <Skeleton width="100%" height="200px" />
        </div>
      ) : (
        renderTable(page.paradigm, slug, t, i18n, navigate)
      )}

      {/* Bottom arrows for convenience */}
      <div className="flex justify-content-center gap-3 mt-3">
        <Button
          icon="pi pi-chevron-left"
          onClick={goPrev}
          disabled={isFirst}
          className="p-button-outlined p-button-sm"
          label={i18n.language === 'ru' ? 'Пред.' : 'Prev'}
        />
        <Button
          icon="pi pi-chevron-right"
          onClick={goNext}
          disabled={isLast}
          className="p-button-outlined p-button-sm"
          iconPos="right"
          label={i18n.language === 'ru' ? 'След.' : 'Next'}
        />
      </div>
    </div>
  );
};

export default GrammarParadigmCarousel;
