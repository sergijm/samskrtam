import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useMe } from './useUser';
import { useSessionAnswerHistory, useQuizSessionSummary } from './useUserQuizSessions';
import { useCompleteQuizSession, useRetakeQuizSession } from './useQuiz';
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

  const isLoading =
    isUserLoading ||
    isAnswersLoading ||
    isSummaryLoading ||
    completeSessionMutation.isPending ||
    retakeSessionMutation.isPending;

  const isError = isAnswersError || isSummaryError;
  const errorMessage = answersError?.message || summaryError?.message;

  const isSessionCompleted = sessionSummary?.status === 'COMPLETED';

  const handleResume = () => {
    if (!sessionSummary) return;
    const { slug, sessionId: sid } = sessionSummary;
    navigate(`/quiz/grammar/${slug}/${sid}`);
  };

  const handleRetake = () => {
    if (!sessionId) return;
    retakeSessionMutation.mutate(
      { sessionId },
      {
        onSuccess: (data) =>
          navigate(`/quiz/grammar/${data.slug ?? ''}/${data.sessionId}`, {
            state: { sessionData: data },
          }),
        onError: (err) => console.error('Failed to retake quiz:', err),
      }
    );
  };

  const handleStartNew = () => {
    if (!sessionSummary) return;
    navigate(`/quiz/grammar/${sessionSummary.slug}`);
  };

  const handleComplete = () => {
    if (!sessionId) return;
    completeSessionMutation.mutate(
      { sessionId },
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
    completeError: completeSessionMutation.error?.message,
  };
}