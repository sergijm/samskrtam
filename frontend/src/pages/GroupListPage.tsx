import React from 'react';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Card } from 'primereact/card';
import { useGroups } from '../hooks/useGroups';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { CreateButton } from '../components/common/buttons';

const GroupListPage = () => {
  const navigate = useNavigate();
  const { data: groups, isLoading } = useGroups();
  const { t } = useTranslation();

  const header = (
    <div className="flex justify-content-between align-items-center">
      <h5 className="m-0">{t('groups.title')}</h5>
      <CreateButton labelKey="groups.createGroup" onClick={() => navigate('/groups/new')} />
    </div>
  );

  if (isLoading) {
    return <ProgressSpinner />;
  }

  return (
    <Card>
      <DataTable value={groups} header={header} responsiveLayout="scroll"
        onRowClick={(e) => navigate(`/groups/${e.data.id}`)} selectionMode="single"
        className="p-datatable-clickable">
        <Column field="name" header={t('groups.groupName')} sortable />
        <Column field="curatorName" header={t('groups.curator')} sortable />
        <Column field="memberCount" header={t('groups.memberCount')} sortable />
        <Column field="createdAt" header={t('common.createdAt')} sortable body={(rowData) => new Date(rowData.createdAt).toLocaleDateString()} />
      </DataTable>
    </Card>
  );
};

export default GroupListPage;
