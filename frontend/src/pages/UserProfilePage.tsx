import React from 'react';
import { useParams } from 'react-router-dom';
import { Card } from 'primereact/card';
import { Tag } from 'primereact/tag';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useTranslation } from 'react-i18next';
import { useUser } from '../hooks/useUser';
import UserAvatar from '../components/user/UserAvatar';
import { UserGroupChips } from '../components/user/UserGroupChips';

const UserProfilePage = () => {
  const { id } = useParams<{ id: string }>();
  const { data: user, isLoading: isUserLoading, isError: isUserError } = useUser(id!);
  const { t } = useTranslation();

  if (isUserLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isUserError || !user) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <div>{t('userProfile.errorLoadingUser')}</div>
      </div>
    );
  }

  return (
    <div className="p-grid p-nogutter p-justify-center">
      <div className="p-col-12 p-md-10 p-lg-8">
        <Card className="p-shadow-2 mt-4">
          <div className="p-grid p-align-center">
            <div className="p-col-12 p-md-4 p-text-center">
              <UserAvatar username={user.username} email={user.email} size="xlarge" />
            </div>
            <div className="p-col-12 p-md-8">
              <h2>{user.username}</h2>
              <Tag value={user.role} severity={user.role === 'ADMIN' ? 'danger' : 'info'} />
              <div className="mt-3">
                <h5>{t('groups.title')}</h5>
                <UserGroupChips userId={user.id} />
              </div>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default UserProfilePage;
