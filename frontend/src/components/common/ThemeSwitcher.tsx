import React from 'react';
import { InputSwitch } from 'primereact/inputswitch';
import { useThemeStore } from '../../store/themeStore';
import { useTranslation } from 'react-i18next';

export const ThemeSwitcher = () => {
  const { theme, setTheme } = useThemeStore();
  const { t } = useTranslation();

  return (
    <div className="flex align-items-center gap-2">
      <i className="pi pi-sun" />
      <InputSwitch
        checked={theme === 'dark'}
        onChange={(e) => setTheme(e.value ? 'dark' : 'light')}
        aria-label={t('settings.toggleTheme')}
      />
      <i className="pi pi-moon" />
    </div>
  );
};
