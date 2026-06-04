import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { Link, useParams } from 'react-router-dom';
import { useQuizList } from '../hooks/useQuiz';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';

const QuizzesPage = () => {
  const { t } = useTranslation();
  const { category } = useParams<{ category?: string }>(); // Get category from URL
  const { data: quizList, isLoading, isError, error } = useQuizList(category); // Pass category to the hook

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
        <Message severity="error" text={t('quizzes.fetchError', { message: error?.message })} />
      </div>
    );
  }

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <h1 className="text-center mb-5">{t('quizzes.title')}</h1>
      <div className="grid justify-content-center w-full" style={{ maxWidth: '1200px' }}>
        {quizList?.map((quiz) => {
          let quizLink = '';
          if (quiz.quizType === 'VOCABULARY') {
            quizLink = `/quiz/vocabulary/${quiz.slug}`;
          } else {
            // For grammar-related quizzes, use quiz.slug in the URL
            quizLink = `/quiz/grammar/${quiz.slug}`;
          }

          return (
            <div key={quiz.id} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
              <Link to={quizLink} className="no-underline h-full flex w-full">
                <Card
                  title={quiz.title}
                  subTitle={quiz.description}
                  className="quiz-card flex flex-column align-items-center justify-content-between text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
                >
                  <div className="flex flex-column align-items-center justify-content-center flex-grow-1">
                    <i className="pi pi-question-circle text-5xl mb-3" />
                    <p className="text-sm text-color-secondary">{quiz.description}</p>
                    <p className="text-xs text-color-secondary mt-2">{t('quiz.totalQuestions', { count: quiz.totalQuestions })}</p>
                  </div>
                </Card>
              </Link>
            </div>
          );
        })}
        {quizList?.length === 0 && (
          <div className="col-12 text-center">
            <Message severity="info" text={t('quizzes.noQuizzesFound')} />
          </div>
        )}
      </div>
    </div>
  );
};

export default QuizzesPage;
