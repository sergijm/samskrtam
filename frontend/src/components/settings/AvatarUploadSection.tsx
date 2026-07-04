import React, { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { Avatar } from 'primereact/avatar';

interface AvatarUploadSectionProps {
  avatarUrl?: string;
  onFileChange: (file: File) => void;
  isLoading: boolean;
}

export default function AvatarUploadSection({ avatarUrl, onFileChange, isLoading }: AvatarUploadSectionProps) {
  const { t } = useTranslation();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      onFileChange(file);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  return (
    <div className="field mb-4 flex align-items-center">
      <span className="font-bold w-10rem mr-3">{t('settings.avatar.title')}</span>
      <div className="flex-grow-1 flex align-items-center gap-3">
        <Avatar image={avatarUrl} icon="pi pi-user" size="xlarge" shape="circle" />
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
          onClick={() => fileInputRef.current?.click()}
          loading={isLoading}
          className="p-button-outlined"
        />
      </div>
    </div>
  );
}