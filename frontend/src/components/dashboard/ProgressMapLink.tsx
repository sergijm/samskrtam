import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { PageButton } from '../common/buttons';

/**
 * Блок «Карта прогресса» (§5.5 IA) — ссылка на полноэкранную диагностику.
 *
 * Сама карта (radar-диаграмма) — отдельная задача (task-progress-map),
 * не входит в этот дашборд. Здесь только кнопка перехода на /progress-map.
 *
 * Пока страницы /progress-map нет — показываем «скоро».
 */
const ProgressMapLink: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const handleClick = () => {
    navigate('/progress-map');
  };

  return (
    <Card className="dashboard-progress-map-link mb-4 fade-in">
      <div className="flex flex-column md:flex-row align-items-center justify-content-between gap-3 p-2">
        <div className="flex flex-column gap-1">
          <div className="flex align-items-center gap-2">
            <i className="pi pi-chart-radar text-2xl text-primary" />
            <h3 className="m-0 text-lg">{t('dashboard.progressMapTitle')}</h3>
          </div>
          <p className="m-0 text-500 text-sm">{t('dashboard.progressMapDesc')}</p>
        </div>
        <PageButton
          variant="navigation"
          labelKey="dashboard.progressMapButton"
          onClick={handleClick}
          disabled
          title={t('dashboard.progressMapComingSoon')}
        />
      </div>
    </Card>
  );
};

export default React.memo(ProgressMapLink);
