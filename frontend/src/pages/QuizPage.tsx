import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { Card } from 'primereact/card';
import { useStartQuizSession, useSubmitQuizAnswer, useQuizBySlug } from '../hooks/useQuiz'; // Import useQuizBySlug
import { QuestionDto } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/QuestionDto';
import { AnswerRequest } from '../../shared/quiz-dtos/src/main/java/sm/selflearn/samskrtam/quiz/dto/AnswerRequest';

const QuizPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { slug } = useParams<{ slug?: string }>(); // Only slug is needed now

  const [sessionId, setSessionId] = useState<string | null>(null);
  const [currentQuestionIndex, setCurrentQuestionIndex] = useState(0);
  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [selectedOptionId, setSelectedOptionId] = useState<string | null>(null);
  const [feedback, setFeedback] = useState<{ isCorrect: boolean; correctOptionId: string; explanationRu: string; explanationEn: string } | null>(null);
  const [startTime, setStartTime] = useState<number>(Date.now());
  const [hasAttemptedStart, setHasAttemptedStart] = useState(false); // New state to track start attempts

  const { data: quizSummary, isLoading: isQuizSummaryLoading, isError: isQuizSummaryError, error: quizSummaryError } = useQuizBySlug(slug || '');

  const startSessionMutation = useStartQuizSession();
  const submitAnswerMutation = useSubmitQuizAnswer();

  useEffect(() => {
    // Only attempt to start a session if quizSummary is loaded, no session is active, and we haven't attempted to start yet
    if (quizSummary && !sessionId && !hasAttemptedStart) {
      setHasAttemptedStart(true); // Mark that we are attempting to start
      startSessionMutation.mutate(
        { quizId: quizSummary.id, quizType: quizSummary.quizType, userLocale: t('locale') },
        {
          onSuccess: (data) => {
            setSessionId(data.sessionId);
            setQuestions(data.questions);
            setStartTime(Date.now());
          },
          onError: (err) => {
            console.error('Failed to start quiz session:', err);
            // The error message will be displayed by the conditional rendering below
            // No need to reset hasAttemptedStart, as we only want one attempt per quizSummary load
          },
        }
      );
    }
  }, [quizSummary, sessionId, t, startSessionMutation, hasAttemptedStart]); // Add hasAttemptedStart to dependencies

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
      { sessionId: sessionId, quizType: quizSummary.quizType, answerRequest: answerRequest, userLocale: t('locale') },
      {
        onSuccess: (data) => {
          setFeedback({
            isCorrect: data.isCorrect,
            correctOptionId: data.correctOptionId,
            explanationRu: data.explanationRu,
            explanationEn: data.explanationEn,
          });
          setStartTime(Date.now()); // Reset timer for next question
        },
        onError: (err) => {
          console.error('Failed to submit answer:', err);
          // Handle error
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
      // Quiz finished, navigate to result page
      navigate(`/quiz/result/${sessionId}`); // Assuming result page takes sessionId
    }
  };

  if (isQuizSummaryLoading || startSessionMutation.isLoading || !sessionId) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  // Display error messages if any mutation or query failed
  if (isQuizSummaryError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quiz.fetchError', { message: quizSummaryError?.message })} />
      </div>
    );
  }

  if (startSessionMutation.isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quiz.startError', { message: startSessionMutation.error?.message })} />
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
                label={option.formIast}
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
            <p className="text-lg">{t('locale') === 'ru' ? feedback.explanationRu : feedback.explanationEn}</p>
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
