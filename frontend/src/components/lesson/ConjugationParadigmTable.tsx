import React from 'react';
import { useTranslation } from 'react-i18next';
import type { SriNumeral } from '../../data/presentConjugation';
import type { ConjugationParadigmDto } from '../../types/content-dtos';

interface ConjugationParadigmTableProps {
  paradigm: ConjugationParadigmDto;
}

const numberShort = (t: (k: string) => string, num: SriNumeral): string => num === 'singular' ? t('grammar.numberShort.singular') : num === 'dual' ? t('grammar.numberShort.dual') : t('grammar.numberShort.plural');

const personNameKey = (person: number): string =>
  person === 3 ? 'PRATHAMA' : person === 2 ? 'MADHYAMA' : 'UTTAMA';

const personOrder = (person: number): number => (person === 3 ? 0 : person === 2 ? 1 : 2);
const numOrder = (num: SriNumeral): number => (num === 'singular' ? 0 : num === 'dual' ? 1 : 2);

/**
 * One verb's present-tense paradigm: rows = persons, columns = numbers.
 * Each cell shows the example sentence (Devanagari + IAST + translation).
 */
const ConjugationParadigmTable: React.FC<ConjugationParadigmTableProps> = ({ paradigm }) => {
  const { t } = useTranslation();

  const persons = paradigm.forms.length > 0
    ? [...new Set(paradigm.forms.map(f => f.person))].sort((a, b) => personOrder(a) - personOrder(b))
    : [1];
  const numbers: SriNumeral[] = ['singular', 'dual', 'plural'];

  const cellOf = (person: number, num: SriNumeral) => {
    const form = paradigm.forms.find(f => f.person === person && f.numberType === num.toUpperCase());
    return form;
  };

  const stemHeader = (
    <div className="mb-3">
      {paradigm.lemmaIast ? (
        <span className="text-2xl font-bold">{paradigm.lemmaIast}</span>
      ) : (
        <span className="text-2xl font-bold">{paradigm.lemmaDevanagari}</span>
      )}
      {paradigm.lemmaDevanagari && (
        <span className="text-base ml-3" style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}>{paradigm.lemmaDevanagari}</span>
      )}
      {paradigm.meaningRu && (
        <span className="text-base ml-3">— {paradigm.meaningRu}</span>
      )}
      <div className="text-xs text-color-secondary mt-1">{t(`grammar.voice.${paradigm.voice}`)}</div>
    </div>
  );

  return (
    <div>
      {stemHeader}
      <div className="overflow-x-auto">
        <table className="w-full border-collapse text-sm" style={{ tableLayout: 'fixed' }}>
          <thead>
            <tr>
              <th className="text-left p-2 border-bottom-1 border-200 font-semibold" style={{ width: '18%' }}>
                {t('grammar.personHeader')}
              </th>
              {numbers
                .sort((a, b) => numOrder(a) - numOrder(b))
                .map(num => (
                  <th key={num} className="text-center p-2 border-bottom-1 border-200 font-semibold">
                    {numberShort(t, num)}
                  </th>
                ))}
            </tr>
          </thead>
          <tbody>
            {persons.map(person => {
              const rowLabel = t(`grammar.person.${personNameKey(person)}`);
              return (
                <tr key={person}>
                  <td className="p-2 border-bottom-1 border-100 text-color-secondary">
                    {rowLabel}
                  </td>
                  {numbers
                    .sort((a, b) => numOrder(a) - numOrder(b))
                    .map(num => {
                      const form = cellOf(person, num);
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
                          className="p-2 border-bottom-1 border-100 align-top"
                          style={{ verticalAlign: 'top' }}
                        >
                          <div className="font-bold">{form.sentenceIast}</div>
                          <div
                            className="text-color-secondary"
                            style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}
                          >
                            {form.sentenceDevanagari}
                          </div>
                          <div className="text-color-secondary italic">{form.translationRu}</div>
                        </td>
                      );
                    })}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ConjugationParadigmTable;