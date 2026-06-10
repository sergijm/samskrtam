import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { Card } from 'primereact/card';

import { useStartQuizSession, useSubmitQuizAnswer, useQuizBySlug, useResumeQuizSession, useCompleteQuizSession } from '../hooks/useQuiz';
import { AnswerRequest, SessionQuestion, QuizType, StartOrResumeResponse } from '../types/quiz';
import { useLocaleStore } from '../store/localeStore';

const QuizPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { slug, sessionId: sessionIdFromParams } = useParams<{ slug?: string; sessionId?: string }>();

  const [sessionId, setSessionId] = useState<string | null>(sessionIdFromParams || null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [questions, setQuestions] = useState<SessionQuestion[]>([]);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ isCorrect: boolean; correctOptionId: string; explanation: string } | null>(null);
  const [startTime, setStartTime] = useState<number>(Date.now());
  const [hasAttemptedSessionLoad, setHasAttemptedSessionLoad] = useState(false);
  const [sessionCompletionAttempted, setSessionCompletionAttempted] = useState(false);

  // State to hold quiz summary data, either from state or fetched
  const [quizSummaryData, setQuizSummaryData] = useState<StartOrResumeResponse | null>(null);

  // Conditionally fetch quiz summary if not available from state
  const shouldFetchQuizSummary = !quizSummaryData && !location.state?.sessionData && slug;
  const { data: fetchedQuizSummary, isLoading: isQuizSummaryLoading, isError: isQuizSummaryError, error: quizSummaryError } = useQuizBySlug(shouldFetchQuizSummary ? slug || '' : '');

  const startSessionMutation = useStartQuizSession();
  const resumeSessionMutation = useResumeQuizSession();
  const submitAnswerMutation = useSubmitQuizAnswer();
  const completeSessionMutation = useCompleteQuizSession();

  const currentQuestion = questions[currentQuestionIndex];
  const isLastQuestion = questions.length > 0 && currentQuestionIndex === questions.length - 1;

  useEffect(() => {

    if (location.state?.sessionData) {
      const sessionDataFromState = location.state.sessionData as StartOrResumeResponse;
      setQuizSummaryData(sessionDataFromState); // Set quiz summary from state
      setSessionId(sessionDataFromState.sessionId);
      setQuestions(sessionDataFromState.questions);
      setCurrentQuestionIndex(sessionDataFromState.currentQuestionIndex);
      setStartTime(Date.now());
      setHasAttemptedSessionLoad(true);
      navigate(location.pathname, { replace: true, state: {} }); // Clear state

    } else if (fetchedQuizSummary && !hasAttemptedSessionLoad) {
      setHasAttemptedSessionLoad(true);
      setQuizSummaryData(fetchedQuizSummary); // Set quiz summary from fetch

      if (sessionIdFromParams) {
        resumeSessionMutation.mutate(
          { sessionId: sessionIdFromParams, quizType: fetchedQuizSummary.quizType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setCurrentQuestionIndex(data.currentQuestionIndex);
              setStartTime(Date.now());
              setQuizSummaryData(data); // Update with full data from resume
            },
            onError: (err) => {
              console.error('Failed to resume quiz session:', err);
            },
          }
        );
      } else {
        startSessionMutation.mutate(
          { quizIdentifier: fetchedQuizSummary.id, quizType: fetchedQuizSummary.quizType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setStartTime(Date.now());
              setQuizSummaryData(data); // Update with full data from start
              navigate(`/quiz/${fetchedQuizSummary.quizType.toLowerCase()}/${fetchedQuizSummary.slug}/${data.sessionId}`, { replace: true });
            },
            onError: (err) => {
              console.error('Failed to start quiz session:', err);
            },
          }
        );
      }
    }
  }, [location.state, fetchedQuizSummary, sessionIdFromParams, startSessionMutation, resumeSessionMutation, hasAttemptedSessionLoad, navigate, location.pathname]);

  useEffect(() => {
    const allQuestionsAnswered = questions.length > 0 && currentQuestionIndex >= questions.length;

    if (allQuestionsAnswered && sessionId && quizSummaryData?.quizType && !sessionCompletionAttempted) {
      setSessionCompletionAttempted(true);
      completeSessionMutation.mutate(
        { sessionId, quizType: quizSummaryData.quizType },
        {
          onSuccess: () => {
            navigate(`/quiz-sessions/${sessionId}/history`, { state: { quizType: quizSummaryData.quizType } });
          },
          onError: (err) => {
            console.error('Failed to complete quiz session:', err);
            navigate(`/quiz-sessions/${sessionId}/history`, { state: { quizType: quizSummaryData.quizType } });
          },
        }
      );
    }
  }, [currentQuestionIndex, questions.length, sessionId, quizSummaryData?.quizType, sessionCompletionAttempted, completeSessionMutation, navigate]);

  const handleSubmitAnswer = (optionIdToSubmit: string) => {
    if (!sessionId || !optionIdToSubmit || !currentQuestion || !quizSummaryData) {
      return;
    }

    setSelectedOptionId(optionIdToSubmit);

    const questionId = currentQuestion.id;
    const responseTimeMs = Date.now() - startTime;

    const selectedOption = currentQuestion.options.find(opt => opt.id === optionIdToSubmit);
    const selectedFormIast = selectedOption ? selectedOption.formIast : undefined;

    const answerRequest: AnswerRequest = {
      questionId: questionId,
      selectedOptionId: optionIdToSubmit,
      selectedFormIast: quizSummaryData.quizType !== QuizType.VOCABULARY ? selectedFormIast : undefined,
      responseTimeMs: responseTimeMs,
    };

    submitAnswerMutation.mutate(
      {
        sessionId: sessionId,
        quizIdentifier: quizSummaryData.quizId,
        quizType: quizSummaryData.quizType,
        answerRequest: answerRequest,
      },
      {
        onSuccess: (data) => {
          if (data.isCorrect) {
            handleNextQuestion();
          } else {
            const explanation = i18n.language === 'ru' ? data.explanationRu : data.explanationEn;
            setFeedback({
              isCorrect: data.isCorrect,
              correctOptionId: data.correctOptionId,
              explanation: explanation || t('quiz.noExplanation'),
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
  };

  const handleNextQuestion = () => {
    setFeedback(null);
    setSelectedOptionId(null);
    if (currentQuestionIndex < questions.length - 1) {
      setCurrentQuestionIndex(currentQuestionIndex + 1);
    } else {
      setCurrentQuestionIndex(questions.length);
      setSessionCompletionAttempted(false);
    }
  };

  if (isQuizSummaryLoading || startSessionMutation.isLoading || resumeSessionMutation.isLoading || completeSessionMutation.isLoading || !sessionId) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isQuizSummaryError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quiz.fetchError', { message: quizSummaryError?.message })} />
      </div>
    );
  }

  if (startSessionMutation.isError || resumeSessionMutation.isError || completeSessionMutation.isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quiz.startError', { message: startSessionMutation.error?.message || resumeSessionMutation.error?.message || completeSessionMutation.error?.message })} />
      </div>
    );
  }

  if (questions.length === 0 && hasAttemptedSessionLoad && !isQuizSummaryLoading && !startSessionMutation.isLoading && !resumeSessionMutation.isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="info" text={t('quiz.noQuestions')} />
      </div>
    );
  }

  if (questions.length === 0 && !hasAttemptedSessionLoad) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (!currentQuestion && questions.length > 0) {
    return null;
  }

  const progress = Math.round(((currentQuestionIndex + (feedback ? 1 : 0)) / questions.length) * 100);

  // Determine localized title and description from quizSummaryData
  const localizedQuizTitle = i18n.language === 'ru' ? quizSummaryData?.quizTitleRu : quizSummaryData?.quizTitleEn;
  const localizedQuizDescription = i18n.language === 'ru' ? quizSummaryData?.quizDescriptionRu : quizSummaryData?.quizDescriptionEn;

  // Function to parse the question text (no longer needed if data is structured)
  // const parseQuestionText = (text: string) => {
  //   const stemMatch = text.match(/Stem:\s*([^,]+),/);
  //   const caseMatch = text.match(/Case:\s*([^,]+),/);
  //   const numberMatch = text.match(/Number:\s*([^,]+)/);

  //   const stem = stemMatch ? stemMatch[1].trim() : '';
  //   const caseType = caseMatch ? caseMatch[1].trim() : '';
  //   const numberType = numberMatch ? numberMatch[1].trim() : '';

  //   return { stem, caseType, numberType };
  // };

  // const { stem, caseType, numberType } = parseQuestionText(currentQuestion.text); // No longer needed

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <Card className="quiz-container" style={{ maxWidth: '800px', width: '100%' }}>
        {localizedQuizTitle && <h1 className="text-center mb-3">{localizedQuizTitle}</h1>}
        {localizedQuizDescription && <p className="text-center text-color-secondary mb-4">{localizedQuizDescription}</p>}
        <ProgressBar value={progress} className="mb-4" />
        <h2 className="text-center mb-4">{t('quiz.question', { current: currentQuestionIndex + 1, total: questions.length })}</h2>
        <div className="text-2xl font-bold text-center mb-5">
          {currentQuestion.stem && (
            <>
              <span style={{ color: 'var(--primary-color)', fontWeight: 'bold', fontSize: '2.5rem' }}>{currentQuestion.stem}</span>
              <br style={{ lineHeight: '1.5' }} /> {/* Increased line height */}
            </>
          )}
          {currentQuestion.caseType && currentQuestion.numberType && (
            <span style={{ fontSize: '1.5rem', fontStyle: 'italic' }}>
              {currentQuestion.caseType}, {currentQuestion.numberType}
            </span>
          )}
        </div>

        <div className="grid">
          {currentQuestion.options.map((option) => (
            <div key={option.id} className="col-12 md:col-6">
              <Button
                label={option.formIast}
                className={`w-full text-xl p-3 mb-3 ${selectedOptionId === option.id ? 'p-button-primary' : 'p-button-outlined'} ${
                  feedback && option.id === feedback.correctOptionId ? 'p-button-success' : ''
                } ${feedback && selectedOptionId === option.id && !feedback.isCorrect ? 'p-button-danger' : ''}`}
                onClick={() => handleSubmitAnswer(option.id)}
                disabled={!!feedback || submitAnswerMutation.isLoading}
              />
            </div>
          ))}
        </div>

        {feedback && (
          <div className="feedback-section mt-5 p-3 border-round-md" style={{ backgroundColor: feedback.isCorrect ? '#e6ffe6' : '#ffe6e6' }}>
            <h3 className="text-xl font-bold mb-2" style={{ color: feedback.isCorrect ? '#28a745' : '#dc3545' }}>
              {feedback.isCorrect ? t('quiz.correct') : t('quiz.incorrect')}
            </h3>
            <p className="text-lg">{feedback.explanation}</p>
            <Button
              label={isLastQuestion ? t('quiz.completeQuiz') : t('quiz.next')}
              icon="pi pi-arrow-right"
              iconPos="right"
              className="mt-3 w-full"
              onClick={handleNextQuestion}
            />
          </div>
        )}
      </Card>
    </div>
  );
};

export default QuizPage;
