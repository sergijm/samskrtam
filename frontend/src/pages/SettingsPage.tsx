import React, { useEffect, useRef, useState } from 'react';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useForm, Controller } from 'react-hook-form';
import { RadioButton } from 'primereact/radiobutton';
import { InputText } from 'primereact/inputtext';
import { Avatar } from 'primereact/avatar';
import { useThemeStore } from '../store/themeStore';
import { useLocaleStore } from '../store/localeStore';
import { useMe, useUpdateProfileDetails, useGenerateAvatarUploadUrl, useConfirmAvatarUpload } from '../hooks/useUser';
import { Toast } from 'primereact/toast';
import { UpdateProfilePayload } from '../types/user';
import axios from 'axios';

const SettingsPage = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const { data: userResponse, refetch: refetchUser } = useMe(); // Renamed 'user' to 'userResponse' for clarity
  const user = userResponse?.data; // Extract the actual user data from the Axios response

  // Log the user object to see what's being received
  console.log("User data from useMe():", userResponse);
  console.log("Extracted user data:", user);


  const updateProfileDetailsMutation = useUpdateProfileDetails();
  const generateUploadUrlMutation = useGenerateAvatarUploadUrl();
  const confirmAvatarUploadMutation = useConfirmAvatarUpload();

  const { control, handleSubmit, reset, formState: { errors } } = useForm<UpdateProfilePayload & { theme: string; locale: string }>({
    defaultValues: {
      username: '',
      firstName: '',
      lastName: '',
      theme: 'light',
      locale: 'ru',
    },
    enableReinitialize: true
  });

  const { theme, setTheme } = useThemeStore();
  const { locale, setLocale } = useLocaleStore();

  useEffect(() => {
    if (user) { // Now 'user' is the actual user data
      const valuesToReset = {
        username: user.username,
        firstName: user.firstName || '',
        lastName: user.lastName || '',
        theme: user.theme || 'light',
        locale: user.locale || 'ru'
      };
      console.log("Resetting form with values:", valuesToReset);
      reset(valuesToReset);
      setTheme(user.theme || 'light');
      setLocale(user.locale || 'ru');
    }
  }, [user, reset, setTheme, setLocale]);

  const onSubmit = (data: UpdateProfilePayload & { theme: string; locale: string }) => {
    const profileUpdateData: UpdateProfilePayload = {
      username: data.username,
      firstName: data.firstName,
      lastName: data.lastName,
    };

    updateProfileDetailsMutation.mutate(profileUpdateData, {
      onSuccess: () => {
        toast.current?.show({ severity: 'success', summary: 'Success', detail: t('settings.saved'), life: 3000 });
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
      // 1. Get presigned URL from backend
      const { uploadUrl, objectKey } = await generateUploadUrlMutation.mutateAsync(file.type);

      // 2. Upload file directly to MinIO
      await axios.put(uploadUrl, file, {
        headers: {
          'Content-Type': file.type,
        },
      });

      // 3. Confirm upload with backend
      await confirmAvatarUploadMutation.mutateAsync(objectKey);

      toast.current?.show({ severity: 'success', summary: 'Success', detail: t('settings.avatar.uploaded'), life: 3000 });
      refetchUser(); // Refetch user data to update avatar URL
    } catch (error) {
      console.error("Avatar upload error:", error);
      toast.current?.show({ severity: 'error', summary: 'Error', detail: t('settings.avatar.uploadError'), life: 3000 });
    } finally {
      // Reset file input to allow re-uploading the same file
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

            {/* Avatar Section */}
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

            {/* Username */}
            <div className="field mb-4 flex align-items-center">
              <label htmlFor="username" className="font-bold w-10rem mr-3">{t('settings.username')}</label>
              <div className="flex-grow-1">
                <Controller
                  name="username"
                  control={control}
                  rules={{ required: t('validation.required', { field: t('settings.username') }) }}
                  render={({ field, fieldState }) => (
                    <>
                      <InputText id={field.name} {...field} className={fieldState.invalid ? 'p-invalid' : ''} />
                      {fieldState.error && <small className="p-error">{fieldState.error.message}</small>}
                    </>
                  )}
                />
              </div>
            </div>

            {/* First Name */}
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

            {/* Last Name */}
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

            {/* Theme Settings (Frontend Managed) */}
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

            {/* Language Settings (Frontend Managed) */}
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