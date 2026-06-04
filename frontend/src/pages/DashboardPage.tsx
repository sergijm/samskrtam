import React from 'react';
import { Card } from 'primereact/card';
import { Button } from 'primereact/button';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

export default function DashboardPage() {
  const { t } = useTranslation();

  const dashboardItems = [
    { title: t('nav.grammar'), description: t('dashboard.grammarDescription'), icon: 'pi pi-book', link: '/quizzes/grammar' },
    { title: t('nav.vocabulary'), description: t('dashboard.vocabularyDescription'), icon: 'pi pi-book', link: '/quizzes/vocabulary' },
    { title: t('nav.dictionary'), description: t('dashboard.dictionaryDescription'), icon: 'pi pi-book', link: '/dictionary' },
    { title: t('nav.statistics'), description: t('dashboard.statisticsDescription'), icon: 'pi pi-chart-line', link: '/statistics' },
    { title: t('nav.leaderboard'), description: t('dashboard.leaderboardDescription'), icon: 'pi pi-trophy', link: '/leaderboard' },
    { title: t('nav.settings'), description: t('dashboard.settingsDescription'), icon: 'pi pi-cog', link: '/settings' },
    { title: t('nav.admin'), description: t('dashboard.adminDescription'), icon: 'pi pi-shield', link: '/admin/users' }, // Updated link to /admin/users
  ];

  return (
    <div className="flex flex-column align-items-center p-4"> {/* Removed justify-content-center and min-h-screen */}
      <h1 className="text-center mb-5">{t('nav.dashboard')}</h1>
      <div className="grid justify-content-center w-full" style={{ maxWidth: '1200px' }}>
        {dashboardItems.map((item, index) => (
          <div key={index} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex"> {/* Added flex to make items align */}
            <Link to={item.link} className="no-underline h-full flex w-full"> {/* Added w-full to make link take full width */}
              <Card
                title={item.title}
                subTitle={item.description}
                className="dashboard-card flex flex-column align-items-center justify-content-between text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full" // Added w-full
              >
                <div className="flex flex-column align-items-center justify-content-center flex-grow-1">
                  <i className={`${item.icon} text-5xl mb-3`} />
                  <p className="text-sm text-color-secondary">{item.description}</p>
                </div>
                {/* Removed the Button here */}
              </Card>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
