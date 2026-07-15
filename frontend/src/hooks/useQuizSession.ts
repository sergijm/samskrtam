import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  useStartQuizSession,
  useQuizBySlug,
  useResumeQuizSession,
  useCompleteQuizSession,
  useStartOrResumeQuizSessionWithFilters,
  useStartOrResumeWithStatusFilter,
} from './useQuiz';
import { useQuizSessionState } from './useQuizSessionState';
import { useSubmitAnswerHandler } from './useQuizSessionSubmit';
import type { FilterParams } from '../api/quizApi';
import type { StartOrResumeResponse } from '../types/quiz';

/**
 * Оркестратор квиз-сессии:
 *  - стейт        → useQuizSessionState
 *  - инициализация → useEffect (5 веток: location.state / resume / statusFilter / filters / default)
 *  - завершение    → useEffect (completeSession)
 *  - отправка      → useSubmitAnswerHandler
 *  - агрегация loading/error
 */
export function useQuizSession(
  slug: string | undefined,
  sessionIdFromParams: string | undefined,
  filterParams?: FilterParams,
  statusFilter?: string,
) {
  const navigate = useNavigate();
  const location = useLocation();

  /* ---- state ---- */
  const state = useQuizSessionState();

  /* ---- mutations ---- */
  const startSession = useStartQuizSession();
  const startFilteredSession = useStartOrResumeQuizSessionWithFilters();
  const startStatusFilterSession = useStartOrResumeWithStatusFilter();
  const resumeSession = useResumeQuizSession();
  const completeSession = useCompleteQuizSession();

  /* ---- quiz summary ---- */
  const shouldFetch = !state.quizSummaryData && !location.state?.sessionData && !!slug;
  const {
    data: fetchedQuizSummary,
    isLoading: isSummaryLoading,
    isError: isSummaryError,
    error: summaryError,
  } = useQuizBySlug(shouldFetch ? slug || '' : '');

  /* ---- submit handlers ---- */
  const { handleSubmitAnswer, handleNextQuestion, submitAnswerMutation } =
    useSubmitAnswerHandler(state);

  /* ═══════ init effect ═══════ */
  useEffect(() => {
    // 1) navigation state
    if (location.state?.sessionData) {
      const sd = location.state.sessionData as StartOrResumeResponse;
      state.setQuizSummaryData(sd);
      state.setSessionId(sd.sessionId);
      state.setQuestions(sd.questions);
      state.setCurrentQuestionIndex(sd.currentQuestionIndex);
      state.setStartTime(Date.now());
      state.setHasAttemptedSessionLoad(true);
      navigate(location.pathname, { replace: true, state: {} });
      return;
    }

    if (!fetchedQuizSummary || state.hasAttemptedSessionLoad) return;

    state.setHasAttemptedSessionLoad(true);
    state.setQuizSummaryData(fetchedQuizSummary as unknown as StartOrResumeResponse);

    const { lessonType, id: quizId, slug: quizSlug } = fetchedQuizSummary;

    const apply = (data: StartOrResumeResponse) => {
      state.setSessionId(data.sessionId);
      state.setQuestions(data.questions);
      state.setCurrentQuestionIndex(data.currentQuestionIndex ?? 0);
      state.setStartTime(Date.now());
      state.setQuizSummaryData(data);
    };

    const applyAndNav = (data: StartOrResumeResponse) => {
      apply(data);
      navigate(`/quiz/${lessonType.toLowerCase()}/${quizSlug}/${data.sessionId}`, { replace: true });
    };

    // 2) resume
    if (sessionIdFromParams) {
      resumeSession.mutate(
        { sessionId: sessionIdFromParams, lessonType },
        { onSuccess: apply, onError: (e) => console.error('resume failed:', e) },
      );
      return;
    }

    // 3) status filter (NEW/LEARNING/REVIEW)
    if (statusFilter) {
      startStatusFilterSession.mutate(
        { quizId, lessonType, statusFilter },
        { onSuccess: applyAndNav, onError: (e) => console.error('status-filter start failed:', e) },
      );
      return;
    }

    // 4) declension filters
    if (filterParams?.filterScope) {
      startFilteredSession.mutate(
        { quizId, lessonType, filters: filterParams },
        { onSuccess: applyAndNav, onError: (e) => console.error('filtered start failed:', e) },
      );
      return;
    }

    // 5) default
    startSession.mutate(
      { quizIdentifier: quizId, lessonType },
      { onSuccess: applyAndNav, onError: (e) => console.error('start failed:', e) },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.state, fetchedQuizSummary, sessionIdFromParams, statusFilter, filterParams, state.hasAttemptedSessionLoad]);

  /* ═══════ completion effect ═══════ */
  useEffect(() => {
    const allDone =
      state.questions.length > 0 &&
      state.currentQuestionIndex >= state.questions.length;

    if (allDone && state.sessionId && state.quizSummaryData?.lessonType && !state.sessionCompletionAttempted) {
      state.setSessionCompletionAttempted(true);
      completeSession.mutate(
        { sessionId: state.sessionId, lessonType: state.quizSummaryData.lessonType },
        {
          onSuccess: () =>
            navigate(`/quiz-sessions/${state.sessionId}/history`, {
              state: { lessonType: state.quizSummaryData?.lessonType },
            }),
          onError: () =>
            navigate(`/quiz-sessions/${state.sessionId}/history`, {
              state: { lessonType: state.quizSummaryData?.lessonType },
            }),
        },
      );
    }
  }, [
    state.currentQuestionIndex,
    state.questions.length,
    state.sessionId,
    state.quizSummaryData?.lessonType,
    state.sessionCompletionAttempted,
    completeSession,
    navigate,
  ]);

  /* ---- aggregates ---- */
  const isLoading =
    isSummaryLoading ||
    startSession.isPending ||
    startFilteredSession.isPending ||
    startStatusFilterSession.isPending ||
    resumeSession.isPending ||
    completeSession.isPending ||
    !state.sessionId;

  const isError =
    isSummaryError ||
    startSession.isError ||
    startFilteredSession.isError ||
    startStatusFilterSession.isError ||
    resumeSession.isError ||
    completeSession.isError;

  const errorMessage =
    summaryError?.message ||
    startSession.error?.message ||
    startFilteredSession.error?.message ||
    startStatusFilterSession.error?.message ||
    resumeSession.error?.message ||
    completeSession.error?.message;

  return {
    sessionId: state.sessionId,
    currentQuestionIndex: state.currentQuestionIndex,
    questions: state.questions,
    currentQuestion: state.currentQuestion,
    feedback: state.feedback,
    isLastQuestion: state.isLastQuestion,
    quizSummaryData: state.quizSummaryData,
    selectedOptionId: state.selectedOptionId,
    isSubmittingAnswer: submitAnswerMutation.isPending,
    isLoading,
    isError,
    errorMessage,
    handleSubmitAnswer,
    handleNextQuestion,
    hasAttemptedSessionLoad: state.hasAttemptedSessionLoad,
  };
}
