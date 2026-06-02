import React, { useRef } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { Button } from 'primereact/button';
import { Password } from 'primereact/password';
import { Card } from 'primereact/card';
import { useTranslation } from 'react-i18next';
import { useChangePassword } from '../hooks/useUser';
import { Toast } from 'primereact/toast';

const ChangePasswordPage = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);
  const changePasswordMutation = useChangePassword();

  const { control, handleSubmit, watch, reset, formState: { errors } } = useForm({
    defaultValues: { currentPassword: '', newPassword: '', confirmPassword: '' }
  });
  const newPassword = watch('newPassword');

  const onSubmit = (data) => {
    changePasswordMutation.mutate({ currentPassword: data.currentPassword, newPassword: data.newPassword }, {
      onSuccess: () => {
        toast.current?.show({ severity: 'success', summary: 'Success', detail: t('auth.passwordChanged'), life: 3000 });
        reset();
      },
      onError: (error: any) => {
        toast.current?.show({ severity: 'error', summary: 'Error', detail: error.response?.data?.message || t('auth.changePasswordError'), life: 3000 }); // Use translation key
      }
    });
  };

  return (
    <>
      <Toast ref={toast} />
      <Card title={t('auth.changePassword')} style={{ maxWidth: '40rem', margin: 'auto' }}>
        <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
          <div className="field">
            <span className="p-float-label">
              <Controller name="currentPassword" control={control}
                rules={{ required: t('validation.currentPasswordRequired') }} // Use translation key
                render={({ field, fieldState }) => <Password id={field.name} {...field} toggleMask className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="currentPassword">{t('auth.currentPassword')}</label>
            </span>
            {errors.currentPassword && <small className="p-error">{errors.currentPassword.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="newPassword" control={control}
                rules={{ required: t('validation.newPasswordRequired') }} // Use translation key
                render={({ field, fieldState }) => <Password id={field.name} {...field} toggleMask className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="newPassword">{t('auth.newPassword')}</label>
            </span>
            {errors.newPassword && <small className="p-error">{errors.newPassword.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="confirmPassword" control={control}
                rules={{ required: t('validation.confirmNewPasswordRequired'), validate: value => value === newPassword || t('validation.passwordsDoNotMatch') }} // Use translation key
                render={({ field, fieldState }) => <Password id={field.name} {...field} feedback={false} toggleMask className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="confirmPassword">{t('auth.confirmPassword')}</label>
            </span>
            {errors.confirmPassword && <small className="p-error">{errors.confirmPassword.message}</small>}
          </div>

          <Button type="submit" label={t('auth.changePassword')} className="mt-2" loading={changePasswordMutation.isLoading} />
        </form>
      </Card>
    </>
  );
};

export default ChangePasswordPage;
