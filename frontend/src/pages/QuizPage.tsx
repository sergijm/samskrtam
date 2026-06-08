import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { Card } from 'primereact/card';
import { useStartQuizSession, useSubmitQuizAnswer, useQuizBySlug, useResumeQuizSession } from '../hooks/useQuiz'; // Import useResumeQuizSession
import { AnswerRequest, SessionQuestion, QuizType } from '../types/quiz'; // Import types from local types file

const QuizPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { slug, sessionId: sessionIdFromParams } = useParams<{ slug?: string; sessionId?: string }>(); // Get sessionId from params

  const [sessionId, setSessionId] = useState<string | null>(sessionIdFromParams || null); // Initialize with sessionId from params
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [questions, setQuestions] = useState<SessionQuestion[]>([]);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ isCorrect: boolean; correctOptionId: string; explanation: string } | null>(null);
  const [startTime, setStartTime] = useState<number>(Date.now());
  const [hasAttemptedSessionLoad, setHasAttemptedSessionLoad] = useState(false); // Renamed from hasAttemptedStart

  const { data: quizSummary, isLoading: isQuizSummaryLoading, isError: isQuizSummaryError, error: quizSummaryError } = useQuizBySlug(slug || '');

  const startSessionMutation = useStartQuizSession();
  const resumeSessionMutation = useResumeQuizSession(); // New mutation for resuming
  const submitAnswerMutation = useSubmitQuizAnswer();

  useEffect(() => {
    if (quizSummary && !hasAttemptedSessionLoad) {
      setHasAttemptedSessionLoad(true);

      if (sessionIdFromParams) {
        // Attempt to resume session
        resumeSessionMutation.mutate(
          { sessionId: sessionIdFromParams, quizType: quizSummary.quizType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setCurrentQuestionIndex(data.currentQuestionIndex); // Set current question index
              setStartTime(Date.now());
            },
            onError: (err) => {
              console.error('Failed to resume quiz session:', err);
              // Optionally, navigate to an error page or try to start a new session
              // For now, we'll just log the error.
            },
          }
        );
      } else {
        // Start a new session
        startSessionMutation.mutate(
          { quizIdentifier: quizSummary.id, quizType: quizSummary.quizType },
          {
            onSuccess: (data) => {
              setSessionId(data.sessionId);
              setQuestions(data.questions);
              setStartTime(Date.now());
            },
            onError: (err) => {
              console.error('Failed to start quiz session:', err);
            },
          }
        );
      }
    }
  }, [quizSummary, sessionIdFromParams, startSessionMutation, resumeSessionMutation, hasAttemptedSessionLoad]);

  const handleOptionSelect = (optionId: string) => {
    setSelectedOptionId(optionId);
  };

  const handleSubmitAnswer = () => {
    if (!sessionId || !selectedOptionId || !questions[currentQuestionIndex] || !quizSummary) {
      return;
    }

    const questionId = questions[currentQuestionIndex].id;
    const responseTimeMs = Date.now() - startTime;

    const answerRequest: AnswerRequest = {
      questionId: questionId,
      selectedOptionId: selectedOptionId,
      responseTimeMs: responseTimeMs,
    };

    submitAnswerMutation.mutate(
      {
        sessionId: sessionId,
        quizIdentifier: quizSummary.id, // Pass quizSummary.id as quizIdentifier
        quizType: quizSummary.quizType,
        answerRequest: answerRequest,
      },
      {
        onSuccess: (data) => {
          setFeedback({
            isCorrect: data.isCorrect,
            correctOptionId: data.correctOptionId,
            explanation: data.explanation,
          });
          setStartTime(Date.now());
        },
        onError: (err) => {
          console.error('Failed to submit answer:', err);
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
      navigate(`/quiz/result/${sessionId}`);
    }
  };

  if (isQuizSummaryLoading || startSessionMutation.isLoading || resumeSessionMutation.isLoading || !sessionId) {
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

  if (startSessionMutation.isError || resumeSessionMutation.isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quiz.startError', { message: startSessionMutation.error?.message || resumeSessionMutation.error?.message })} />
      </div>
    );
  }

  const currentQuestion = questions[currentQuestionIndex];
  if (!currentQuestion) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="info" text={t('quiz.noQuestions')} />
      </div>
    );
  }

  const progress = Math.round(((currentQuestionIndex + (feedback ? 1 : 0)) / questions.length) * 100);

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <Card className="quiz-container" style={{ maxWidth: '800px', width: '100%' }}>
        <ProgressBar value={progress} className="mb-4" />
        <h2 className="text-center mb-4">{t('quiz.question', { current: currentQuestionIndex + 1, total: questions.length })}</h2>
        <div className="text-2xl font-bold text-center mb-5">{currentQuestion.text}</div>

        <div className="grid">
          {currentQuestion.options.map((option) => (
            <div key={option.id} className="col-12 md:col-6">
              <Button
                label={option.formIast} // Use option.formIast instead of option.text
                className={`w-full text-xl p-3 mb-3 ${selectedOptionId === option.id ? 'p-button-primary' : 'p-button-outlined'} ${
                  feedback && option.id === feedback.correctOptionId ? 'p-button-success' : ''
                } ${feedback && selectedOptionId === option.id && !feedback.isCorrect ? 'p-button-danger' : ''}`}
                onClick={() => handleOptionSelect(option.id)}
                disabled={!!feedback}
              />
            </div>
          ))}
        </div>

        {feedback && (
          <div className="feedback-section mt-5 p-3 border-round-md" style={{ backgroundColor: feedback.isCorrect ? '#e6ffe6' : '#ffe6e6' }}>
            <h3 className="text-xl font-bold mb-2" style={{ color: feedback.isCorrect ? '#28a745' : '#dc3545' }}>
              {feedback.isCorrect ? t('quiz.correct') : t('quiz.incorrect')}
            </h3>
            <p className="text-lg">{feedback.explanation}</p> {/* Use single explanation field */}
            <Button label={t('quiz.next')} icon="pi pi-arrow-right" iconPos="right" className="mt-3 w-full" onClick={handleNextQuestion} />
          </div>
        )}

        {!feedback && (
          <Button
            label={t('quiz.submitAnswer')}
            className="mt-5 w-full p-button-lg"
            onClick={handleSubmitAnswer}
            disabled={!selectedOptionId || submitAnswerMutation.isLoading}
          />
        )}
      </Card>
    </div>
  );
};

export default QuizPage;
