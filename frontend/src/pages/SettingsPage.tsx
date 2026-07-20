import { useEffect, useRef, useState } from 'react';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useForm } from 'react-hook-form';
import { useThemeStore } from '../store/themeStore';
import { useLocaleStore } from '../store/localeStore';
import { useMe, useUpdateProfileDetails, useGenerateAvatarUploadUrl, useConfirmAvatarUpload } from '../hooks/useUser';
import { Toast } from 'primereact/toast';
import { UpdateProfilePayload } from '../types/user';
import axios from 'axios';
import { useQueryClient } from '@tanstack/react-query';

import AvatarUploadSection from '../components/settings/AvatarUploadSection';
import ProfileFieldsSection from '../components/settings/ProfileFieldsSection';
import PreferencesSection from '../components/settings/PreferencesSection';
import { SubmitButton } from '../components/common/buttons';

interface FormValues {
  username: string;
  firstName: string;
  lastName: string;
  quizSize: number;
  theme: string;
  locale: string;
}

const SettingsPage = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);
  const queryClient = useQueryClient();

  const { data: user } = useMe();
  const updateProfileDetailsMutation = useUpdateProfileDetails();
  const generateUploadUrlMutation = useGenerateAvatarUploadUrl();
  const confirmAvatarUploadMutation = useConfirmAvatarUpload();

  const { theme, setTheme } = useThemeStore();
  const { locale, setLocale } = useLocaleStore();

  const { control, handleSubmit } = useForm<FormValues>({
    defaultValues: {
      username: user?.username || '',
      firstName: user?.firstName || '',
      lastName: user?.lastName || '',
      quizSize: user?.quizSize || 10,
      theme: theme || 'light',
      locale: locale || 'ru',
    },
    enableReinitialize: true,
  });

  const [uploading, setUploading] = useState(false);

  const handleFileUpload = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: t('settings.avatar.invalidFileType'), life: 3000 });
      return;
    }
    try {
      setUploading(true);
      const { uploadUrl, objectKey } = await generateUploadUrlMutation.mutateAsync(file.type);
      await axios.put(uploadUrl, file, { headers: { 'Content-Type': file.type } });
      await confirmAvatarUploadMutation.mutateAsync(objectKey);
      toast.current?.show({ severity: 'success', summary: 'Success', detail: t('settings.avatar.uploaded'), life: 3000 });
      queryClient.invalidateQueries({ queryKey: ['me'] });
    } catch {
      toast.current?.show({ severity: 'error', summary: 'Error', detail: t('settings.avatar.uploadError'), life: 3000 });
    } finally {
      setUploading(false);
    }
  };

  const onSubmit = (data: FormValues) => {
    const profileData: UpdateProfilePayload = {
      username: data.username,
      firstName: data.firstName,
      lastName: data.lastName,
      quizSize: data.quizSize,
    };
    updateProfileDetailsMutation.mutate(profileData, {
      onSuccess: () => {
        toast.current?.show({ severity: 'success', summary: 'Success', detail: t('settings.saved'), life: 3000 });
        queryClient.invalidateQueries({ queryKey: ['me'] });
      },
      onError: () => {
        toast.current?.show({ severity: 'error', summary: 'Error', detail: t('settings.saveError'), life: 3000 });
      },
    });
    setTheme(data.theme);
    setLocale(data.locale);
  };

  // Sync user preferences on load
  useEffect(() => {
    if (user) {
      setTheme(user.theme || 'light');
      setLocale(user.locale || 'ru');
    }
  }, [user, setTheme, setLocale]);

  return (
    <>
      <Toast ref={toast} />
      <Card title={t('settings.title')}>
        <div className="max-w-40rem mx-auto">
          <form onSubmit={handleSubmit(onSubmit)} className="p-fluid">
            <AvatarUploadSection
              avatarUrl={user?.avatarUrl}
              onFileChange={handleFileUpload}
              isLoading={uploading}
            />
            <ProfileFieldsSection control={control} />
            <PreferencesSection
              control={control}
              onThemeChange={setTheme}
              onLocaleChange={setLocale}
            />
            <div className="mt-4 flex flex-column gap-3">
                            <SubmitButton
                labelKey="settings.save"
                loading={updateProfileDetailsMutation.isPending}
                className="max-w-15rem"
              />
              <Link to="/settings/password" className="p-button p-button-text max-w-15rem">
                {t('auth.changePassword')}
              </Link>
            </div>
          </form>
        </div>
      </Card>
    </>
  );
};

export default SettingsPage;

