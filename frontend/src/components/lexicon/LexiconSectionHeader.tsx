import React from 'react';
import { useTranslation } from 'react-i18next';

interface LexiconSectionHeaderProps {
  titleKey: string;
  subtitleKey?: string;
  icon?: string;
  action?: React.ReactNode;
}

/** Заголовок блока страницы «Лексика»: иконка + заголовок + подзаголовок + действие. */
export const LexiconSectionHeader: React.FC<LexiconSectionHeaderProps> = ({
  titleKey,
  subtitleKey,
  icon,
  action,
}) => {
  const { t } = useTranslation();
  return (
    <div className="flex flex-column md:flex-row md:align-items-center md:justify-content-between gap-2 mb-3">
      <div>
        <div className="flex align-items-center gap-2">
          {icon && <i className={`pi ${icon} text-lg text-primary`} />}
          <h2 className="m-0 text-xl">{t(titleKey)}</h2>
        </div>
        {subtitleKey && <p className="m-0 text-sm text-500 mt-1">{t(subtitleKey)}</p>}
      </div>
      {action && <div className="flex-shrink-0">{action}</div>}
    </div>
  );
};
