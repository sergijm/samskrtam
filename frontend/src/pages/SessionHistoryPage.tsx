import React from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';

import { useSessionHistory } from '../hooks/useSessionHistory';
import SessionHistoryTable from '../components/quiz/SessionHistoryTable';
import SessionHistoryActions from '../components/quiz/SessionHistoryActions';
const SessionHistoryPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { sessionId } = useParams<{ sessionId: string }>();
  const location = useLocation();

  const {
    answers,
    isLoading,
    isError,
    errorMessage,
    isSessionCompleted,
    handleResume,
    handleRetake,
    handleStartNew,
    retakeLoading,
    startNewLoading,
    completeError,
  } = useSessionHistory(sessionId || '', null);
  if (!sessionId) {
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
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory', { message: errorMessage })} />
      </div>
    );
  }

  return (
    <div className="max-w-60rem mx-auto">
      <Card title={t('sessionHistory.title', { sessionId })} className="mb-4">
        <SessionHistoryTable answers={answers || []} />
        <SessionHistoryActions
          isCompleted={isSessionCompleted}
          onResume={handleResume}
          onRetake={handleRetake}
          onStartNew={handleStartNew}
          retakeLoading={retakeLoading}
          startNewLoading={startNewLoading}
          completeError={completeError}
        />
      </Card>
    </div>
  );
};

export default SessionHistoryPage;

