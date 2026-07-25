import React from 'react';
import { useTranslation } from 'react-i18next';

const AlphabetPage: React.FC = () => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-column align-items-center p-4">
      <img
        src="/sanskrit-alphabet-2.png"
        alt={t('writing.alphabet')}
        className="w-full max-w-4xl border-round shadow-2"
      />
    </div>
  );
};

export default AlphabetPage;
