import React, { useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Card } from 'primereact/card';
import { useNavigate, useParams } from 'react-router-dom';
import { useGroup, useRenameGroup } from '../hooks/useGroups';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';

const GroupEditPage = () => {
  const { id } = useParams<{ id: string }>();
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: group, isLoading: isLoadingGroup } = useGroup(id!);
  const renameGroupMutation = useRenameGroup(id!);

  const { control, handleSubmit, reset, formState: { errors } } = useForm({ defaultValues: { name: '' } });

  useEffect(() => {
    if (group) {
      reset({ name: group.name });
    }
  }, [group, reset]);

  const onSubmit = (data) => {
    renameGroupMutation.mutate(data.name, {
      onSuccess: () => {
        navigate(`/groups/${id}`);
      },
    });
  };

  if (isLoadingGroup) {
    return <ProgressSpinner />;
  }

  return (
    <Card title={t('groups.editGroup')}>
      <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
        <div className="field">
          <span className="p-float-label">
            <Controller name="name" control={control}
              rules={{ required: 'Group name is required.' }}
              render={({ field, fieldState }) => <InputText id={field.name} {...field} autoFocus className={fieldState.error ? 'p-invalid' : ''} />} />
            <label htmlFor="name">{t('groups.groupName')}</label>
          </span>
          {errors.name && <small className="p-error">{errors.name.message}</small>}
        </div>

        <div className="mt-4">
          <Button type="submit" label={t('common.save')} loading={renameGroupMutation.isLoading} />
          <Button label={t('common.cancel')} className="p-button-text" onClick={() => navigate(`/groups/${id}`)} />
        </div>
      </form>
    </Card>
  );
};

export default GroupEditPage;
