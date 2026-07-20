import React, { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useTranslation } from 'react-i18next';
import { PageButton } from '../components/common/buttons';

const RegisterPage = () => {
  const { t } = useTranslation();
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { control, handleSubmit, watch, formState: { errors } } = useForm({
    defaultValues: { username: '', email: '', password: '', confirmPassword: '' }
  });
  const password = watch('password');

  const onSubmit = async (data: any) => {
    try {
      await authApi.register(data.username, data.email, data.password);
      setSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.message || t('auth.registerError'));
    }
  };

  if (success) {
    return (
      <div className="flex justify-content-center align-items-center h-screen">
        <Card title={t('auth.registerSuccessTitle')}>
          <p>{t('auth.registerSuccessMessage')}</p>
          <Link to="/login">{t('auth.backToLogin')}</Link>
        </Card>
      </div>
    );
  }

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <Card title={t('auth.register')} style={{ width: '25rem' }}>
        <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
          <div className="field">
            <span className="p-float-label">
              <Controller name="username" control={control}
                rules={{ required: t('validation.usernameRequired') }}
                render={({ field, fieldState }) => <InputText id={field.name} {...field} className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="username">{t('auth.username')}</label>
            </span>
            {errors.username && <small className="p-error">{errors.username.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="email" control={control}
                rules={{ required: t('validation.emailRequired'), pattern: { value: /^\S+@\S+\.\S+$/, message: t('validation.invalidEmail') } }}
                render={({ field, fieldState }) => <InputText id={field.name} {...field} className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="email">{t('auth.email')}</label>
            </span>
            {errors.email && <small className="p-error">{errors.email.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="password" control={control}
                rules={{ required: t('validation.passwordRequired') }}
                render={({ field, fieldState }) => <Password id={field.name} {...field} toggleMask className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="password">{t('auth.password')}</label>
            </span>
            {errors.password && <small className="p-error">{errors.password.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="confirmPassword" control={control}
                rules={{ required: t('validation.confirmPasswordRequired'), validate: value => value === password || t('validation.passwordsDoNotMatch') }}
                render={({ field, fieldState }) => <Password id={field.name} {...field} feedback={false} toggleMask className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="confirmPassword">{t('auth.confirmPassword')}</label>
            </span>
            {errors.confirmPassword && <small className="p-error">{errors.confirmPassword.message}</small>}
          </div>

          {error && <div className="p-error mb-2">{error}</div>}

          <PageButton variant="form-submit" labelKey="auth.register" className="mt-2" />
        </form>
        <div className="mt-4 text-center">
          <Link to="/login">{t('auth.backToLogin')}</Link>
        </div>
      </Card>
    </div>
  );
};

export default RegisterPage;

