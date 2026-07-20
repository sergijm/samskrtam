import React, { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Card } from 'primereact/card';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi';
import { useTranslation } from 'react-i18next';
import { LocaleSwitcher } from '../components/common/LocaleSwitcher';
import { ThemeSwitcher } from '../components/common/ThemeSwitcher';
import { Divider } from 'primereact/divider';
import { PageButton, CtaButton } from '../components/common/buttons';

const LoginPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { login, redirectPath, setRedirectPath } = useAuthStore();
  const [error, setError] = useState<string | null>(null);
  const [showPasswordLoginForm, setShowPasswordLoginForm] = useState(false);

  const { control, handleSubmit, formState: { errors } } = useForm({
    defaultValues: { username: '', password: '' }
  });

  const onSubmit = async (data) => {
    try {
      console.log("LoginPage: Attempting ROPC login...");
      const authResponse = await authApi.login(data.username, data.password);
      console.log("LoginPage: authApi.login response:", authResponse);

      const tokens = {
        accessToken: authResponse.accessToken,
        refreshToken: authResponse.refreshToken,
      };

      console.log("LoginPage: Tokens received:", tokens);
      localStorage.setItem('accessToken', tokens.accessToken || '');
      localStorage.setItem('refreshToken', tokens.refreshToken || '');

      console.log("LoginPage: Fetching user details with accessToken:", localStorage.getItem('accessToken'));
      const userResponse = await userApi.getMe();
      const user = userResponse.data;
      console.log("LoginPage: User details received:", user);

      login(tokens, user);

      const targetPath = redirectPath || '/dashboard';
      setRedirectPath(null);
      navigate(targetPath);
    } catch (err) {
      console.error("Login error:", err);
      setError(t('auth.error'));
    }
  };

  return (
    <div className="flex justify-content-center align-items-center h-screen">
      <Card title={t('auth.login')} style={{ width: '25rem' }}>
        <div className="flex justify-content-between mb-4">
          <LocaleSwitcher />
          <ThemeSwitcher />
        </div>

        {!showPasswordLoginForm ? (
          <div className="flex flex-column gap-3">
            <PageButton variant="cta-primary" labelKey="auth.google" iconName="pi-google" className="p-button-outlined" onClick={authApi.loginWithGoogle} />
            <PageButton variant="cta-primary" labelKey="auth.mailru" iconName="pi-envelope" className="p-button-outlined" onClick={authApi.loginWithMailRu} />
            <Divider align="center" className="my-2">
              <span>{t('common.or')}</span>
            </Divider>
            <PageButton variant="navigation" labelKey="auth.loginWithPassword" iconName="pi-user" className="p-button-secondary" onClick={() => setShowPasswordLoginForm(true)} />
          </div>
        ) : (
          <>
            <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
              <div className="field">
                <span className="p-float-label">
                  <Controller name="username" control={control}
                    rules={{ required: t('validation.usernameRequired') }}
                    render={({ field, fieldState }) => (
                      <InputText id={field.name} {...field} autoFocus className={fieldState.error ? 'p-invalid' : ''} />
                    )} />
                  <label htmlFor="username">{t('auth.emailOrLogin')}</label>
                </span>
                {errors.username && <small className="p-error">{errors.username.message}</small>}
              </div>

              <div className="field">
                <span className="p-float-label">
                  <Controller name="password" control={control}
                    rules={{ required: t('validation.passwordRequired') }}
                    render={({ field, fieldState }) => (
                      <Password id={field.name} {...field} feedback={false} toggleMask className={fieldState.error ? 'p-invalid' : ''} />
                    )} />
                  <label htmlFor="password">{t('auth.password')}</label>
                </span>
                {errors.password && <small className="p-error">{errors.password.message}</small>}
              </div>

              {error && <div className="p-error mb-2">{error}</div>}

              <PageButton variant="form-submit" labelKey="auth.login" className="mt-2" />
            </form>

            <div className="mt-3 text-center">
              <Link to="/register">{t('auth.registerLink')}</Link> | <Link to="/forgot-password">{t('auth.forgotPasswordLink')}</Link>
            </div>
            <div className="mt-3 text-center">
              <PageButton variant="form-cancel" labelKey="common.back" iconName="pi-arrow-left" className="p-button-sm" onClick={() => setShowPasswordLoginForm(false)} />
            </div>
          </>
        )}
      </Card>
    </div>
  );
};

export default LoginPage;