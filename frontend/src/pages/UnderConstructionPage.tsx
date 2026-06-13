import React from 'react';
import { Card } from 'primereact/card';
import { useTranslation } from 'react-i18next';

const UnderConstructionPage = () => {
  const { t } = useTranslation();

  return (
    <div className="flex flex-column align-items-center justify-content-center min-h-screen p-4">
      <Card title={t('underConstruction.title')} className="text-center">
        <p className="text-xl">{t('underConstruction.message')}</p>
        <i className="pi pi-wrench text-6xl mt-4" style={{ color: 'var(--primary-color)' }}></i>
      </Card>
    </div>
  );
};

export default UnderConstructionPage;
