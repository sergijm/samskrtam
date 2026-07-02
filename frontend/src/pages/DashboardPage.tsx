import React from 'react';
import { Card } from 'primereact/card';
import { Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMe } from '../hooks/useUser';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';

export default function DashboardPage() {
  const { t } = useTranslation();
  const { data: user, isLoading, isError, error } = useMe();

  console.log('DashboardPage: Rendering...', { isLoading, isError, user });

  const learningModules = [
    { title: t('nav.grammar'), description: t('dashboard.grammarDescription'), icon: 'pi pi-book', link: '/grammar' },
    { title: t('nav.vocabulary'), description: t('dashboard.vocabularyDescription'), icon: 'pi pi-book', link: '/quizzes/vocabulary' },
    { title: t('nav.sangraha'), description: t('dashboard.sangrahaDescription'), icon: 'pi pi-bookmark', link: '/sangraha' },
    { title: t('nav.dictionary'), description: t('dashboard.dictionaryDescription'), icon: 'pi pi-book', link: '/dictionary' },
  ];

  const progressAndActivity = [
    { title: t('nav.statistics'), description: t('dashboard.statisticsDescription'), icon: 'pi pi-chart-line', link: '/statistics' },
    { title: t('nav.leaderboard'), description: t('dashboard.leaderboardDescription'), icon: 'pi pi-trophy', link: '/leaderboard' },
    { title: t('userProfile.quizSessions'), description: t('dashboard.quizSessionsDescription'), icon: 'pi pi-history', link: `/quiz-sessions` },
  ];

  const administration = [
    { title: t('nav.admin'), description: t('dashboard.adminDescription'), icon: 'pi pi-shield', link: '/admin' },
  ];

  const renderRow = (items: any[]) => (
    <div className="grid justify-content-center w-full mb-4">
      {items.map((item, index) => (
        <div key={index} className="col-12 sm:col-6 lg:col-3 p-2 flex">
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
  );

  if (isLoading) {
    console.log('DashboardPage: Showing loading spinner.');
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError) {
    console.log('DashboardPage: Showing error message.');
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('userProfile.errorLoadingUser', { message: error?.message })} />
      </div>
    );
  }

  console.log('DashboardPage: Rendering content.');
  return (
    <div className="flex flex-column align-items-center p-4">
      <h1 className="text-center mb-5">{t('nav.dashboard')}</h1>
      <div className="w-full" style={{ maxWidth: '1600px' }}>
        {renderRow(learningModules)}
        {renderRow(progressAndActivity)}
        {user?.roles.includes('ADMIN') && renderRow(administration)}
      </div>
    </div>
  );
}

