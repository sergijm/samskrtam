import React, { useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { Button } from 'primereact/button';
import { InputText } from 'primereact/inputtext';
import { Password } from 'primereact/password';
import { Card } from 'primereact/card';
import { Link, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../store/authStore';
import { authApi } from '../api/authApi';
import { userApi } from '../api/userApi'; // Import userApi
import { useTranslation } from 'react-i18next';
import { LocaleSwitcher } from '../components/common/LocaleSwitcher';
import { ThemeSwitcher } from '../components/common/ThemeSwitcher';
import { Divider } from 'primereact/divider';

const LoginPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const login = useAuthStore((state) => state.login);
  const [error, setError] = useState<string | null>(null);

  const { control, handleSubmit, formState: { errors } } = useForm({
    defaultValues: { username: '', password: '' }
  });

  const onSubmit = async (data) => {
    try {
      // 1. Authenticate and get tokens
      console.log("LoginPage: Attempting ROPC login...");
      const authResponse = await authApi.login(data.username, data.password);
      console.log("LoginPage: authApi.login response:", authResponse);

      const tokens = {
        accessToken: authResponse.accessToken,
        refreshToken: authResponse.refreshToken,
      };

      console.log("LoginPage: Tokens received:", tokens);
      localStorage.setItem('accessToken', tokens.accessToken || ''); // Ensure it's stored for the next call
      localStorage.setItem('refreshToken', tokens.refreshToken || ''); // Ensure it's stored

      // 2. Fetch user details using the new access token
      console.log("LoginPage: Fetching user details with accessToken:", localStorage.getItem('accessToken'));
      const userResponse = await userApi.getMe();
      const user = userResponse.data;
      console.log("LoginPage: User details received:", user);

      // 3. Log in the user with tokens and full user object
      login(tokens, user);
      navigate('/dashboard'); // Navigate to dashboard after successful login
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
                rules={{ required: 'Password is required.' }}
                render={({ field, fieldState }) => (
                  <Password id={field.name} {...field} feedback={false} toggleMask className={fieldState.error ? 'p-invalid' : ''} />
                )} />
              <label htmlFor="password">{t('auth.password')}</label>
            </span>
            {errors.password && <small className="p-error">{errors.password.message}</small>}
          </div>

          {error && <div className="p-error mb-2">{error}</div>}

          <Button type="submit" label={t('auth.login')} className="mt-2" />
        </form>
        
        <div className="mt-3 text-center">
          <Link to="/register">{t('auth.registerLink')}</Link> | <Link to="/forgot-password">{t('auth.forgotPasswordLink')}</Link>
        </div>

        <Divider align="center" className="my-4">
          <span>{t('common.or')}</span>
        </Divider>

        <div className="flex flex-column gap-2">
          <Button label={t('auth.google')} icon="pi pi-google" className="p-button-outlined" onClick={authApi.loginWithGoogle} />
          <Button label={t('auth.mailru')} icon="pi pi-envelope" className="p-button-outlined" onClick={authApi.loginWithMailRu} />
        </div>
      </Card>
    </div>
  );
};

export default LoginPage;
