import React from 'react';
import { Tag } from 'primereact/tag';
import { useTranslation } from 'react-i18next';

interface GroupCuratorBadgeProps {
  isCurator: boolean;
}

const GroupCuratorBadge = ({ isCurator }: GroupCuratorBadgeProps) => {
  const { t } = useTranslation();

  if (!isCurator) {
    return null;
  }

  return (
    <Tag value={t('groups.curator')} severity="warning" icon="pi pi-star" />
  );
};

export default GroupCuratorBadge;
