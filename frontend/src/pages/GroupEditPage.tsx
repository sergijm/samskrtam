import React, { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { InputText } from 'primereact/inputtext';
import { Card } from 'primereact/card';
import { useNavigate, useParams } from 'react-router-dom';
import { useGroup, useRenameGroup } from '../hooks/useGroups';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { SubmitButton, CancelButton } from '../components/common/buttons';

const GroupEditPage = () => {
  const { id } = useParams<{ id: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: group, isLoading: isLoadingGroup, isError: isErrorGroup, error: errorGroup } = useGroup(id!);
  const renameGroupMutation = useRenameGroup(id!);

  const { control, handleSubmit, reset, formState: { errors } } = useForm({ defaultValues: { name: '' } });

  useEffect(() => {
    if (group) {
      reset({ name: group.name });
    }
  }, [group, reset]);

  const onSubmit = (data: { name: string }) => {
    renameGroupMutation.mutate(data.name, {
      onSuccess: () => {
        navigate(`/groups/${id}`);
      },
      onError: (error) => {
        console.error("Failed to rename group:", error);
        // Optionally show a toast or message for the error
      }
    });
  };

  if (isLoadingGroup) {
    return <ProgressSpinner />;
  }

  if (isErrorGroup) {
    return <Message severity="error" text={`Error loading group: ${errorGroup?.message}`} />;
  }

  if (!group) {
    return <Message severity="info" text="Group not found." />;
  }

  return (
    <Card title={t('groups.editGroup')}>
      <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
        <div className="field">
          <span className="p-float-label">
            <Controller name="name" control={control}
              rules={{ required: t('validation.groupNameRequired') }}
              render={({ field, fieldState }) => <InputText id={field.name} {...field} autoFocus className={fieldState.error ? 'p-invalid' : ''} />} />
            <label htmlFor="name">{t('groups.groupName')}</label>
          </span>
          {errors.name && <small className="p-error">{errors.name.message}</small>}
        </div>

                <div className="mt-4">
          <SubmitButton labelKey="common.save" loading={renameGroupMutation.isLoading} />
          <CancelButton labelKey="common.cancel" className="ml-2" onClick={() => navigate(`/groups/${id}`)} />
        </div>
      </form>
    </Card>
  );
};

export default GroupEditPage;
