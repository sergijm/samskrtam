import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMe } from './useUser';
import { useSessionAnswerHistory, useQuizSessionSummary } from './useUserQuizSessions';
import { useCompleteQuizSession, useRetakeQuizSession, useStartNewQuizSession } from './useQuiz';
import { useQueryClient } from '@tanstack/react-query';

export function useSessionHistory() {
  const { sessionId } = useParams<{ sessionId: string }>();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const queryClient = useQueryClient();

  const { data: user, isLoading: isUserLoading } = useMe();
  const userId = user?.id;

  const {
    data: answers,
    isLoading: isAnswersLoading,
    isError: isAnswersError,
    error: answersError,
  } = useSessionAnswerHistory(sessionId || '', userId || '');

  const {
    data: sessionSummary,
    isLoading: isSummaryLoading,
    isError: isSummaryError,
    error: summaryError,
  } = useQuizSessionSummary(sessionId || '', userId || '');

  const completeSessionMutation = useCompleteQuizSession();
  const retakeSessionMutation = useRetakeQuizSession();
  const startNewQuizSessionMutation = useStartNewQuizSession();

  const isLoading =
    isUserLoading ||
    isAnswersLoading ||
    isSummaryLoading ||
    completeSessionMutation.isPending ||
    retakeSessionMutation.isPending ||
    startNewQuizSessionMutation.isPending;

  const isError = isAnswersError || isSummaryError;
  const errorMessage = answersError?.message || summaryError?.message;

  const isSessionCompleted = sessionSummary?.status === 'COMPLETED';

  const handleResume = () => {
    if (!sessionSummary) return;
    const { lessonType, slug, sessionId: sid } = sessionSummary;
    navigate(`/quiz/${lessonType.toLowerCase()}/${slug}/${sid}`);
  };

  const handleRetake = () => {
    if (!sessionId || !sessionSummary) return;
    retakeSessionMutation.mutate(
      { sessionId, lessonType: sessionSummary.lessonType, slug: sessionSummary.slug },
      {
        onSuccess: (data) =>
          navigate(`/quiz/${data.lessonType.toLowerCase()}/${data.slug}/${data.sessionId}`, {
            state: { sessionData: data },
          }),
        onError: (err) => console.error('Failed to retake quiz:', err),
      }
    );
  };

  const handleStartNew = () => {
    if (!sessionId || !sessionSummary) return;
    startNewQuizSessionMutation.mutate(
      { sessionId, lessonType: sessionSummary.lessonType, slug: sessionSummary.slug },
      {
        onSuccess: (data) =>
          navigate(`/quiz/${data.lessonType.toLowerCase()}/${data.slug}/${data.sessionId}`, {
            state: { sessionData: data },
          }),
        onError: (err) => console.error('Failed to start new quiz:', err),
      }
    );
  };

  const handleComplete = () => {
    if (!sessionId || !sessionSummary?.lessonType) return;
    completeSessionMutation.mutate(
      { sessionId, lessonType: sessionSummary.lessonType },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: ['quizSessionSummary', sessionId] });
          navigate('/dashboard', { replace: true });
        },
        onError: (err) => console.error('Failed to complete quiz:', err),
      }
    );
  };

  return {
    sessionId,
    answers: answers || [],
    sessionSummary,
    isLoading,
    isError,
    errorMessage,
    isSessionCompleted,
    handleResume,
    handleRetake,
    handleStartNew,
    handleComplete,
    retakeLoading: retakeSessionMutation.isPending,
    startNewLoading: startNewQuizSessionMutation.isPending,
    completeError: completeSessionMutation.error?.message,
  };
}