import React, { useEffect } from 'react';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useForm, Controller } from 'react-hook-form';
import { RadioButton } from 'primereact/radiobutton';
import { useThemeStore } from '../store/themeStore';
import { useLocaleStore } from '../store/localeStore';
import { useMe, useUpdateMe } from '../hooks/useUser';
import { Toast } from 'primereact/toast';
import { useRef } from 'react';

const SettingsPage = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);
  const { data: user } = useMe();
  const updateMeMutation = useUpdateMe();

  const { control, handleSubmit, reset } = useForm({
    defaultValues: {
      theme: 'light',
      locale: 'ru',
    }
  });

  const { theme, setTheme } = useThemeStore();
  const { locale, setLocale } = useLocaleStore();

  useEffect(() => {
    if (user) {
      reset({ theme: user.theme, locale: user.locale });
      setTheme(user.theme);
      setLocale(user.locale);
    }
  }, [user, reset, setTheme, setLocale]);

  const onSubmit = (data) => {
    updateMeMutation.mutate(data, {
      onSuccess: () => {
        toast.current?.show({ severity: 'success', summary: 'Success', detail: t('settings.saved'), life: 3000 });
      },
      onError: () => {
        toast.current?.show({ severity: 'error', summary: 'Error', detail: 'Failed to save settings.', life: 3000 });
      }
    });
  };

  return (
    <>
      <Toast ref={toast} />
      <Card title={t('settings.title')}>
        <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
          <h5>{t('settings.theme')}</h5>
          <div className="field-radiobutton">
            <Controller name="theme" control={control} render={({ field }) => (
              <RadioButton inputId="theme-light" {...field} value="light" checked={field.value === 'light'} onChange={(e) => { field.onChange(e); setTheme('light'); }} />
            )} />
            <label htmlFor="theme-light">{t('settings.themeLight')}</label>
          </div>
          <div className="field-radiobutton">
            <Controller name="theme" control={control} render={({ field }) => (
              <RadioButton inputId="theme-dark" {...field} value="dark" checked={field.value === 'dark'} onChange={(e) => { field.onChange(e); setTheme('dark'); }} />
            )} />
            <label htmlFor="theme-dark">{t('settings.themeDark')}</label>
          </div>

          <h5 className="mt-4">{t('settings.language')}</h5>
          <div className="field-radiobutton">
            <Controller name="locale" control={control} render={({ field }) => (
              <RadioButton inputId="locale-ru" {...field} value="ru" checked={field.value === 'ru'} onChange={(e) => { field.onChange(e); setLocale('ru'); }} />
            )} />
            <label htmlFor="locale-ru">Русский</label>
          </div>
          <div className="field-radiobutton">
            <Controller name="locale" control={control} render={({ field }) => (
              <RadioButton inputId="locale-en" {...field} value="en" checked={field.value === 'en'} onChange={(e) => { field.onChange(e); setLocale('en'); }} />
            )} />
            <label htmlFor="locale-en">English</label>
          </div>

          <div className="mt-4">
            <Button type="submit" label={t('settings.save')} loading={updateMeMutation.isLoading} />
            <Link to="/settings/password" className="p-button p-button-text ml-2">{t('auth.changePassword')}</Link>
          </div>
        </form>
      </Card>
    </>
  );
};

export default SettingsPage;
