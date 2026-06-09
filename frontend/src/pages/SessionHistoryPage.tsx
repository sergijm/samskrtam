import React, { useState } from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom'; // Import useLocation and useNavigate
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag';
import { Button } from 'primereact/button'; // Import Button
import { useAuthStore } from '../store/authStore';
import { useSessionAnswerHistory } from '../hooks/useUserQuizSessions';
import { useCompleteQuizSession } from '../hooks/useQuiz'; // Import useCompleteQuizSession
import { AnswerHistory, QuizType } from '../types/quiz'; // Import QuizType

const SessionHistoryPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate(); // Initialize useNavigate
  const { sessionId } = useParams<{ sessionId: string }>();
  const location = useLocation(); // Get location object
  const { quizType } = (location.state || {}) as { quizType?: QuizType }; // Extract quizType from location.state
  const { user } = useAuthStore();
  const userId = user?.id;

  const { data: answers, isLoading, isError, error } = useSessionAnswerHistory(
    sessionId || '',
    userId || ''
  );

  const completeSessionMutation = useCompleteQuizSession(); // Initialize the mutation

  const handleCompleteQuiz = () => {
    if (sessionId && quizType) {
      completeSessionMutation.mutate(
        { sessionId, quizType },
        {
          onSuccess: () => {
            // Optionally, navigate to another page or show a success message
            navigate('/dashboard'); // Example: navigate to dashboard
          },
          onError: (err) => {
            console.error('Failed to complete quiz session:', err);
            // Show an error message to the user
          },
        }
      );
    }
  };

  if (!sessionId || !userId) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory')} />
      </div>
    );
  }

  if (isLoading || completeSessionMutation.isLoading) { // Add mutation loading state
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

  const sessionAnswers = answers || [];

  const resultBodyTemplate = (rowData: AnswerHistory) => {
    if (rowData.isCorrect === undefined || rowData.isCorrect === null) { // Changed from 'correct' to 'isCorrect'
      return null; // Or a specific indicator for unanswered
    }
    return (
      <Tag
        value={rowData.isCorrect ? t('common.correct') : t('common.incorrect')}
        severity={rowData.isCorrect ? 'success' : 'danger'}
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
          value={sessionAnswers}
          sortMode="single"
          emptyMessage={t('sessionHistory.noAnswersFound')}
        >
          <Column field="questionText" header={t('sessionHistory.question')} sortable />
          <Column field="selectedAnswerIast" header={t('sessionHistory.yourAnswer')} body={selectedAnswerBodyTemplate} sortable />
          <Column field="correctOptionIast" header={t('sessionHistory.correctAnswer')} body={correctOptionBodyTemplate} sortable />
          <Column field="isCorrect" header={t('sessionHistory.result')} body={resultBodyTemplate} sortable /> {/* Changed field to 'isCorrect' */}
          <Column field="responseTimeMs" header={t('sessionHistory.responseTime')} sortable />
          <Column field="answeredAt" header={t('sessionHistory.answeredAt')} body={answeredAtBodyTemplate} sortable />
          <Column field="explanation" header={t('sessionHistory.explanation')} body={explanationBodyTemplate} sortable />
        </DataTable>

        {completeSessionMutation.isError && ( // Display error message if mutation fails
          <Message severity="error" text={t('quiz.completeSessionError', { message: completeSessionMutation.error?.message })} className="mt-3" />
        )}

        {quizType && ( // Only show button if quizType is available
          <div className="flex justify-content-end mt-4">
            <Button
              label={t('quiz.completeQuiz')}
              icon="pi pi-check"
              onClick={handleCompleteQuiz}
              disabled={completeSessionMutation.isLoading}
            />
          </div>
        )}
      </Card>
    </div>
  );
};

export default SessionHistoryPage;