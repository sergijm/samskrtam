import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Card } from 'primereact/card';
import { ProgressBar } from 'primereact/progressbar';

import { useQuizSession } from '../hooks/useQuizSession';
import QuestionRenderer from '../components/quiz/QuestionRenderer';
import QuizQuestionPanel from '../components/quiz/QuizQuestionPanel';
import QuizCaseSelectPanel from '../components/quiz/QuizCaseSelectPanel';
import QuizEndingMatchPanel from '../components/quiz/QuizEndingMatchPanel';
import QuizMatchPanel from '../components/quiz/QuizMatchPanel';
import QuizFreeTextPanel from '../components/quiz/QuizFreeTextPanel';
import QuizFeedbackPanel from '../components/quiz/QuizFeedbackPanel';

const QuizPage = () => {
  const { t, i18n } = useTranslation();
  const { slug, sessionId: sessionIdFromParams } = useParams<{ slug?: string; sessionId?: string }>();

  const {
    currentQuestionIndex,
    questions,
    currentQuestion,
    feedback,
    isLastQuestion,
    quizSummaryData,
    selectedOptionId,
    isSubmittingAnswer,
    isLoading,
    isError,
    errorMessage,
    handleSubmitAnswer,
    handleNextQuestion,
    hasAttemptedSessionLoad,
  } = useQuizSession(slug, sessionIdFromParams);

  if (isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quiz.fetchError', { message: errorMessage })} />
      </div>
    );
  }

  if (questions.length === 0 && hasAttemptedSessionLoad) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="info" text={t('quiz.noQuestions')} />
      </div>
    );
  }

  if (!currentQuestion && questions.length > 0) {
    return null;
  }

  const progress = Math.round(((currentQuestionIndex + (feedback ? 1 : 0)) / questions.length) * 100);
  const localizedTitle = i18n.language === 'ru' ? quizSummaryData?.quizTitleRu : quizSummaryData?.quizTitleEn;
  const localizedDesc = i18n.language === 'ru' ? quizSummaryData?.quizDescriptionRu : quizSummaryData?.quizDescriptionEn;

  return (
        <div className="flex flex-column align-items-center p-4 w-full">
      <Card className="lesson-container w-full" style={{ maxWidth: '800px' }}>
        {localizedTitle && <h1 className="text-center mb-3">{localizedTitle}</h1>}
        {localizedDesc && <p className="text-center text-color-secondary mb-4">{localizedDesc}</p>}
        <ProgressBar value={progress} className="mb-4" />
        {currentQuestion && quizSummaryData && (
          <>
            {currentQuestion.answerMode ? (
              <QuestionRenderer
                question={currentQuestion}
                selectedOptionId={selectedOptionId}
                currentQuestionIndex={currentQuestionIndex}
                totalQuestions={questions.length}
                disabled={!!feedback || isSubmittingAnswer}
                feedback={feedback}
                onSubmit={handleSubmitAnswer}
              />
            ) : currentQuestion.questionType === 'CASE_BY_FORM' ? (
              <QuizCaseSelectPanel
              question={currentQuestion}
                selectedOptionId={selectedOptionId}
              disabled={!!feedback || isSubmittingAnswer}
              feedback={feedback}
              onSelectOption={handleSubmitAnswer}
            />
            ) : currentQuestion.questionType === 'ENDING_MATCH' ? (
              <QuizEndingMatchPanel
                question={currentQuestion}
                disabled={!!feedback || isSubmittingAnswer}
                feedback={feedback}
                onSelectOption={handleSubmitAnswer}
              />
            ) : currentQuestion.questionType === 'MATCHING' ? (
              <QuizMatchPanel
                question={currentQuestion}
                disabled={!!feedback || isSubmittingAnswer}
                feedback={feedback}
                onSelectOption={handleSubmitAnswer}
              />
            ) : currentQuestion.questionType === 'FREE_TEXT' ? (
              <QuizFreeTextPanel
                question={currentQuestion}
                disabled={isSubmittingAnswer}
                currentQuestionIndex={currentQuestionIndex}
                totalQuestions={questions.length}
                feedback={feedback}
                onFreeTextSubmit={handleSubmitAnswer}
              />
            ) : (
              <QuizQuestionPanel
                question={currentQuestion}
                currentQuestionIndex={currentQuestionIndex}
                totalQuestions={questions.length}
                selectedOptionId={selectedOptionId}
                disabled={!!feedback || isSubmittingAnswer}
                lessonType={quizSummaryData.lessonType}
                feedback={feedback}
                onSelectOption={handleSubmitAnswer}
              />
        )}
            {feedback && (
              <QuizFeedbackPanel
                isCorrect={feedback.isCorrect}
                correctAnswerText={feedback.correctAnswerText}
                explanation={feedback.explanation}
                isLastQuestion={isLastQuestion}
                onNext={handleNextQuestion}
              />
        )}
          </>
        )}
      </Card>
    </div>
  );
};

export default QuizPage;

