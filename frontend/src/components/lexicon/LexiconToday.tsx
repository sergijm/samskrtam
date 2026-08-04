import React from 'react';
import { useTranslation } from 'react-i18next';
import { Toast } from 'primereact/toast';
import { LexiconToday as LexiconTodayData } from '../../types/lexicon';
import { useLexiconToast } from './useLexiconToast';

interface LexiconTodayProps {
  today: LexiconTodayData;
}

/** Блок «Сегодня» — что делать сейчас: повторение, новые слова, слабые. */
const LexiconToday: React.FC<LexiconTodayProps> = ({ today }) => {
  const { t } = useTranslation();
  const { toast, showComingSoon } = useLexiconToast();

  const stats = [
    { id: 'review', icon: 'pi pi-clock', value: today.reviewDue, label: t('lexicon.todayReview') },
    { id: 'new', icon: 'pi pi-plus', value: today.newWords, label: t('lexicon.todayNew') },
    { id: 'weak', icon: 'pi pi-exclamation-circle', value: today.weakWords, label: t('lexicon.todayWeak') },
  ];

  return (
    <>
      <Toast ref={toast} />
      <div className="lexicon-card lexicon-today mb-4">
        <div className="flex flex-column md:flex-row align-items-center justify-content-between gap-3">
          <div className="flex flex-column flex-1 w-full">
            <div className="flex align-items-center gap-2 mb-3">
              <i className="pi pi-calendar text-lg text-primary" />
              <h2 className="m-0 text-xl">{t('lexicon.todayTitle')}</h2>
              <span className="text-sm text-500 ml-2">{t('lexicon.todaySubtitle')}</span>
            </div>
            <div className="grid">
              {stats.map((stat) => (
                <div key={stat.id} className="col-12 sm:col-6 md:col-4">
                  <div className="lexicon-stat">
                    <i className={`${stat.icon} lexicon-stat-icon`} />
                    <span className="lexicon-stat-value">{stat.value}</span>
                    <span className="text-sm text-500">{stat.label}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
          <button type="button" className="lexicon-cta" onClick={() => showComingSoon()}>
            {t('lexicon.startReviewCta')}
            <i className="pi pi-arrow-right" />
          </button>
        </div>
      </div>
    </>
  );
};

export default React.memo(LexiconToday);
