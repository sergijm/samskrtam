import { useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  useComposeQuizSession,
  useQuizBySlug,
  useResumeQuizSession,
  useCompleteQuizSession,
} from './useQuiz';
import { useQuizSessionState } from './useQuizSessionState';
import { useSubmitAnswerHandler } from './useQuizSessionSubmit';
import type { StartOrResumeResponse, ComposeQuizResponse, SessionQuestion } from '../types/quiz';

/**
 * Оркестратор квиз-сессии:
 *  - стейт        → useQuizSessionState
 *  - инициализация → useEffect (location.state / resume / compose-default)
 *  - завершение    → useEffect (completeSession)
 *  - отправка      → useSubmitAnswerHandler
 *  - агрегация loading/error
 */
export function useQuizSession(
  slug: string | undefined,
  sessionIdFromParams: string | undefined,
) {
  const navigate = useNavigate();
  const location = useLocation();

  /* ---- state ---- */
  const state = useQuizSessionState();

  /* ---- progress-tag slice from URL (?progressTagSetId=...) ---- */
  const progressTagSetId =
    new URLSearchParams(location.search).get('progressTagSetId') || undefined;

  /* ---- mutations ---- */
  const composeSession = useComposeQuizSession();
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
    // 1) navigation state (from compose holder pages)
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

    const apply = (data: { sessionId: string; questions: SessionQuestion[]; currentQuestionIndex?: number }) => {
      state.setSessionId(data.sessionId);
      state.setQuestions(data.questions);
      state.setCurrentQuestionIndex(data.currentQuestionIndex ?? 0);
      state.setStartTime(Date.now());
    };

    const applyAndNav = (data: StartOrResumeResponse) => {
      state.setQuizSummaryData(data);
      apply(data);
      navigate(`/quiz/${lessonType.toLowerCase()}/${quizSlug}/${data.sessionId}`, { replace: true });
    };

    // 2) resume an existing session
    if (sessionIdFromParams) {
      resumeSession.mutate(
        { sessionId: sessionIdFromParams, lessonType },
        { onSuccess: apply, onError: (e) => console.error('resume failed:', e) },
      );
      return;
    }

    // 3) default: compose the lesson from its curriculum topic
    composeSession.mutate(
      {
        topicCode: quizSlug || slug || '',
        count: fetchedQuizSummary.totalQuestions && fetchedQuizSummary.totalQuestions > 0
          ? fetchedQuizSummary.totalQuestions
          : 10,
        progressTagSetId,
      },
      {
        onSuccess: (data: ComposeQuizResponse) => {
          const merged: StartOrResumeResponse = {
            sessionId: data.sessionId,
            quizId: fetchedQuizSummary.id,
            lessonType: fetchedQuizSummary.lessonType,
            totalQuestions: data.totalQuestions,
            answeredQuestions: data.answeredQuestions,
            score: data.score,
            questions: data.questions,
            currentQuestionIndex: data.currentQuestionIndex,
            currentQuestionNumber: data.currentQuestionNumber,
            quizTitleRu: fetchedQuizSummary.titleRu,
            quizTitleEn: fetchedQuizSummary.titleEn,
            quizDescriptionRu: fetchedQuizSummary.descriptionRu,
            quizDescriptionEn: fetchedQuizSummary.descriptionEn,
            slug: fetchedQuizSummary.slug,
          };
          applyAndNav(merged);
        },
        onError: (e) => console.error('compose start failed:', e),
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.state, fetchedQuizSummary, sessionIdFromParams, state.hasAttemptedSessionLoad]);

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
    composeSession.isPending ||
    resumeSession.isPending ||
    completeSession.isPending ||
    !state.sessionId;

  const isError =
    isSummaryError ||
    composeSession.isError ||
    resumeSession.isError ||
    completeSession.isError;

  const errorMessage =
    summaryError?.message ||
    composeSession.error?.message ||
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
