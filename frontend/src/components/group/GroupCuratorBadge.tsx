import React from 'react';
import { Tag } from 'primereact/tag';
import { useTranslation } from 'react-i18next';

const GroupCuratorBadge = () => {
  const { t } = useTranslation();
  return <Tag severity="warning" value={t('groups.curator')} icon="pi pi-star" />;
};

export default GroupCuratorBadge;
