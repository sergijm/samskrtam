import { useParams, useSearchParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Card } from 'primereact/card';
import { ProgressBar } from 'primereact/progressbar';

import { useQuizSession } from '../hooks/useQuizSession';
import { FilterParams } from '../api/quizApi';
import QuizQuestionPanel from '../components/quiz/QuizQuestionPanel';
import QuizCaseSelectPanel from '../components/quiz/QuizCaseSelectPanel';
import QuizEndingMatchPanel from '../components/quiz/QuizEndingMatchPanel';
import QuizFeedbackPanel from '../components/quiz/QuizFeedbackPanel';

const QuizPage = () => {
  const { t, i18n } = useTranslation();
  const { slug, sessionId: sessionIdFromParams } = useParams<{ slug?: string; sessionId?: string }>();
  const [searchParams] = useSearchParams();

    // Считываем фильтры из query-параметров URL
  const filterScope = searchParams.get('filterScope') as FilterParams['filterScope'] | null;
  const filterCaseType = searchParams.get('filterCaseType') || undefined;
  const filterNumberType = searchParams.get('filterNumberType') || undefined;
  const filterGender = searchParams.get('filterGender') || undefined;
  const filterCombinations = searchParams.get('filterCombinations') || undefined;
  const statusFilter = searchParams.get('statusFilter') || undefined;

  const filterParams: FilterParams | undefined = filterScope
    ? {
        filterScope,
        filterCaseTypes: filterScope === 'CASE_ONLY' ? filterCaseType : undefined,
        filterNumberTypes: filterScope === 'NUMBER_ONLY' ? filterNumberType : undefined,
        filterCombinations: filterScope === 'CASE_NUMBER_GENDER'
          ? (filterCombinations || (filterCaseType && filterNumberType && filterGender
              ? `${filterCaseType}:${filterNumberType}:${filterGender}`
              : undefined))
          : undefined,
      }
    : undefined;

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
  } = useQuizSession(slug, sessionIdFromParams, filterParams, statusFilter);

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
            {currentQuestion.questionType === 'CASE_BY_FORM' ? (
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

