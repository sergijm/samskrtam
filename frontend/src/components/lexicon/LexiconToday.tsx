import React from 'react';
import { useTranslation } from 'react-i18next';
import { LexiconToday as LexiconTodayData } from '../../types/lexicon';

interface LexiconTodayProps {
  today: LexiconTodayData;
}

/** Блок «Сегодня» — что делать сейчас: повторение, новые слова, слабые. */
const LexiconToday: React.FC<LexiconTodayProps> = ({ today }) => {
  const { t } = useTranslation();

  const stats = [
    { id: 'review', icon: 'pi pi-clock', value: today.reviewDue, label: t('lexicon.todayReview') },
    { id: 'new', icon: 'pi pi-plus', value: today.newWords, label: t('lexicon.todayNew') },
    { id: 'weak', icon: 'pi pi-exclamation-circle', value: today.weakWords, label: t('lexicon.todayWeak') },
  ];

  return (
    <div className="lexicon-today mb-4">
      <div className="flex flex-column md:flex-row align-items-center justify-content-between gap-3">
        <div className="flex flex-column flex-1 w-full">
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
      </div>
    </div>
  );
};

export default React.memo(LexiconToday);
