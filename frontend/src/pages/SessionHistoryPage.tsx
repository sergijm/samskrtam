import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag';
import { useAuthStore } from '../store/authStore';
import { useSessionAnswerHistory } from '../hooks/useUserQuizSessions';
import { AnswerHistory } from '../types/quiz';

const SessionHistoryPage = () => {
  const { t } = useTranslation();
  const { sessionId } = useParams<{ sessionId: string }>();
  const { user } = useAuthStore();
  const userId = user?.id;

  // Removed useState for page, size, sortBy, sortDirection as pagination is removed
  // The useSessionAnswerHistory hook now also doesn't take these parameters
  const { data: answers, isLoading, isError, error } = useSessionAnswerHistory(
    sessionId || '',
    userId || ''
  );

  if (!sessionId || !userId) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory')} />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory', { message: error?.message })} />
      </div>
    );
  }

  // Data is now directly the list of answers, not a paginated object
  const sessionAnswers = answers || [];

  const resultBodyTemplate = (rowData: AnswerHistory) => {
    if (rowData.correct === undefined || rowData.correct === null) {
      return null; // Or a specific indicator for unanswered
    }
    return (
      <Tag
        value={rowData.correct ? t('common.correct') : t('common.incorrect')}
        severity={rowData.correct ? 'success' : 'danger'}
      />
    );
  };

  const answeredAtBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.answeredAt ? new Date(rowData.answeredAt).toLocaleString() : null;
  };

  const explanationBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.explanation || t('quiz.noExplanation');
  };

  const selectedAnswerBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.selectedAnswerIast || t('sessionHistory.notAnswered');
  };

  const correctOptionBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.correctOptionIast || t('sessionHistory.notApplicable');
  };

  return (
    <div className="max-w-60rem mx-auto">
      <Card title={t('sessionHistory.title', { sessionId: sessionId })} className="mb-4">
        <DataTable
          value={sessionAnswers} // Use the direct list of answers
          sortMode="single" // Enable single column sorting
          emptyMessage={t('sessionHistory.noAnswersFound')}
          // Removed pagination related props: lazy, paginator, first, rows, totalRecords
        >
          <Column field="questionText" header={t('sessionHistory.question')} sortable />
          <Column field="selectedAnswerIast" header={t('sessionHistory.yourAnswer')} body={selectedAnswerBodyTemplate} sortable />
          <Column field="correctOptionIast" header={t('sessionHistory.correctAnswer')} body={correctOptionBodyTemplate} sortable />
          <Column field="correct" header={t('sessionHistory.result')} body={resultBodyTemplate} sortable />
          <Column field="responseTimeMs" header={t('sessionHistory.responseTime')} sortable />
          <Column field="answeredAt" header={t('sessionHistory.answeredAt')} body={answeredAtBodyTemplate} sortable />
          <Column field="explanation" header={t('sessionHistory.explanation')} body={explanationBodyTemplate} sortable />
        </DataTable>

        {/* Removed Paginator component */}
      </Card>
    </div>
  );
};

export default SessionHistoryPage;