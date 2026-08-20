import React from 'react';
import { useTranslation } from 'react-i18next';
import {
  PRESENT_ENDINGS,
  PRESENT_PERSONS,
  PRESENT_NUMBERS,
  findEndingCell,
  type SriEndingRow,
  type SriNumeral,
  type SriVoice,
} from '../../data/presentConjugation';

interface ConjugationEndingsTableProps {
  voice: SriVoice;
  endings?: Record<SriVoice, SriEndingRow[]>;
}

const personNameKey = (person: number): string =>
  person === 3 ? 'PRATHAMA' : person === 2 ? 'MADHYAMA' : 'UTTAMA';

const numberShort = (t: (k: string) => string, num: SriNumeral): string => num === 'singular' ? t('grammar.numberShort.singular') : num === 'dual' ? t('grammar.numberShort.dual') : t('grammar.numberShort.plural');

const ConjugationEndingsTable: React.FC<ConjugationEndingsTableProps> = ({ voice, endings = PRESENT_ENDINGS }) => {
  const { t } = useTranslation();

  return (
    <div>
      <table className="border-collapse text-sm">
        <thead>
          <tr>
            <th className="text-left p-2 pr-3 border-1 border-200 font-semibold whitespace-nowrap">
              {t('grammar.personHeader')}
            </th>
            {PRESENT_NUMBERS.map(num => (
              <th key={num} className="text-left p-2 border-1 border-200 font-semibold">
                {numberShort(t, num)}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {PRESENT_PERSONS.map(person => {
            const row = endings[voice].find(r => r.person === person);
            return (
              <tr key={person}>
                <td className="p-2 pr-3 border-1 border-200 text-color-secondary font-medium whitespace-nowrap">
                  {t(`grammar.person.${personNameKey(person)}`)}
                </td>
                {PRESENT_NUMBERS.map(num => {
                  const cell = row ? findEndingCell(row, num) : undefined;
                  return (
                    <td key={num} className="text-left p-2 border-1 border-200">
                      {cell ? (
                        <>
                          <span className="font-medium">{cell.transliteration}</span>
                          <span className="block text-color-secondary" style={{ fontFamily: 'Noto Sans Devanagari, sans-serif' }}>
                            {cell.sanskrit}
                          </span>
                        </>
                      ) : (
                        <span className="text-color-secondary">—</span>
                      )}
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
};

export default ConjugationEndingsTable;