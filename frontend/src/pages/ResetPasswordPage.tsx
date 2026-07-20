import React, { useState, useEffect } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { InputText } from 'primereact/inputtext';
import { Card } from 'primereact/card';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { authApi } from '../api/authApi';
import { useTranslation } from 'react-i18next';
import { Password } from 'primereact/password';
import { classNames } from 'primereact/utils';
import { PageButton } from '../components/common/buttons';

const ResetPasswordPage = () => {
  const { t } = useTranslation();
  const location = useLocation();
  const navigate = useNavigate();
  const [resetStatus, setResetStatus] = useState<'idle' | 'success' | 'error'>('idle');
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const { control, handleSubmit, getValues, formState: { errors } } = useForm({
    defaultValues: {
      newPassword: '',
      confirmPassword: ''
    }
  });

  const token = new URLSearchParams(location.search).get('token');

  useEffect(() => {
    if (!token) {
      setErrorMessage(t('resetPassword.noToken'));
      setResetStatus('error');
    }
  }, [token, t]);

  const onSubmit = async (data) => {
    if (!token) {
      setErrorMessage(t('resetPassword.noToken'));
      setResetStatus('error');
      return;
    }

    try {
      await authApi.resetPassword(token, data.newPassword);
      setResetStatus('success');
    } catch (error: any) {
      setResetStatus('error');
      if (error.response && error.response.data && error.response.data.message) {
        setErrorMessage(error.response.data.message);
      } else {
        setErrorMessage(t('resetPassword.genericError'));
      }
    }
  };

  const getFormErrorMessage = (name) => {
    return errors[name] && <small className="p-error">{errors[name].message}</small>;
  };

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <Card title={t('resetPassword.title')} style={{ width: '25rem' }}>
        {resetStatus === 'success' ? (
          <div>
            <p>{t('resetPassword.successMessage')}</p>
            <Link to="/login">{t('auth.backToLogin')}</Link>
          </div>
        ) : resetStatus === 'error' ? (
          <div>
            <p className="p-error">{errorMessage || t('resetPassword.genericError')}</p>
            <Link to="/forgot-password">{t('resetPassword.tryAgain')}</Link>
            <br />
            <Link to="/login">{t('auth.backToLogin')}</Link>
          </div>
        ) : (
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
            <div className="field">
              <span className="p-float-label">
                <Controller
                  name="newPassword"
                  control={control}
                  rules={{
                    required: t('validation.passwordRequired'),
                    minLength: { value: 8, message: t('validation.passwordMinLength', { length: 8 }) }
                  }}
                  render={({ field, fieldState }) => (
                    <Password
                      id={field.name}
                      {...field}
                      toggleMask
                      className={classNames({ 'p-invalid': fieldState.invalid })}
                    />
                  )}
                />
                <label htmlFor="newPassword" className={classNames({ 'p-error': errors.newPassword })}>{t('resetPassword.newPassword')}</label>
              </span>
              {getFormErrorMessage('newPassword')}
            </div>

            <div className="field">
              <span className="p-float-label">
                <Controller
                  name="confirmPassword"
                  control={control}
                  rules={{
                    required: t('validation.confirmPasswordRequired'),
                    validate: (value) => value === getValues('newPassword') || t('validation.passwordsMismatch')
                  }}
                  render={({ field, fieldState }) => (
                    <Password
                      id={field.name}
                      {...field}
                      toggleMask
                      className={classNames({ 'p-invalid': fieldState.invalid })}
                    />
                  )}
                />
                <label htmlFor="confirmPassword" className={classNames({ 'p-error': errors.confirmPassword })}>{t('resetPassword.confirmPassword')}</label>
              </span>
              {getFormErrorMessage('confirmPassword')}
            </div>

            <PageButton variant="form-submit" labelKey="resetPassword.resetButton" className="mt-2" />
          </form>
        )}
      </Card>
    </div>
  );
};

export default ResetPasswordPage;

