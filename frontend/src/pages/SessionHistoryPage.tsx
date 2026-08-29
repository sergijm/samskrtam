import React from 'react';
import { useParams, useLocation, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';

import { useSessionHistory } from '../hooks/useSessionHistory';
import SessionHistoryTable from '../components/quiz/SessionHistoryTable';
import SessionHistoryActions from '../components/quiz/SessionHistoryActions';
import { isVocabularyQuiz } from '../types/quizEnums';

const SessionHistoryPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { sessionId } = useParams<{ sessionId: string }>();
  const stateSlug = (location.state as { slug?: string })?.slug;
  const stateLessonType = (location.state as { lessonType?: string })?.lessonType;

  const {
    answers,
    isLoading,
    isError,
    errorMessage,
    isSessionCompleted,
    sessionSummary,
    completeError,
  } = useSessionHistory();

  const handleBackToLesson = () => {
    const slug = sessionSummary?.slug || stateSlug;
    const lessonType = sessionSummary?.lessonType || stateLessonType;
    if (!slug) {
      navigate('/grammar');
      return;
    }
    const prefix = isVocabularyQuiz(lessonType) ? '/lessons/vocabulary/' : '/lessons/grammar/';
    navigate(`${prefix}${slug}`);
  };

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
          onBackToLesson={handleBackToLesson}
          completeError={completeError}
        />
      </Card>
    </div>
  );
};

export default SessionHistoryPage;