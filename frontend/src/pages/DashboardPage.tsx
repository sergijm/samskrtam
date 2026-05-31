import React from 'react';
import { Card } from 'primereact/card';
import { Button } from 'primereact/button';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function DashboardPage() {
  const { t } = useTranslation();

  const dashboardItems = [
    { title: t('nav.quizzes'), description: t('dashboard.quizzesDescription'), icon: 'pi pi-question-circle', link: '/quizzes' },
    { title: t('nav.dictionary'), description: t('dashboard.dictionaryDescription'), icon: 'pi pi-book', link: '/dictionary' },
    { title: t('nav.statistics'), description: t('dashboard.statisticsDescription'), icon: 'pi pi-chart-line', link: '/statistics' },
    { title: t('nav.leaderboard'), description: t('dashboard.leaderboardDescription'), icon: 'pi pi-trophy', link: '/leaderboard' },
    { title: t('nav.settings'), description: t('dashboard.settingsDescription'), icon: 'pi pi-cog', link: '/settings' },
    { title: t('nav.admin'), description: t('dashboard.adminDescription'), icon: 'pi pi-shield', link: '/admin' },
  ];

  return (
    <div className="flex flex-column align-items-center justify-content-center min-h-screen p-4"> {/* Adjusted main container */}
      <h1 className="text-center mb-5">{t('nav.dashboard')}</h1>
      <div className="grid justify-content-center w-full" style={{ maxWidth: '1200px' }}> {/* Using PrimeFlex grid */}
        {dashboardItems.map((item, index) => (
          <div key={index} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2"> {/* Responsive columns */}
            <Link to={item.link} className="no-underline h-full flex"> {/* Ensure Link takes full height */}
              <Card
                title={item.title}
                subTitle={item.description}
                className="dashboard-card flex flex-column align-items-center justify-content-between text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200"
              >
                <div className="flex flex-column align-items-center justify-content-center flex-grow-1"> {/* Content takes available space */}
                  <i className={`${item.icon} text-5xl mb-3`} />
                  <p className="text-sm text-color-secondary">{item.description}</p> {/* Moved description here */}
                </div>
                <Button label={t('common.go')} icon="pi pi-arrow-right" iconPos="right" className="p-button-text mt-3" />
              </Card>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
