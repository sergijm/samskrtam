import React from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag';
import { Button } from 'primereact/button';
import { useAuthStore } from '../store/authStore';
import { useSessionAnswerHistory, useQuizSessionSummary } from '../hooks/useUserQuizSessions';
import { useCompleteQuizSession } from '../hooks/useQuiz';
import { AnswerHistory, SessionStatus } from '../types/quiz';

const SessionHistoryPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { sessionId } = useParams<{ sessionId: string }>();
  const location = useLocation();
  const { user } = useAuthStore();
  const userId = user?.id;

  const { data: answers, isLoading: isAnswersLoading, isError: isAnswersError, error: answersError } = useSessionAnswerHistory(
    sessionId || '',
    userId || ''
  );

  const { data: sessionSummary, isLoading: isSummaryLoading, isError: isSummaryError, error: summaryError } = useQuizSessionSummary(
    sessionId || '',
    userId || ''
  );

  const completeSessionMutation = useCompleteQuizSession();

  const handleCompleteQuiz = () => {
    if (sessionId && sessionSummary?.quizType) {
      completeSessionMutation.mutate(
        { sessionId, quizType: sessionSummary.quizType },
        {
          onSuccess: () => {
            navigate('/dashboard', { replace: true });
          },
          onError: (err) => {
            console.error('Failed to complete quiz session:', err);
          },
        }
      );
    }
  };

  const handleResumeQuiz = () => {
    if (sessionSummary) {
      const { quizType, slug, sessionId } = sessionSummary;
      navigate(`/quiz/${quizType.toLowerCase()}/${slug}/${sessionId}`);
    }
  };

  if (!sessionId || !userId) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory')} />
      </div>
    );
  }

  if (isAnswersLoading || isSummaryLoading || completeSessionMutation.isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isAnswersError || isSummaryError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory', { message: answersError?.message || summaryError?.message })} />
      </div>
    );
  }

  const sessionAnswers = answers || [];
  const isSessionCompleted = sessionSummary?.status === SessionStatus.COMPLETED;

  const resultBodyTemplate = (rowData: AnswerHistory) => {
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

  const answeredAtBodyTemplate = (rowData: AnswerHistory) => {
    return rowData.answeredAt ? new Date(rowData.answeredAt).toLocaleString() : null;
  };

  const explanationBodyTemplate = (rowData: AnswerHistory) => {
    return i18n.language === 'ru' ? rowData.explanationRu : rowData.explanationEn || t('quiz.noExplanation');
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
          <Column field="isCorrect" header={t('sessionHistory.result')} body={resultBodyTemplate} sortable />
          <Column field="responseTimeMs" header={t('sessionHistory.responseTime')} sortable />
          <Column field="answeredAt" header={t('sessionHistory.answeredAt')} body={answeredAtBodyTemplate} sortable />
          <Column field="explanation" header={t('sessionHistory.explanation')} body={explanationBodyTemplate} sortable />
        </DataTable>

        {completeSessionMutation.isError && (
          <Message severity="error" text={t('quiz.completeSessionError', { message: completeSessionMutation.error?.message })} className="mt-3" />
        )}

        {!isSessionCompleted && (
          <div className="flex justify-content-end mt-4 gap-2">
            <Button
              label={t('common.continue')}
              icon="pi pi-play"
              className="p-button-success"
              onClick={handleResumeQuiz}
            />
            <Button
              label={t('quiz.completeQuiz')}
              icon="pi pi-check"
              onClick={handleCompleteQuiz}
              loading={completeSessionMutation.isLoading}
              className="p-button-warning"
            />
          </div>
        )}
      </Card>
    </div>
  );
};

export default SessionHistoryPage;
