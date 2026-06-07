import React from 'react';
import { Card } from 'primereact/card';
import { Button } from 'primereact/button';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore'; // Import useAuthStore

export default function DashboardPage() {
  const { t } = useTranslation();
  const { user } = useAuthStore(); // Get user from auth store

  const baseDashboardItems = [
    { title: t('nav.grammar'), description: t('dashboard.grammarDescription'), icon: 'pi pi-book', link: '/quizzes/grammar' },
    { title: t('nav.vocabulary'), description: t('dashboard.vocabularyDescription'), icon: 'pi pi-book', link: '/quizzes/vocabulary' },
    { title: t('nav.dictionary'), description: t('dashboard.dictionaryDescription'), icon: 'pi pi-book', link: '/dictionary' },
    { title: t('nav.statistics'), description: t('dashboard.statisticsDescription'), icon: 'pi pi-chart-line', link: '/statistics' }, // Updated link to /statistics
    { title: t('nav.leaderboard'), description: t('dashboard.leaderboardDescription'), icon: 'pi pi-trophy', link: '/leaderboard' },
    { title: t('userProfile.quizSessions'), description: t('dashboard.quizSessionsDescription'), icon: 'pi pi-history', link: `/quiz-sessions` }, // New tile, updated link
  ];

  // Admin tile now links to /admin, which will be the AdminHomePage
  const adminDashboardItem = { title: t('nav.admin'), description: t('dashboard.adminDescription'), icon: 'pi pi-shield', link: '/admin' };

  // Filter dashboard items based on user roles
  const dashboardItems = user?.roles.includes('ADMIN')
    ? [...baseDashboardItems, adminDashboardItem]
    : baseDashboardItems;

  return (
    <div className="flex flex-column align-items-center p-4">
      <h1 className="text-center mb-5">{t('nav.dashboard')}</h1>
      <div className="grid justify-content-center w-full" style={{ maxWidth: '1200px' }}>
        {dashboardItems.map((item, index) => (
          <div key={index} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
            <Link to={item.link} className="no-underline h-full flex w-full">
              <Card
                title={item.title}
                subTitle={item.description}
                className="dashboard-card flex flex-column align-items-center justify-content-between text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
              >
                <div className="flex flex-column align-items-center justify-content-center flex-grow-1">
                  <i className={`${item.icon} text-5xl mb-3`} />
                  <p className="text-sm text-color-secondary">{item.description}</p>
                </div>
              </Card>
            </Link>
          </div>
        ))}
      </div>
    </div>
  );
}
