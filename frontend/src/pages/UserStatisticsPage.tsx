import React from 'react';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore'; // To get current user's ID
import { useUserStatistics } from '../hooks/useStatistics';
import { UserQuizStatisticDto } from '../types/statistics';

const UserStatisticsPage = () => {
  const { t } = useTranslation();
  const { user } = useAuthStore(); // Get current logged-in user

  const { data: statistics, isLoading: isStatisticsLoading, isError: isStatisticsError } = useUserStatistics(user?.id);

  if (isStatisticsLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  const quizTypeBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return t(`quizType.${rowData.quizType}`);
  };

  const averageScoreBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return `${(rowData.averageScore * 100).toFixed(2)}%`;
  };

  const lastCompletedAtBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return new Date(rowData.lastCompletedAt).toLocaleString();
  };

  // Ensure statistics is an array before passing to DataTable
  const dataTableValue = Array.isArray(statistics) ? statistics : [];

  return (
    <div className="p-grid p-nogutter p-justify-center">
      <div className="p-col-12 p-md-10 p-lg-8">
        <Card className="p-shadow-2 mt-4">
          <div className="mt-5">
            <h3>{t('userProfile.quizStatistics')}</h3>
            {isStatisticsError ? (
              <div className="p-error">{t('userProfile.errorLoadingStatistics')}</div>
            ) : (
              <DataTable value={dataTableValue} emptyMessage={t('userProfile.noStatistics')}> {/* Corrected: Use dataTableValue */}
                <Column field="quizId" header={t('userProfile.quizId')} />
                <Column field="quizType" header={t('userProfile.quizType')} body={quizTypeBodyTemplate} />
                <Column field="totalSessions" header={t('userProfile.totalSessions')} />
                <Column field="totalQuestionsAnswered" header={t('userProfile.totalQuestionsAnswered')} />
                <Column field="totalCorrectAnswers" header={t('userProfile.totalCorrectAnswers')} />
                <Column field="totalScore" header={t('userProfile.totalScore')} />
                <Column field="averageScore" header={t('userProfile.averageScore')} body={averageScoreBodyTemplate} />
                <Column field="lastCompletedAt" header={t('userProfile.lastCompletedAt')} body={lastCompletedAtBodyTemplate} />
              </DataTable>
            )}
          </div>
        </Card>
      </div>
    </div>
  );
};

export default UserStatisticsPage;
