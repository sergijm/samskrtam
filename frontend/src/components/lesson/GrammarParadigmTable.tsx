import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';
import { CASE_TYPES } from '../../utils/grammarAggregation';
import { lookup, FULL_CASE, FULL_CASE_RU } from '../../utils/grammarTerms';
import type { DeclensionParadigmDto, DeclensionFormDto } from '../../types/content-dtos';

interface GrammarParadigmTableProps {
  paradigms: DeclensionParadigmDto[];
  quizSlug: string;
}

/**
 * Finds a form in the array by (caseType, numberType).
 */
const findForm = (forms: DeclensionFormDto[], caseType: string, numberType: string): DeclensionFormDto | undefined =>
  forms.find(f => f.caseType === caseType && f.numberType === numberType);

/**
 * Collects unique numberTypes present in the forms, in canonical order.
 */
const deriveColumns = (forms: DeclensionFormDto[]): string[] => {
  const present = new Set(forms.map(f => f.numberType));
  return (['SINGULAR', 'DUAL', 'PLURAL'] as const).filter(n => present.has(n));
};

const GrammarParadigmTable: React.FC<GrammarParadigmTableProps> = ({ paradigms, quizSlug }) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  if (!paradigms || paradigms.length === 0) {
    return <div className="text-color-secondary p-4 text-center">{t('grammar.paradigmsEmpty')}</div>;
  }

  return (
    <>
      {paradigms.map((paradigm) => {
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
          <div key={paradigm.stemId} className="mb-4">
            {/* Subtitle: gender label for multi-gender lessons */}
            {paradigms.length > 1 && (
              <div className="text-lg font-semibold text-color-secondary mb-2">
                {genderLabel}
              </div>
            )}

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
            </div>

            {/* Paradigm table: rows = cases, cols = numbers */}
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
                        {lookup(caseType, i18n.language === 'ru' ? FULL_CASE_RU : FULL_CASE)}
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
          </div>
        );
      })}
    </>
  );
};

export default GrammarParadigmTable;
