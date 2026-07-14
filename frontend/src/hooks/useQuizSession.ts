import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import {
  useStartQuizSession,
  useSubmitQuizAnswer,
  useQuizBySlug,
  useResumeQuizSession,
  useCompleteQuizSession,
    useStartOrResumeQuizSessionWithFilters,
  useStartOrResumeWithStatusFilter,
} from './useQuiz';
import type { FilterParams } from '../api/quizApi';
import { AnswerRequest, SessionQuestion, StartOrResumeResponse, LessonType } from '../types/quiz';

export function useQuizSession(slug: string | undefined, sessionIdFromParams: string | undefined, filterParams?: FilterParams, statusFilter?: string) {
  const navigate = useNavigate();
  const location = useLocation();
  const { i18n } = useTranslation();

  const [sessionId, setSessionId] = useState<string | null>(sessionIdFromParams || null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [questions, setQuestions] = useState<SessionQuestion[]>([]);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{
    isCorrect: boolean;
    correctOptionId: string;
    correctAnswerText: string;
    explanation: string;
  } | null>(null);
  const [startTime, setStartTime] = useState<number>(Date.now());
  const [hasAttemptedSessionLoad, setHasAttemptedSessionLoad] = useState(false);
  const [sessionCompletionAttempted, setSessionCompletionAttempted] = useState(false);
  const [quizSummaryData, setQuizSummaryData] = useState<StartOrResumeResponse | null>(null);

  const shouldFetchQuizSummary = !quizSummaryData && !location.state?.sessionData && !!slug;
  const { data: fetchedQuizSummary, isLoading: isQuizSummaryLoading, isError: isQuizSummaryError, error: quizSummaryError } = useQuizBySlug(
    shouldFetchQuizSummary ? slug || '' : ''
  );

    const startSessionMutation = useStartQuizSession();
  const startFilteredSessionMutation = useStartOrResumeQuizSessionWithFilters();
  const startStatusFilterSessionMutation = useStartOrResumeWithStatusFilter();
  const resumeSessionMutation = useResumeQuizSession();
  const submitAnswerMutation = useSubmitQuizAnswer();
  const completeSessionMutation = useCompleteQuizSession();

  const currentQuestion = questions[currentQuestionIndex];
  const isLastQuestion = questions.length > 0 && currentQuestionIndex === questions.length - 1;

  useEffect(() => {
    if (location.state?.sessionData) {
      const sessionDataFromState = location.state.sessionData as StartOrResumeResponse;
      setQuizSummaryData(sessionDataFromState);
      setSessionId(sessionDataFromState.sessionId);
      setQuestions(sessionDataFromState.questions);
      setCurrentQuestionIndex(sessionDataFromState.currentQuestionIndex);
      setStartTime(Date.now());
      setHasAttemptedSessionLoad(true);
      navigate(location.pathname, { replace: true, state: {} });
    } else if (fetchedQuizSummary && !hasAttemptedSessionLoad) {
      setHasAttemptedSessionLoad(true);
      setQuizSummaryData(fetchedQuizSummary);

      if (sessionIdFromParams) {
        resumeSessionMutation.mutate(
          { sessionId: sessionIdFromParams, lessonType: fetchedQuizSummary.lessonType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setCurrentQuestionIndex(data.currentQuestionIndex);
              setStartTime(Date.now());
              setQuizSummaryData(data);
            },
            onError: (err) => console.error('Failed to resume lesson session:', err),
          }
        );
      } else if (statusFilter) {
        // Сессия с фильтром по статусу (NEW/LEARNING/REVIEW)
        startStatusFilterSessionMutation.mutate(
          { quizId: fetchedQuizSummary.id, lessonType: fetchedQuizSummary.lessonType, statusFilter },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setCurrentQuestionIndex(data.currentQuestionIndex || 0);
              setStartTime(Date.now());
              setQuizSummaryData(data);
              navigate(`/quiz/${fetchedQuizSummary.lessonType.toLowerCase()}/${fetchedQuizSummary.slug}/${data.sessionId}`, { replace: true });
            },
            onError: (err) => console.error('Failed to start status-filtered session:', err),
          }
        );
      } else if (filterParams && filterParams.filterScope) {
        // Отфильтрованная сессия по падежу/роду/числу
        startFilteredSessionMutation.mutate(
          { quizId: fetchedQuizSummary.id, lessonType: fetchedQuizSummary.lessonType, filters: filterParams },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setCurrentQuestionIndex(data.currentQuestionIndex || 0);
              setStartTime(Date.now());
              setQuizSummaryData(data);
              // URL без фильтров, но с sessionId
              navigate(`/quiz/${fetchedQuizSummary.lessonType.toLowerCase()}/${fetchedQuizSummary.slug}/${data.sessionId}`, { replace: true });
            },
            onError: (err) => console.error('Failed to start filtered session:', err),
          }
        );
      } else {
        startSessionMutation.mutate(
          { quizIdentifier: fetchedQuizSummary.id, lessonType: fetchedQuizSummary.lessonType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setStartTime(Date.now());
              setQuizSummaryData(data);
              navigate(`/quiz/${fetchedQuizSummary.lessonType.toLowerCase()}/${fetchedQuizSummary.slug}/${data.sessionId}`, { replace: true });
            },
            onError: (err) => console.error('Failed to start lesson session:', err),
          }
        );
      }
    }
  }, [location.state, fetchedQuizSummary, sessionIdFromParams, statusFilter, startSessionMutation, startFilteredSessionMutation, startStatusFilterSessionMutation, resumeSessionMutation, hasAttemptedSessionLoad, navigate, location.pathname, filterParams]);

  useEffect(() => {
    const allQuestionsAnswered = questions.length > 0 && currentQuestionIndex >= questions.length;
    if (allQuestionsAnswered && sessionId && quizSummaryData?.lessonType && !sessionCompletionAttempted) {
      setSessionCompletionAttempted(true);
      completeSessionMutation.mutate(
        { sessionId, lessonType: quizSummaryData.lessonType },
        {
          onSuccess: () => navigate(`/quiz-sessions/${sessionId}/history`, { state: { lessonType: quizSummaryData.lessonType } }),
          onError: () => navigate(`/quiz-sessions/${sessionId}/history`, { state: { lessonType: quizSummaryData.lessonType } }),
        }
      );
    }
  }, [currentQuestionIndex, questions.length, sessionId, quizSummaryData?.lessonType, sessionCompletionAttempted, completeSessionMutation, navigate]);

  const handleSubmitAnswer = useCallback((optionIdToSubmit: string) => {
    if (!sessionId || !optionIdToSubmit || !currentQuestion || !quizSummaryData) return;

    setSelectedOptionId(optionIdToSubmit);
    const questionId = currentQuestion.id;
    const responseTimeMs = Date.now() - startTime;
    const selectedOption = currentQuestion.options.find((opt) => opt.id === optionIdToSubmit);
    const selectedFormIast = selectedOption ? selectedOption.formIast : undefined;

    const answerRequest: AnswerRequest = {
      questionId,
      selectedOptionId: optionIdToSubmit,
      selectedFormIast: quizSummaryData.lessonType !== LessonType.VOCABULARY ? selectedFormIast : undefined,
      responseTimeMs,
    };

    submitAnswerMutation.mutate(
      {
        sessionId,
        quizIdentifier: quizSummaryData.quizId,
        lessonType: quizSummaryData.lessonType,
        answerRequest,
      },
      {
        onSuccess: (data) => {
          if (data.isCorrect) {
            setFeedback(null);
            setSelectedOptionId(null);
            if (currentQuestionIndex < questions.length - 1) {
              setCurrentQuestionIndex((i) => i + 1);
            } else {
              setCurrentQuestionIndex(questions.length);
              setSessionCompletionAttempted(false);
            }
          } else {
            const explanation = i18n.language === 'ru' ? data.explanationRu : data.explanationEn;
            setFeedback({
              isCorrect: data.isCorrect,
              correctOptionId: data.correctOptionId,
              correctAnswerText: data.correctAnswerText,
              explanation: explanation || 'No explanation',
            });
            setStartTime(Date.now());
          }
        },
        onError: (err) => {
          console.error('Failed to submit answer:', err);
          setSelectedOptionId(null);
        },
      }
    );
  }, [sessionId, currentQuestion, quizSummaryData, startTime, submitAnswerMutation, i18n.language, currentQuestionIndex, questions.length]);

  const handleNextQuestion = useCallback(() => {
    setFeedback(null);
    setSelectedOptionId(null);
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex((i) => i + 1);
    } else {
      setCurrentQuestionIndex(questions.length);
      setSessionCompletionAttempted(false);
    }
  }, [currentQuestionIndex, questions.length]);

  return {
    sessionId,
    currentQuestionIndex,
    questions,
    currentQuestion,
    feedback,
    isLastQuestion,
    quizSummaryData,
    selectedOptionId,
    isSubmittingAnswer: submitAnswerMutation.isPending,
        isLoading:
      isQuizSummaryLoading ||
      startSessionMutation.isPending ||
      startFilteredSessionMutation.isPending ||
      startStatusFilterSessionMutation.isPending ||
      resumeSessionMutation.isPending ||
      completeSessionMutation.isPending ||
      !sessionId,
        isError:
      isQuizSummaryError ||
      startSessionMutation.isError ||
      startFilteredSessionMutation.isError ||
      startStatusFilterSessionMutation.isError ||
      resumeSessionMutation.isError ||
      completeSessionMutation.isError,
    errorMessage:
      quizSummaryError?.message ||
      startSessionMutation.error?.message ||
      startFilteredSessionMutation.error?.message ||
      resumeSessionMutation.error?.message ||
      completeSessionMutation.error?.message,
    handleSubmitAnswer,
    handleNextQuestion,
    hasAttemptedSessionLoad,
  };
}
