import React from 'react';
import { useTranslation } from 'react-i18next';
import VowelsTable from './alphabet/VowelsTable';
import WeakenedConsonantsTable from './alphabet/WeakenedConsonantsTable';
import ConsonantsTable from './alphabet/ConsonantsTable';
import NumeralsTable from './alphabet/NumeralsTable';

const AlphabetPage: React.FC = () => {
  const { i18n } = useTranslation();
  const isRu = i18n.language === 'ru';

  return (
      <div className="p-4">
        <h2 className="text-center mb-4">
          {isRu ? 'Алфавит и цифры санскрита (деванагари)' : 'Sanskrit alphabet & numerals (Devanāgarī)'}
        </h2>

        <div className="flex flex-wrap gap-3 justify-content-center align-items-start overflow-x-auto">
        <VowelsTable isRu={isRu} />
        <WeakenedConsonantsTable isRu={isRu} />
        <ConsonantsTable isRu={isRu} />
        </div>

      <NumeralsTable isRu={isRu} />
      </div>
  );
};

export default AlphabetPage;

