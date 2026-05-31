import React, { useState } from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { useTranslation } from 'react-i18next';
import { GroupMember } from '../../types/user';
import GroupCuratorBadge from './GroupCuratorBadge';
import { useAuthStore } from '../../store/authStore';
import { useRemoveMember, useSetCurator } from '../../hooks/useGroups';
import { confirmDialog } from 'primereact/confirmdialog';

interface GroupMembersTableProps {
  groupId: string;
  members: GroupMember[];
  curatorId: string;
}

const GroupMembersTable = ({ groupId, members, curatorId }: GroupMembersTableProps) => {
  const { t } = useTranslation();
  const [globalFilter, setGlobalFilter] = useState('');
  const currentUser = useAuthStore((state) => state.user);
  const removeMemberMutation = useRemoveMember(groupId);
  const setCuratorMutation = useSetCurator(groupId);

  const canManage = currentUser?.role === 'ADMIN' || currentUser?.id === curatorId;

  const confirmRemove = (member: GroupMember) => {
    confirmDialog({
      message: t('groups.confirm.remove', { username: member.username }),
      header: 'Confirmation',
      icon: 'pi pi-exclamation-triangle',
      accept: () => removeMemberMutation.mutate(member.userId),
    });
  };

  const confirmSetCurator = (member: GroupMember) => {
    confirmDialog({
        message: t('groups.confirm.setCurator', { username: member.username }),
        header: 'Confirmation',
        icon: 'pi pi-question-circle',
        accept: () => setCuratorMutation.mutate(member.userId),
    });
  };

  const roleTemplate = (rowData: GroupMember) => {
    return rowData.groupRole === 'CURATOR' ? <GroupCuratorBadge /> : t('groups.member');
  };

  const actionsTemplate = (rowData: GroupMember) => {
    if (!canManage) return null;
    // Cannot remove self if curator, cannot make self curator again
    const isSelf = rowData.userId === currentUser?.id;
    const isCurator = rowData.groupRole === 'CURATOR';

    return (
      <div className="flex gap-2">
        <Button icon="pi pi-user-plus" className="p-button-rounded p-button-success" tooltip={t('groups.setCurator')}
            onClick={() => confirmSetCurator(rowData)} disabled={isCurator || setCuratorMutation.isLoading} />
        <Button icon="pi pi-trash" className="p-button-rounded p-button-danger" tooltip={t('groups.removeMember')}
            onClick={() => confirmRemove(rowData)} disabled={(isSelf && isCurator) || removeMemberMutation.isLoading} />
      </div>
    );
  };

  const header = (
    <div className="flex justify-content-end">
      <span className="p-input-icon-left">
        <i className="pi pi-search" />
        <InputText type="search" onInput={(e) => setGlobalFilter((e.target as HTMLInputElement).value)} placeholder={t('common.search')} />
      </span>
    </div>
  );

  return (
    <DataTable value={members} paginator rows={20} sortField="username" sortOrder={1}
      globalFilter={globalFilter} header={header} responsiveLayout="scroll">
      <Column field="username" header={t('groups.table.name')} sortable />
      <Column field="email" header="Email" sortable />
      <Column field="groupRole" header={t('groups.table.role')} body={roleTemplate} sortable />
      <Column field="joinedAt" header={t('groups.table.joined')} sortable body={(rowData) => new Date(rowData.joinedAt).toLocaleDateString()} />
      {canManage && <Column body={actionsTemplate} style={{ textAlign: 'center', width: '8rem' }} />}
    </DataTable>
  );
};

export default GroupMembersTable;
