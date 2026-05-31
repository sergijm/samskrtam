import React, { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useTranslation } from 'react-i18next';

const RegisterPage = () => {
  const { t } = useTranslation();
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const { control, handleSubmit, watch, formState: { errors } } = useForm({
    defaultValues: { username: '', email: '', password: '', confirmPassword: '' }
  });
  const password = watch('password');

  const onSubmit = async (data) => {
    try {
      await authApi.register(data.username, data.email, data.password);
      setSuccess(true);
    } catch (err) {
      setError(err.response?.data?.message || 'Registration failed.');
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
                rules={{ required: 'Username is required.' }}
                render={({ field, fieldState }) => <InputText id={field.name} {...field} className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="username">{t('auth.username')}</label>
            </span>
            {errors.username && <small className="p-error">{errors.username.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="email" control={control}
                rules={{ required: 'Email is required.', pattern: { value: /\\S+@\\S+\\.\\S+/, message: 'Invalid email.' } }}
                render={({ field, fieldState }) => <InputText id={field.name} {...field} className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="email">{t('auth.email')}</label>
            </span>
            {errors.email && <small className="p-error">{errors.email.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="password" control={control}
                rules={{ required: 'Password is required.' }}
                render={({ field, fieldState }) => <Password id={field.name} {...field} toggleMask className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="password">{t('auth.password')}</label>
            </span>
            {errors.password && <small className="p-error">{errors.password.message}</small>}
          </div>

          <div className="field">
            <span className="p-float-label">
              <Controller name="confirmPassword" control={control}
                rules={{ required: 'Please confirm your password.', validate: value => value === password || 'Passwords do not match.' }}
                render={({ field, fieldState }) => <Password id={field.name} {...field} feedback={false} toggleMask className={fieldState.error ? 'p-invalid' : ''} />} />
              <label htmlFor="confirmPassword">{t('auth.confirmPassword')}</label>
            </span>
            {errors.confirmPassword && <small className="p-error">{errors.confirmPassword.message}</small>}
          </div>

          {error && <div className="p-error mb-2">{error}</div>}

          <Button type="submit" label={t('auth.register')} className="mt-2" />
        </form>
        <div className="mt-4 text-center">
          <Link to="/login">{t('auth.backToLogin')}</Link>
        </div>
      </Card>
    </div>
  );
};

export default RegisterPage;
