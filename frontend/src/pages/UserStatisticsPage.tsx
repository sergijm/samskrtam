import React, { useState } from 'react';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { useTranslation } from 'react-i18next';
import { useMe } from '../hooks/useUser';
import { useUserStatistics } from '../hooks/useStatistics';
import { UserQuizStatisticDto } from '../types/statistics';

const UserStatisticsPage = () => {
  const { t } = useTranslation();
  const { data: user, isLoading: isUserLoading } = useMe();

  const { data: paginatedStatistics, isLoading: isStatisticsLoading, isError: isStatisticsError } = useUserStatistics(
    user?.id,
    0,
    10,
    'lastCompletedAt',
    'desc'
  );

  if (isUserLoading || isStatisticsLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  const statistics = paginatedStatistics?.content || [];

  const quizTypeBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return t(`lessonType.${rowData.lessonType}`);
  };

  const averageScoreBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return `${(rowData.averageScore * 100).toFixed(2)}%`;
  };

  const lastCompletedAtBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return new Date(rowData.lastCompletedAt).toLocaleString();
  };

  return (
    <div className="flex justify-content-center">
      <Card className="w-full max-w-60rem mt-4">
        <div className="mt-5">
          <h3>{t('userProfile.quizStatistics')}</h3>
          {isStatisticsError ? (
            <div className="p-error">{t('userProfile.errorLoadingStatistics')}</div>
          ) : (
            <DataTable
              value={statistics}
              emptyMessage={t('userProfile.noStatistics')}
              dataKey="id"
            >
              <Column field="quizId" header={t('userProfile.quizId')} />
              <Column field="lessonType" header={t('userProfile.lessonType')} body={quizTypeBodyTemplate} />
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
  );
};

export default UserStatisticsPage;
