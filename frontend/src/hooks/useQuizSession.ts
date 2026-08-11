import { useEffect } from 'react';
import { useNavigate, useLocation, useSearchParams } from 'react-router-dom';
import {
  useComposeQuizSession,
  useQuizBySlug,
  useResumeQuizSession,
  useCompleteQuizSession,
} from './useQuiz';
import { useQuizSessionState } from './useQuizSessionState';
import { useSubmitAnswerHandler } from './useQuizSessionSubmit';
import type { StartOrResumeResponse, ComposeQuizResponse, SessionQuestion } from '../types/quiz';

export function useQuizSession(
  slug: string | undefined,
  sessionIdFromParams: string | undefined,
) {
  const navigate = useNavigate();
  const location = useLocation();
  const [searchParams] = useSearchParams();
  const progressTagSetId = searchParams.get('progressTagSetId');

  const state = useQuizSessionState();

  const composeSession = useComposeQuizSession();
  const resumeSession = useResumeQuizSession();
  const completeSession = useCompleteQuizSession();

  const shouldFetch = !state.quizSummaryData && !location.state?.sessionData && !!slug;
  const {
    data: fetchedQuizSummary,
    isLoading: isSummaryLoading,
    isError: isSummaryError,
    error: summaryError,
  } = useQuizBySlug(shouldFetch ? slug || '' : '');

  const { handleSubmitAnswer, handleNextQuestion, submitAnswerMutation } =
    useSubmitAnswerHandler(state);

  useEffect(() => {
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

    const { id: quizId, slug: quizSlug } = fetchedQuizSummary;

    const apply = (data: { sessionId: string; questions: SessionQuestion[]; currentQuestionIndex?: number }) => {
      state.setSessionId(data.sessionId);
      state.setQuestions(data.questions);
      state.setCurrentQuestionIndex(data.currentQuestionIndex ?? 0);
      state.setStartTime(Date.now());
    };

    const applyAndNav = (data: StartOrResumeResponse) => {
      state.setQuizSummaryData(data);
      apply(data);
      navigate(`/quiz/grammar/${quizSlug}/${data.sessionId}`, { replace: true });
    };

    if (sessionIdFromParams) {
      resumeSession.mutate(
        { sessionId: sessionIdFromParams },
        { onSuccess: apply, onError: (e) => console.error('resume failed:', e) },
      );
      return;
    }

    composeSession.mutate(
      {
        topicCode: quizSlug || slug || '',
        count: fetchedQuizSummary.totalQuestions && fetchedQuizSummary.totalQuestions > 0
          ? fetchedQuizSummary.totalQuestions
          : 10,
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

  /* ---- completion effect ---- */
  useEffect(() => {
    const allDone =
      state.questions.length > 0 &&
      state.currentQuestionIndex >= state.questions.length;

    if (allDone && state.sessionId && !state.sessionCompletionAttempted) {
      state.setSessionCompletionAttempted(true);
      completeSession.mutate(
        { sessionId: state.sessionId },
        {
          onSuccess: () =>
            navigate(`/quiz-sessions/${state.sessionId}/history`),
          onError: () =>
            navigate(`/quiz-sessions/${state.sessionId}/history`),
        },
      );
    }
  }, [
    state.currentQuestionIndex,
    state.questions.length,
    state.sessionId,
    state.sessionCompletionAttempted,
    completeSession,
    navigate,
  ]);

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