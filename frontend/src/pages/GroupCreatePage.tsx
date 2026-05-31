import React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Card } from 'primereact/card';
import { useNavigate } from 'react-router-dom';
import { useCreateGroup } from '../hooks/useGroups';
import { useTranslation } from 'react-i18next';

const GroupCreatePage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const createGroupMutation = useCreateGroup();

  const { control, handleSubmit, formState: { errors } } = useForm({ defaultValues: { name: '' } });

  const onSubmit = (data) => {
    createGroupMutation.mutate(data.name, {
      onSuccess: (newGroup) => {
        navigate(`/groups/${newGroup.id}`);
      },
    });
  };

  return (
    <Card title={t('groups.createGroup')}>
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
          <Button type="submit" label={t('common.create')} loading={createGroupMutation.isLoading} />
          <Button label={t('common.cancel')} className="p-button-text" onClick={() => navigate('/groups')} />
        </div>
      </form>
    </Card>
  );
};

export default GroupCreatePage;
