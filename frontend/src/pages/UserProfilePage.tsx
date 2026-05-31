import React from 'react';
import { useParams } from 'react-router-dom';
import { Card } from 'primereact/card';
import { Tag } from 'primereact/tag';
import { useUser } from '../hooks/useUser';
import { ProgressSpinner } from 'primereact/progressspinner';
import UserAvatar from '../components/user/UserAvatar';
import { UserGroupChips } from '../components/user/UserGroupChips';
import { useTranslation } from 'react-i18next';

const UserProfilePage = () => {
  const { id } = useParams<{ id: string }>();
  const { data: user, isLoading, isError } = useUser(id!);
  const { t } = useTranslation();

  if (isLoading) {
    return <ProgressSpinner />;
  }

  if (isError || !user) {
    return <div>Error loading user profile.</div>;
  }

  return (
    <Card>
      <div className="grid">
        <div className="col-12 md:col-4">
          <UserAvatar username={user.username} email={user.email} size="xlarge" />
        </div>
        <div className="col-12 md:col-8">
          <h2>{user.username}</h2>
          <Tag value={user.role} severity={user.role === 'ADMIN' ? 'danger' : 'info'} />
        </div>
      </div>

      <div className="mt-4">
        <h5>{t('groups.title')}</h5>
        <UserGroupChips userId={user.id} />
      </div>
    </Card>
  );
};

export default UserProfilePage;
