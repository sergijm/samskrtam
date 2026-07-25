import React from 'react';
import { SelectButton } from 'primereact/selectbutton';
import { useLocaleStore } from '../../store/localeStore';

export const LocaleSwitcher = () => {
  const { locale, setLocale } = useLocaleStore();

  const options = [
    { label: 'RU', value: 'ru' },
    { label: 'EN', value: 'en' },
  ];

  return (
    <SelectButton
      value={locale}
      onChange={(e) => e.value && setLocale(e.value)}
      options={options}
      aria-label="Language"
      pt={{ button: { className: 'p-button-sm' } }}
    />
  );
};

