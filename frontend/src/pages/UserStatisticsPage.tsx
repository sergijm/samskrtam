import React, { useState } from 'react'; // Import useState
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag'; // Import Tag for resultBodyTemplate
import { useTranslation } from 'react-i18next';
import { useAuthStore } from '../store/authStore';
import { useUserStatistics } from '../hooks/useStatistics';
import { UserQuizStatisticDto } from '../types/statistics';
import { AnswerHistory } from '../types/quiz'; // Import AnswerHistory

const UserStatisticsPage = () => {
  const { t } = useTranslation();
  const { user } = useAuthStore();

  const [expandedRows, setExpandedRows] = useState(null); // State for expanded rows

  // The useUserStatistics hook now returns PaginatedResponse
  const { data: paginatedStatistics, isLoading: isStatisticsLoading, isError: isStatisticsError } = useUserStatistics(
    user?.id,
    0, // page
    10, // size
    'lastCompletedAt', // sortBy
    'desc' // sortDirection
  );

  if (isStatisticsLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  const statistics = paginatedStatistics?.content || []; // Extract content from paginated response

  const quizTypeBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return t(`quizType.${rowData.quizType}`);
  };

  const averageScoreBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return `${(rowData.averageScore * 100).toFixed(2)}%`;
  };

  const lastCompletedAtBodyTemplate = (rowData: UserQuizStatisticDto) => {
    return new Date(rowData.lastCompletedAt).toLocaleString();
  };

  // Body templates for nested DataTable (AnswerHistory)
  const answerHistoryResultBodyTemplate = (rowData: AnswerHistory) => {
    if (rowData.isCorrect === undefined || rowData.isCorrect === null) {
      return null;
    }
    return (
      <Tag
        value={rowData.isCorrect ? t('common.correct') : t('common.incorrect')}
        severity={rowData.isCorrect ? 'success' : 'danger'}
      />
    );
  };

  const answerHistoryAnsweredAtBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.answeredAt ? new Date(rowData.answeredAt).toLocaleString() : null;
  };

  const answerHistoryExplanationBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.explanation || t('quiz.noExplanation');
  };

  const answerHistorySelectedAnswerBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.selectedAnswerIast || t('sessionHistory.notAnswered');
  };

  const answerHistoryCorrectOptionBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.correctOptionIast || t('sessionHistory.notApplicable');
  };

  // Row expansion template
  const rowExpansionTemplate = (data: UserQuizStatisticDto) => {
    if (!data.answerHistoryJson) {
      return <p>{t('userProfile.noAnswerHistory')}</p>;
    }

    let answerHistory: AnswerHistory[] = [];
    try {
      answerHistory = JSON.parse(data.answerHistoryJson);
    } catch (e) {
      console.error('Error parsing answer history JSON:', e);
      return <p>{t('userProfile.errorParsingAnswerHistory')}</p>;
    }

    return (
      <div className="p-3">
        <h5>{t('userProfile.answerHistoryForQuiz', { quizId: data.quizId })}</h5>
        <DataTable value={answerHistory} emptyMessage={t('sessionHistory.noAnswersFound')}>
          <Column field="questionText" header={t('sessionHistory.question')} />
          <Column field="selectedAnswerIast" header={t('sessionHistory.yourAnswer')} body={answerHistorySelectedAnswerBodyTemplate} />
          <Column field="correctOptionIast" header={t('sessionHistory.correctAnswer')} body={answerHistoryCorrectOptionBodyTemplate} />
          <Column field="isCorrect" header={t('sessionHistory.result')} body={answerHistoryResultBodyTemplate} />
          <Column field="responseTimeMs" header={t('sessionHistory.responseTime')} />
          <Column field="answeredAt" header={t('sessionHistory.answeredAt')} body={answerHistoryAnsweredAtBodyTemplate} />
          <Column field="explanation" header={t('sessionHistory.explanation')} body={answerHistoryExplanationBodyTemplate} />
        </DataTable>
      </div>
    );
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
              expandedRows={expandedRows}
              onRowToggle={(e) => setExpandedRows(e.data)}
              rowExpansionTemplate={rowExpansionTemplate}
              dataKey="id" // Assuming 'id' is unique for each statistic entry
            >
              <Column expander style={{ width: '3rem' }} />
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
  );
};

export default UserStatisticsPage;
