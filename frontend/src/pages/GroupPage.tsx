import React, { useState, useEffect, useRef } from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { Card } from 'primereact/card';
import { Button } from 'primereact/button';
import { ProgressSpinner } from 'primereact/progressspinner';
import { InputText } from 'primereact/inputtext'; // Import InputText
import { useForm, Controller } from 'react-hook-form'; // Import useForm and Controller
import { useGroup, useRenameGroup } from '../hooks/useGroups';
import { useAuthStore } from '../store/authStore';
import { useTranslation } from 'react-i18next';
import GroupMembersTable from '../components/group/GroupMembersTable';
import AddMemberDialog from '../components/group/AddMemberDialog';
import { ConfirmDialog, confirmDialog } from 'primereact/confirmdialog'; // Import confirmDialog
import { Toast } from 'primereact/toast'; // Import Toast

const GroupPage = () => {
  const { id } = useParams<{ id: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);

  const { data: group, isLoading, isError, refetch } = useGroup(id!);
  const currentUser = useAuthStore((state) => state.user);
  const renameGroupMutation = useRenameGroup(id!);

  const [addMemberDialogVisible, setAddMemberDialogVisible] = useState(false);
  const [isEditingName, setIsEditingName] = useState(false);

  const { control, handleSubmit, reset, formState: { errors } } = useForm({
    defaultValues: { name: '' }
  });

  useEffect(() => {
    if (group) {
      reset({ name: group.name });
    }
  }, [group, reset]);

  if (isLoading) {
    return <ProgressSpinner />;
  }

  if (isError || !group) {
    return <div>Error loading group or group not found.</div>;
  }

  // Updated canManage logic to check for 'ADMIN' role in roles array
  const canManage = currentUser?.roles.includes('ADMIN') || currentUser?.id === group.curatorId;
  const isCurator = currentUser?.id === group.curatorId;

  const onRenameSubmit = (data: { name: string }) => {
    renameGroupMutation.mutate(data.name, {
      onSuccess: () => {
        toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('groups.renameSuccess'), life: 3000 });
        setIsEditingName(false);
        refetch(); // Refetch group data to update the name
      },
      onError: (error) => {
        console.error("Failed to rename group:", error);
        toast.current?.show({ severity: 'error', summary: t('common.error'), detail: t('groups.renameError'), life: 3000 });
      }
    });
  };

  const header = (
    <div className="flex justify-content-between align-items-center">
      {isEditingName ? (
        <form onSubmit={handleSubmit(onRenameSubmit)} className="p-fluid flex-grow-1 flex align-items-center gap-2">
          <Controller name="name" control={control}
            rules={{ required: t('validation.groupNameRequired') }}
            render={({ field, fieldState }) => (
              <InputText id={field.name} {...field} autoFocus className={fieldState.error ? 'p-invalid' : ''} />
            )} />
          <Button icon="pi pi-check" className="p-button-text p-button-success" type="submit" loading={renameGroupMutation.isLoading} />
          <Button icon="pi pi-times" className="p-button-text p-button-danger" onClick={() => setIsEditingName(false)} />
        </form>
      ) : (
        <h2 className="m-0">{group.name}</h2>
      )}
      {canManage && !isEditingName && (
        <div className="flex gap-2">
          <Button label={t('groups.addMember')} icon="pi pi-plus" onClick={() => setAddMemberDialogVisible(true)} />
          <Button icon="pi pi-pencil" className="p-button-text" onClick={() => setIsEditingName(true)} tooltip={t('common.edit')} />
        </div>
      )}
    </div>
  );

  return (
    <>
      <Toast ref={toast} />
      <ConfirmDialog />
      <Card header={header}>
        <div className="mb-4">
          <strong>{t('groups.curator')}: </strong>
          <Link to={`/users/${group.curatorId}`}>{group.curatorName}</Link>
        </div>
        <GroupMembersTable groupId={group.id} members={group.members} curatorId={group.curatorId} canManage={canManage} isCurator={isCurator} />
      </Card>
      {canManage && (
        <AddMemberDialog
          groupId={group.id}
          visible={addMemberDialogVisible}
          onHide={() => setAddMemberDialogVisible(false)}
          onMemberAdded={() => refetch()} // Refetch group data after member is added
        />
      )}
    </>
  );
};

export default GroupPage;
