import React from 'react';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Card } from 'primereact/card';
import { useGroups } from '../hooks/useGroups';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { CreateButton } from '../components/common/buttons';

export default function AdminGroupsPage() {
  const navigate = useNavigate();
  const { data: groups, isLoading, isError, error } = useGroups(); // Fetch groups
  const { t } = useTranslation();

  const header = (
    <div className="flex justify-content-between align-items-center">
      <h5 className="m-0">{t('groups.title')}</h5>
      <CreateButton labelKey="groups.createGroup" onClick={() => navigate('/groups/new')} />
    </div>
  );

  if (isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('common.fetchError', { message: error?.message })} />
      </div>
    );
  }

  // Ensure groups is an array before passing to DataTable
  const groupsData = groups || [];

  return (
    <div className="flex flex-column align-items-center p-4">
      <Card title={header} className="w-full" style={{ maxWidth: '1600px' }}>
        <DataTable value={groupsData} responsiveLayout="scroll" // Use groupsData here
          onRowClick={(e) => navigate(`/groups/${e.data.id}`)} selectionMode="single"
          className="p-datatable-clickable">
          <Column field="name" header={t('groups.groupName')} sortable />
          <Column field="curatorName" header={t('groups.curator')} sortable />
          <Column field="memberCount" header={t('groups.memberCount')} sortable />
          <Column field="createdAt" header={t('common.createdAt')} sortable body={(rowData) => new Date(rowData.createdAt).toLocaleDateString()} />
        </DataTable>
      </Card>
    </div>
  );
}
