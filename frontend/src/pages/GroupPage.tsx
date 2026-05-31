import React, { useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { Card } from 'primereact/card';
import { Button } from 'primereact/button';
import { ProgressSpinner } from 'primereact/progressspinner';
import { useGroup } from '../hooks/useGroups';
import { useAuthStore } from '../store/authStore';
import { useTranslation } from 'react-i18next';
import GroupMembersTable from '../components/group/GroupMembersTable';
import AddMemberDialog from '../components/group/AddMemberDialog';
import { ConfirmDialog } from 'primereact/confirmdialog';

const GroupPage = () => {
  const { id } = useParams<{ id: string }>();
  const { data: group, isLoading, isError } = useGroup(id!);
  const currentUser = useAuthStore((state) => state.user);
  const { t } = useTranslation();
  const [addMemberDialogVisible, setAddMemberDialogVisible] = useState(false);

  if (isLoading) {
    return <ProgressSpinner />;
  }

  if (isError || !group) {
    return <div>Error loading group.</div>;
  }

  const canManage = currentUser?.role === 'ADMIN' || currentUser?.id === group.curatorId;

  const header = (
    <div className="flex justify-content-between align-items-center">
      <h2 className="m-0">{group.name}</h2>
      {canManage && (
        <div className="flex gap-2">
          <Button label={t('groups.addMember')} icon="pi pi-plus" onClick={() => setAddMemberDialogVisible(true)} />
          <Link to={`/groups/${id}/edit`} className="p-button">{t('common.edit')}</Link>
        </div>
      )}
    </div>
  );

  return (
    <>
      <ConfirmDialog />
      <Card header={header}>
        <div className="mb-4">
          <strong>{t('groups.curator')}: </strong>
          <Link to={`/users/${group.curatorId}`}>{group.curatorName}</Link>
        </div>
        <GroupMembersTable groupId={group.id} members={group.members} curatorId={group.curatorId} />
      </Card>
      {canManage && (
        <AddMemberDialog
          groupId={group.id}
          visible={addMemberDialogVisible}
          onHide={() => setAddMemberDialogVisible(false)}
        />
      )}
    </>
  );
};

export default GroupPage;
