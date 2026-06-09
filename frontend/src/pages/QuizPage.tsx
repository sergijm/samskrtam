import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { Card } from 'primereact/card';
import { useStartQuizSession, useSubmitQuizAnswer, useQuizBySlug, useResumeQuizSession, useCompleteQuizSession } from '../hooks/useQuiz'; // Import useCompleteQuizSession
import { AnswerRequest, SessionQuestion, QuizType } from '../types/quiz';
import { useLocaleStore } from '../store/localeStore'; // Import useLocaleStore

const QuizPage = () => {
  const { t, i18n } = useTranslation(); // Get i18n instance for current language
  const navigate = useNavigate();
  const { slug, sessionId: sessionIdFromParams } = useParams<{ slug?: string; sessionId?: string }>();

  const [sessionId, setSessionId] = useState<string | null>(sessionIdFromParams || null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [questions, setQuestions] = useState<SessionQuestion[]>([]);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ isCorrect: boolean; correctOptionId: string; explanation: string } | null>(null);
  const [startTime, setStartTime] = useState<number>(Date.now());
  const [hasAttemptedSessionLoad, setHasAttemptedSessionLoad] = useState(false);
  const [sessionCompletionAttempted, setSessionCompletionAttempted] = useState(false); // New state to prevent multiple calls

  const { data: quizSummary, isLoading: isQuizSummaryLoading, isError: isQuizSummaryError, error: quizSummaryError } = useQuizBySlug(slug || '');

  const startSessionMutation = useStartQuizSession();
  const resumeSessionMutation = useResumeQuizSession();
  const submitAnswerMutation = useSubmitQuizAnswer();
  const completeSessionMutation = useCompleteQuizSession(); // Initialize useCompleteQuizSession

  // Determine current question early
  const currentQuestion = questions[currentQuestionIndex];
  // Determine if it's the last question
  const isLastQuestion = questions.length > 0 && currentQuestionIndex === questions.length - 1;


  useEffect(() => {
    if (quizSummary && !hasAttemptedSessionLoad) {
      setHasAttemptedSessionLoad(true);

      if (sessionIdFromParams) {
        resumeSessionMutation.mutate(
          { sessionId: sessionIdFromParams, quizType: quizSummary.quizType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setCurrentQuestionIndex(data.currentQuestionIndex);
              setStartTime(Date.now());
            },
            onError: (err) => {
              console.error('Failed to resume quiz session:', err);
            },
          }
        );
      } else {
        startSessionMutation.mutate(
          { quizIdentifier: quizSummary.id, quizType: quizSummary.quizType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setStartTime(Date.now());
              // !!! ДОБАВЛЕНО: Обновляем URL с новым sessionId !!!
              navigate(`/quiz/${quizSummary.quizType.toLowerCase()}/${quizSummary.slug}/${data.sessionId}`, { replace: true });
            },
            onError: (err) => {
              console.error('Failed to start quiz session:', err);
            },
          }
        );
      }
    }
  }, [quizSummary, sessionIdFromParams, startSessionMutation, resumeSessionMutation, hasAttemptedSessionLoad, navigate]);

  // New useEffect to handle session completion when all questions are answered
  useEffect(() => {
    // Check if all questions have been answered
    const allQuestionsAnswered = questions.length > 0 && currentQuestionIndex >= questions.length;

    if (allQuestionsAnswered && sessionId && quizSummary?.quizType && !sessionCompletionAttempted) {
      setSessionCompletionAttempted(true); // Mark that we've attempted completion
      completeSessionMutation.mutate(
        { sessionId, quizType: quizSummary.quizType },
        {
          onSuccess: () => {
            navigate(`/quiz-sessions/${sessionId}/history`, { state: { quizType: quizSummary.quizType } });
          },
          onError: (err) => {
            console.error('Failed to complete quiz session:', err);
            // Even if completion fails, navigate to history to show answers
            navigate(`/quiz-sessions/${sessionId}/history`, { state: { quizType: quizSummary.quizType } });
          },
        }
      );
    }
  }, [currentQuestionIndex, questions.length, sessionId, quizSummary?.quizType, sessionCompletionAttempted, completeSessionMutation, navigate]);


  const handleSubmitAnswer = (optionIdToSubmit: string) => {
    if (!sessionId || !optionIdToSubmit || !currentQuestion || !quizSummary) { // Use currentQuestion directly
      return;
    }

    setSelectedOptionId(optionIdToSubmit); // For visual feedback

    const questionId = currentQuestion.id; // Use currentQuestion directly
    const responseTimeMs = Date.now() - startTime;

    const selectedOption = currentQuestion.options.find(opt => opt.id === optionIdToSubmit);
    const selectedFormIast = selectedOption ? selectedOption.formIast : undefined;

    const answerRequest: AnswerRequest = {
      questionId: questionId,
      selectedOptionId: optionIdToSubmit,
      selectedFormIast: quizSummary.quizType !== QuizType.VOCABULARY ? selectedFormIast : undefined, // Only send for non-vocabulary quizzes
      responseTimeMs: responseTimeMs,
    };

    submitAnswerMutation.mutate(
      {
        sessionId: sessionId,
        quizIdentifier: quizSummary.id,
        quizType: quizSummary.quizType,
        answerRequest: answerRequest,
      },
      {
        onSuccess: (data) => {
          if (data.isCorrect) { // If correct, immediately move to the next question
            handleNextQuestion();
          } else { // If incorrect, show feedback
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
          setSelectedOptionId(null); // Reset selection on error
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
      // Если это последний вопрос, мы должны увеличить currentQuestionIndex
      // так, чтобы условие useEffect (currentQuestionIndex >= questions.length) стало истинным.
      setCurrentQuestionIndex(questions.length); // Устанавливаем его в questions.length, чтобы запустить логику завершения
      setSessionCompletionAttempted(false); // Сбрасываем, чтобы позволить useEffect сработать
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

  // If there are no questions and session load was attempted, display a message
  if (questions.length === 0 && hasAttemptedSessionLoad && !isQuizSummaryLoading && !startSessionMutation.isLoading && !resumeSessionMutation.isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="info" text={t('quiz.noQuestions')} />
      </div>
    );
  }

  // If there are no questions and session load was NOT attempted, it means we are still loading or there's an initial state.
  // In this case, we should show the spinner or wait for the initial load to complete.
  if (questions.length === 0 && !hasAttemptedSessionLoad) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  // If currentQuestion is null but questions.length > 0, it means we've gone past the end of questions.
  // The useEffect for completion should handle this. Render nothing while it processes.
  if (!currentQuestion && questions.length > 0) {
    return null;
  }


  const progress = Math.round(((currentQuestionIndex + (feedback ? 1 : 0)) / questions.length) * 100);

  // Determine localized title and description
  const localizedQuizTitle = i18n.language === 'ru' ? quizSummary?.titleRu : quizSummary?.titleEn;
  const localizedQuizDescription = i18n.language === 'ru' ? quizSummary?.descriptionRu : quizSummary?.descriptionEn;

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <Card className="quiz-container" style={{ maxWidth: '800px', width: '100%' }}>
        {localizedQuizTitle && <h1 className="text-center mb-3">{localizedQuizTitle}</h1>}
        {localizedQuizDescription && <p className="text-center text-color-secondary mb-4">{localizedQuizDescription}</p>}
        <ProgressBar value={progress} className="mb-4" />
        <h2 className="text-center mb-4">{t('quiz.question', { current: currentQuestionIndex + 1, total: questions.length })}</h2>
        <div className="text-2xl font-bold text-center mb-5">{currentQuestion.text}</div>

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
              label={isLastQuestion ? t('quiz.completeQuiz') : t('quiz.next')} // Conditional label
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
