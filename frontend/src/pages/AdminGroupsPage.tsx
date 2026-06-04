import React from 'react';
import { Card } from 'primereact/card';
import { useTranslation } from 'react-i18next';

export default function AdminGroupsPage() {
  const { t } = useTranslation();

  return (
    <div className="flex flex-column align-items-center p-4">
      <Card title={t('groups.title')} className="w-full" style={{ maxWidth: '1200px' }}>
        <p>{t('admin.groups.description')}</p>
        {/* TODO: Add group management components here */}
        <p>This page will contain the group management interface.</p>
      </Card>
    </div>
  );
}
