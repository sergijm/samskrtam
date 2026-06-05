import React, { useState, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { InputText } from 'primereact/inputtext';
import { Dropdown } from 'primereact/dropdown';
import { Button } from 'primereact/button';
import { Tag } from 'primereact/tag';
import { Paginator, PaginatorPageChangeEvent } from 'primereact/paginator';
import { useAdminUsers } from '../hooks/useAdmin'; // Will create this hook
import { UserRole } from '../types/user'; // Assuming UserRole is defined here
import { Toast } from 'primereact/toast';
import { Link } from 'react-router-dom';

const AdminUsersPage = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortDirection, setSortDirection] = useState('desc');
  const [search, setSearch] = useState('');
  const [roleFilter, setRoleFilter] = useState<UserRole | null>(null);
  const [blockedFilter, setBlockedFilter] = useState<boolean | null>(null);

  const { data, isLoading, isError, error } = useAdminUsers(
    page,
    size,
    sortBy,
    sortDirection,
    search,
    roleFilter,
    blockedFilter
  );

  const onPageChange = (event: PaginatorPageChangeEvent) => {
    setPage(event.page);
    setSize(event.rows);
  };

  const onSort = (event: any) => {
    setSortBy(event.sortField);
    setSortDirection(event.sortOrder === 1 ? 'asc' : 'desc');
  };

  const roleOptions = [
    { label: t('admin.users.role.all'), value: null },
    { label: t('admin.users.role.student'), value: UserRole.STUDENT },
    { label: t('admin.users.role.admin'), value: UserRole.ADMIN },
  ];

  const blockedOptions = [
    { label: t('admin.users.blocked.all'), value: null },
    { label: t('admin.users.blocked.true'), value: true },
    { label: t('admin.users.blocked.false'), value: false },
  ];

  const statusBodyTemplate = (rowData: any) => {
    return <Tag value={rowData.blocked ? t('admin.users.blocked') : t('admin.users.active')} severity={rowData.blocked ? 'danger' : 'success'} />;
  };

  const actionsBodyTemplate = (rowData: any) => {
    return (
      <div className="flex gap-2">
        <Link to={`/admin/users/${rowData.id}`} className="p-button p-button-sm p-button-text">
          {t('common.edit')}
        </Link>
        {/* Block/Unblock buttons will be added here later */}
      </div>
    );
  };

  if (isError) {
    toast.current?.show({ severity: 'error', summary: 'Error', detail: error?.message || t('admin.users.fetchError'), life: 3000 });
  }

  return (
    <div className="max-w-60rem mx-auto">
      <Toast ref={toast} />
      <Card title={t('admin.users.title')} className="mb-4">
        <div className="flex flex-wrap gap-3 mb-4">
          <InputText
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder={t('admin.users.searchPlaceholder')}
            className="flex-grow-1"
          />
          <Dropdown
            value={roleFilter}
            options={roleOptions}
            onChange={(e) => setRoleFilter(e.value)}
            placeholder={t('admin.users.filterByRole')}
            className="w-12rem"
          />
          <Dropdown
            value={blockedFilter}
            options={blockedOptions}
            onChange={(e) => setBlockedFilter(e.value)}
            placeholder={t('admin.users.filterByStatus')}
            className="w-12rem"
          />
          <Button icon="pi pi-filter-slash" className="p-button-outlined" onClick={() => {
            setSearch('');
            setRoleFilter(null);
            setBlockedFilter(null);
          }} />
        </div>

        <DataTable
          value={data?.users}
          lazy
          paginator={false}
          first={page * size}
          rows={size}
          totalRecords={data?.totalElements}
          onSort={onSort}
          sortField={sortBy}
          sortOrder={sortDirection === 'asc' ? 1 : -1}
          loading={isLoading}
          emptyMessage={t('admin.users.noUsersFound')}
        >
          <Column field="id" header="ID" sortable />
          <Column field="username" header={t('settings.username')} sortable />
          <Column field="email" header={t('auth.email')} sortable />
          <Column field="firstName" header={t('settings.firstName')} sortable />
          <Column field="lastName" header={t('settings.lastName')} sortable />
          <Column field="roles" header={t('admin.users.role.column-title')} sortable /> {/* Changed header key */}
          <Column field="blocked" header={t('admin.users.status')} body={statusBodyTemplate} sortable />
          <Column field="createdAt" header={t('common.createdAt')} sortable />
          <Column body={actionsBodyTemplate} header={t('common.actions')} style={{ width: '10rem' }} />
        </DataTable>

        <Paginator
          first={page * size}
          rows={size}
          totalRecords={data?.totalElements}
          onPageChange={onPageChange}
          rowsPerPageOptions={[10, 20, 50]}
          template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
          currentPageReportTemplate="{first}-{last} of {totalRecords}"
        />
      </Card>
    </div>
  );
};

export default AdminUsersPage;
