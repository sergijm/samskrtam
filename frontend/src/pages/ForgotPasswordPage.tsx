import React, { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useTranslation } from 'react-i18next';

const ForgotPasswordPage = () => {
  const { t } = useTranslation();
  const [submitted, setSubmitted] = useState(false);
  const { control, handleSubmit, formState: { errors } } = useForm({ defaultValues: { email: '' } });

  const onSubmit = async (data) => {
    try {
      await authApi.forgotPassword(data.email);
    } catch (error) {
      // Per spec, we don't show specific errors here
    } finally {
      setSubmitted(true);
    }
  };

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <Card title={t('auth.forgotPassword')} style={{ width: '25rem' }}>
        {submitted ? (
          <div>
            <p>{t('auth.forgotPasswordSuccess')}</p>
            <Link to="/login">{t('auth.backToLogin')}</Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
            <div className="field">
              <span className="p-float-label">
                <Controller name="email" control={control}
                  rules={{ required: t('validation.emailRequired'), pattern: { value: /^\S+@\S+\.\S+$/, message: t('validation.invalidEmail') } }} // Use translation key and corrected regex
                  render={({ field, fieldState }) => <InputText id={field.name} {...field} autoFocus className={fieldState.error ? 'p-invalid' : ''} />} />
                <label htmlFor="email">{t('auth.email')}</label>
              </span>
              {errors.email && <small className="p-error">{errors.email.message}</small>}
            </div>
            <Button type="submit" label={t('auth.sendResetLink')} className="mt-2" />
          </form>
        )}
        {!submitted && (
          <div className="mt-4 text-center">
            <Link to="/login">{t('auth.backToLogin')}</Link>
          </div>
        )}
      </Card>
    </div>
  );
};

export default ForgotPasswordPage;

