import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { Toast } from 'primereact/toast';

/**
 * Общий toast для mock-переходов на странице «Лексика».
 * Пока бэкенда нет — клики показывают «Модуль в разработке».
 */
export const useLexiconToast = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);

  const showComingSoon = (label?: string) => {
    toast.current?.show({
      severity: 'info',
      summary: t('lexicon.underConstruction'),
      detail: label ?? t('lexicon.comingSoon'),
      life: 2500,
    });
  };

  return { toast, showComingSoon };
};
