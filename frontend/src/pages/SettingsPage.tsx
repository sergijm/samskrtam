import React, { useEffect, useRef, useState } from 'react';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useForm, Controller } from 'react-hook-form';
import { RadioButton } from 'primereact/radiobutton';
import { InputText } from 'primereact/inputtext';
import { Avatar } from 'primereact/avatar';
import { Dropdown } from 'primereact/dropdown'; // Import Dropdown
import { useThemeStore } from '../store/themeStore';
import { useLocaleStore } from '../store/localeStore';
import { useMe, useUpdateProfileDetails, useGenerateAvatarUploadUrl, useConfirmAvatarUpload } from '../hooks/useUser';
import { Toast } from 'primereact/toast';
import { UpdateProfilePayload } from '../types/user';
import axios from 'axios';
import { useQueryClient } from '@tanstack/react-query';

const SettingsPage = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const queryClient = useQueryClient();

  const { data: user } = useMe();

  const updateProfileDetailsMutation = useUpdateProfileDetails();
  const generateUploadUrlMutation = useGenerateAvatarUploadUrl();
  const confirmAvatarUploadMutation = useConfirmAvatarUpload();

  const { control, handleSubmit, reset, formState: { errors } } = useForm<UpdateProfilePayload & { theme: string; locale: string; quizSize: number }>({
    defaultValues: {
      username: '',
      firstName: '',
      lastName: '',
      theme: 'light',
      locale: 'ru',
      quizSize: 10,
    },
    enableReinitialize: true
  });

  const { theme, setTheme } = useThemeStore();
  const { locale, setLocale } = useLocaleStore();

  const quizSizeOptions = [
    { label: '5', value: 5 },
    { label: '10', value: 10 },
    { label: '15', value: 15 },
    { label: '20', value: 20 },
    { label: '30', value: 30 },
    { label: '50', value: 50 },
  ];

  useEffect(() => {
    if (user) {
      const valuesToReset = {
        username: user.username,
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        theme: user.theme || 'light',
        locale: user.locale || 'ru',
        quizSize: user.quizSize || 10,
      };
      reset(valuesToReset);
      setTheme(user.theme || 'light');
      setLocale(user.locale || 'ru');
    }
  }, [user, reset, setTheme, setLocale]);

  const onSubmit = (data: UpdateProfilePayload & { theme: string; locale: string; quizSize: number }) => {
    const profileUpdateData: UpdateProfilePayload = {
      username: data.username,
      firstName: data.firstName,
      lastName: data.lastName,
      quizSize: data.quizSize,
    };

    updateProfileDetailsMutation.mutate(profileUpdateData, {
      onSuccess: () => {
        toast.current?.show({ severity: 'success', summary: 'Success', detail: t('settings.saved'), life: 3000 });
        queryClient.invalidateQueries(['me']);
      },
      onError: (error) => {
        console.error("Profile update error:", error);
        toast.current?.show({ severity: 'error', summary: 'Error', detail: t('settings.saveError'), life: 3000 });
      }
    });

    setTheme(data.theme);
    setLocale(data.locale);
  };

  const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    if (!file.type.startsWith('image/')) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: t('settings.avatar.invalidFileType'), life: 3000 });
      return;
    }

    try {
      const { uploadUrl, objectKey } = await generateUploadUrlMutation.mutateAsync(file.type);

      await axios.put(uploadUrl, file, {
        headers: {
          'Content-Type': file.type,
        },
      });

      await confirmAvatarUploadMutation.mutateAsync(objectKey);

      toast.current?.show({ severity: 'success', summary: 'Success', detail: t('settings.avatar.uploaded'), life: 3000 });
      
      queryClient.invalidateQueries(['me']);
    } catch (error) {
      console.error("Avatar upload error:", error);
      toast.current?.show({ severity: 'error', summary: 'Error', detail: t('settings.avatar.uploadError'), life: 3000 });
    } finally {
      if (fileInputRef.current) {
        fileInputRef.current.value = '';
      }
    }
  };

  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };

  return (
    <>
      <Toast ref={toast} />
      <Card title={t('settings.title')}>
        <div className="max-w-40rem mx-auto">
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">

            <div className="field mb-4 flex align-items-center">
              <span className="font-bold w-10rem mr-3">{t('settings.avatar.title')}</span>
              <div className="flex-grow-1 flex align-items-center gap-3">
                <Avatar image={user?.avatarUrl} icon="pi pi-user" size="xlarge" shape="circle" />
                <input
                  type="file"
                  ref={fileInputRef}
                  onChange={handleFileChange}
                  accept="image/jpeg, image/png, image/webp"
                  style={{ display: 'none' }}
                />
                <Button
                  type="button"
                  label={t('settings.avatar.upload')}
                  icon="pi pi-upload"
                  onClick={triggerFileInput}
                  loading={generateUploadUrlMutation.isLoading || confirmAvatarUploadMutation.isLoading}
                  className="p-button-outlined"
                />
              </div>
            </div>

            <div className="field mb-4 flex align-items-center">
              <label htmlFor="username" className="font-bold w-10rem mr-3">{t('settings.username')}</label>
              <div className="flex-grow-1">
                <Controller
                  name="username"
                  control={control}
                  rules={{ required: t('validation.usernameRequired') }}
                  render={({ field, fieldState }) => (
                    <>
                      <InputText id={field.name} {...field} className={fieldState.invalid ? 'p-invalid' : ''} />
                      {fieldState.error && <small className="p-error">{fieldState.error.message}</small>}
                    </>
                  )}
                />
              </div>
            </div>

            <div className="field mb-4 flex align-items-center">
              <label htmlFor="firstName" className="font-bold w-10rem mr-3">{t('settings.firstName')}</label>
              <div className="flex-grow-1">
                <Controller
                  name="firstName"
                  control={control}
                  render={({ field }) => (
                    <InputText id={field.name} {...field} />
                  )}
                />
              </div>
            </div>

            <div className="field mb-4 flex align-items-center">
              <label htmlFor="lastName" className="font-bold w-10rem mr-3">{t('settings.lastName')}</label>
              <div className="flex-grow-1">
                <Controller
                  name="lastName"
                  control={control}
                  render={({ field }) => (
                    <InputText id={field.name} {...field} />
                  )}
                />
              </div>
            </div>

            <div className="field mb-4 flex align-items-center">
              <label htmlFor="quizSize" className="font-bold w-10rem mr-3">{t('settings.quizSize')}</label>
              <div className="flex-grow-1">
                <Controller
                  name="quizSize"
                  control={control}
                  render={({ field }) => (
                    <Dropdown id={field.name} {...field} options={quizSizeOptions} />
                  )}
                />
              </div>
            </div>

            <div className="flex align-items-center mb-4">
              <span className="font-bold w-10rem mr-3">{t('settings.theme')}</span>
              <div className="flex flex-wrap gap-3">
                <div className="field-radiobutton">
                  <Controller name="theme" control={control} render={({ field }) => (
                    <RadioButton inputId="theme-light" {...field} value="light" checked={field.value === 'light'} onChange={(e) => { field.onChange(e); setTheme('light'); }} />
                  )} />
                  <label htmlFor="theme-light" className="ml-2">{t('settings.themeLight')}</label>
                </div>
                <div className="field-radiobutton">
                  <Controller name="theme" control={control} render={({ field }) => (
                    <RadioButton inputId="theme-dark" {...field} value="dark" checked={field.value === 'dark'} onChange={(e) => { field.onChange(e); setTheme('dark'); }} />
                  )} />
                  <label htmlFor="theme-dark" className="ml-2">{t('settings.themeDark')}</label>
                </div>
              </div>
            </div>

            <div className="flex align-items-center mb-4">
              <span className="font-bold w-10rem mr-3">{t('settings.language')}</span>
              <div className="flex flex-wrap gap-3">
                <div className="field-radiobutton">
                  <Controller name="locale" control={control} render={({ field }) => (
                    <RadioButton inputId="locale-ru" {...field} value="ru" checked={field.value === 'ru'} onChange={(e) => { field.onChange(e); setLocale('ru'); }} />
                  )} />
                  <label htmlFor="locale-ru" className="ml-2">{t('settings.languageRu')}</label>
                </div>
                <div className="field-radiobutton">
                  <Controller name="locale" control={control} render={({ field }) => (
                    <RadioButton inputId="locale-en" {...field} value="en" checked={field.value === 'en'} onChange={(e) => { field.onChange(e); setLocale('en'); }} />
                  )} />
                  <label htmlFor="locale-en" className="ml-2">{t('settings.languageEn')}</label>
                </div>
              </div>
            </div>

            <div className="mt-4 flex flex-column gap-3">
              <Button type="submit" label={t('settings.save')} loading={updateProfileDetailsMutation.isLoading} className="max-w-15rem" />
              <Link to="/settings/password" className="p-button p-button-text max-w-15rem">{t('auth.changePassword')}</Link>
            </div>
          </form>
        </div>
      </Card>
    </>
  );
};

export default SettingsPage;
