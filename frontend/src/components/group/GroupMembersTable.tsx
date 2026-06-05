import React from 'react';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { Tag } from 'primereact/tag';
import { useTranslation } from 'react-i18next';
import { UserGroupSummary, GroupMember } from '../../types/user'; // Import GroupMember
import { useRemoveMember, useSetCurator } from '../../hooks/useGroups';
import { confirmDialog } from 'primereact/confirmdialog'; // Import confirmDialog

interface GroupMembersTableProps {
  groupId: string;
  members: GroupMember[];
  curatorId: string;
  canManage: boolean;
  isCurator: boolean;
}

const GroupMembersTable = ({ groupId, members, curatorId, canManage, isCurator }: GroupMembersTableProps) => {
  const { t } = useTranslation();
  const removeMemberMutation = useRemoveMember(groupId);
  const setCuratorMutation = useSetCurator(groupId);

  const roleBodyTemplate = (rowData: GroupMember) => {
    return rowData.userId === curatorId ? (
      <Tag value={t('groups.curator')} severity="warning" />
    ) : (
      <Tag value={t('groups.member')} severity="info" />
    );
  };

  const dateBodyTemplate = (rowData: GroupMember) => {
    return new Date(rowData.joinedAt).toLocaleDateString();
  };

  const confirmRemoveMember = (member: GroupMember) => {
    confirmDialog({
      message: t('groups.confirm.remove', { username: member.username }),
      header: t('common.confirm'),
      icon: 'pi pi-exclamation-triangle',
      acceptClassName: 'p-button-danger',
      accept: () => removeMemberMutation.mutate(member.userId),
    });
  };

  const confirmSetCurator = (member: GroupMember) => {
    confirmDialog({
      message: t('groups.confirm.setCurator', { username: member.username }),
      header: t('common.confirm'),
      icon: 'pi pi-info-circle',
      accept: () => setCuratorMutation.mutate(member.userId),
    });
  };

  const actionBodyTemplate = (rowData: GroupMember) => {
    const isCurrentUser = rowData.userId === curatorId; // Check if the member is the current curator

    return (
      <div className="flex flex-wrap gap-2">
        {canManage && !isCurrentUser && ( // Only show remove button if canManage and not the current curator
          <Button
            icon="pi pi-trash"
            className="p-button-rounded p-button-text p-button-danger"
            onClick={() => confirmRemoveMember(rowData)}
            tooltip={t('common.remove')}
          />
        )}
        {canManage && rowData.userId !== curatorId && ( // Only show set curator if canManage and not already curator
          <Button
            label={t('groups.setCurator')}
            className="p-button-text"
            onClick={() => confirmSetCurator(rowData)}
          />
        )}
      </div>
    );
  };

  return (
    <DataTable value={members} paginator rows={10}
      sortField="username" sortOrder={1}
      responsiveLayout="scroll"
      emptyMessage={t('groups.noMembers')} // Assuming a translation key for no members
    >
      <Column field="username" header={t('settings.username')} sortable filter />
      <Column field="email" header={t('auth.email')} sortable filter />
      <Column field="groupRole" header={t('groups.table.role')} body={roleBodyTemplate} />
      <Column field="joinedAt" header={t('groups.table.joined')} body={dateBodyTemplate} sortable />
      {canManage && <Column body={actionBodyTemplate} header={t('common.actions')} style={{ width: '10rem' }} />}
    </DataTable>
  );
};

export default GroupMembersTable;
