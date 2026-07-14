import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { Card } from 'primereact/card';
import { Skeleton } from 'primereact/skeleton';
import type { DashboardContinueCta } from '../../types/dashboard';

interface ContinueCtaProps {
  data: DashboardContinueCta | undefined;
  isLoading: boolean;
  isError: boolean;
}

/**
 * Главный CTA дашборда — «Продолжить».
 *
 * §5.1 IA: следующий урок курикулума ИЛИ SRS-очередь на сегодня.
 * Приоритет: если оба доступны — CURRICULUM_NEXT > SRS_QUEUE (решение Оркестратора).
 * Если ни одного — кнопка-заглушка.
 */
const ContinueCta: React.FC<ContinueCtaProps> = ({ data, isLoading, isError }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  if (isLoading) {
    return (
      <Card className="dashboard-continue-cta mb-4">
        <div className="flex flex-column gap-2">
          <Skeleton width="60%" height="1.5rem" />
          <Skeleton width="80%" height="2.5rem" />
        </div>
      </Card>
    );
  }

  // Пока backend не готов — «скоро», не фиктивная заглушка
  if (isError || !data || data.source === 'NONE') {
    return (
      <Card className="dashboard-continue-cta mb-4 fade-in">
        <div className="flex flex-column align-items-center gap-3 p-3">
          <i className="pi pi-clock text-4xl text-500" />
          <h3 className="m-0 text-600">{t('dashboard.continueComingSoon')}</h3>
          <p className="m-0 text-500 text-center">{t('dashboard.continueComingSoonDesc')}</p>
        </div>
      </Card>
    );
  }

  const handleClick = () => {
    if (data.route) {
      navigate(data.route);
    }
  };

  const label =
    data.source === 'SRS_QUEUE'
      ? t('dashboard.continueSrsLabel', { count: data.srsDueCount ?? 0 })
      : t('dashboard.continueLabel');

  return (
    <Card className="dashboard-continue-cta mb-4 fade-in">
      <div className="flex flex-column md:flex-row align-items-center justify-content-between gap-3">
        <div className="flex flex-column gap-1">
          <span className="text-sm text-500 font-medium">{t('dashboard.continueSubtitle')}</span>
          <h2 className="m-0 text-xl md:text-2xl">{label}</h2>
          {data.titleKey && (
            <span className="text-600">{data.titleKey}</span>
          )}
        </div>
        <Button
          label={t('dashboard.continueButton')}
          icon="pi pi-play"
          iconPos="right"
          className="p-button-lg p-button-primary"
          onClick={handleClick}
        />
      </div>
    </Card>
  );
};

export default React.memo(ContinueCta);
