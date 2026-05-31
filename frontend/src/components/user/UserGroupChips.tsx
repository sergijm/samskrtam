import React from 'react';
import { Chip } from 'primereact/chip';
import { useNavigate } from 'react-router-dom';
import { useUserGroups } from '../../hooks/useUser';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';

interface UserGroupChipsProps {
  userId: string;
}

export const UserGroupChips = ({ userId }: UserGroupChipsProps) => {
  const { data: groups, isLoading } = useUserGroups(userId);
  const navigate = useNavigate();
  const { t } = useTranslation();

  if (isLoading) {
    return <ProgressSpinner style={{ width: '20px', height: '20px' }} />;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {groups?.map(g => (
        <Chip
          key={g.groupId}
          label={g.groupName}
          icon={g.groupRole === 'CURATOR' ? 'pi pi-star' : undefined}
          className={g.groupRole === 'CURATOR' ? 'p-chip-warning' : ''}
          onClick={() => navigate(`/groups/${g.groupId}`)}
          style={{ cursor: 'pointer' }}
        />
      ))}
      {groups?.length === 0 && (
        <span className="text-color-secondary">{t('groups.noGroups')}</span>
      )}
    </div>
  );
};
