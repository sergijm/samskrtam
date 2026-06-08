import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { Link, useParams } from 'react-router-dom';
import { useQuizList } from '../hooks/useQuiz';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useQueries } from '@tanstack/react-query'; // Import useQueries
import { useAuthStore } from '../store/authStore'; // Import useAuthStore
import { quizApi } from '../api/quizApi'; // Import quizApi
import { QuizProgress } from '../types/quiz'; // Import QuizProgress

const QuizzesPage = () => {
  const { t } = useTranslation();
  const { category } = useParams<{ category?: string }>(); // Get category from URL
  const { data: quizList, isLoading, isError, error } = useQuizList(category); // Pass category to the hook
  const { user } = useAuthStore(); // Get user from auth store
  const userId = user?.id;

  // Define queries for quiz progress using useQueries
  const quizProgressQueries = useQueries({
    queries: (quizList || []).map(quiz => ({
      queryKey: ['quizProgress', quiz.id, userId], // queryKey already uses quiz.id for uniqueness
      queryFn: () => userId ? quizApi.getLatestUnfinishedQuizProgress(userId, quiz.id).then(res => res.data) : Promise.reject('User ID not available'), // Pass quiz.id instead of quiz.quizType
      enabled: !!userId, // Only run query if userId is available
      staleTime: 5 * 60 * 1000, // 5 minutes
    })),
  });

  // Check if any of the progress queries are loading
  const isProgressLoading = quizProgressQueries.some(query => query.isLoading);

  if (isLoading || isProgressLoading) {
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
        {quizList?.map((quiz, index) => {
          const quizCategory = quiz.quizType === 'VOCABULARY' ? 'vocabulary' : 'grammar';
          let quizLink = '';
          const quizProgress = quizProgressQueries[index]?.data as QuizProgress | undefined;

          if (quizProgress && quizProgress.found && quizProgress.sessionId) {
            // If an unfinished quiz is found, link to resume it
            quizLink = `/quiz/${quizCategory}/${quiz.slug}/${quizProgress.sessionId}`;
          } else {
            // Otherwise, link to start a new quiz
            quizLink = `/quiz/${quizCategory}/${quiz.slug}`;
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
                    {/* Removed duplicate quiz.description */}
                    {quizProgress && quizProgress.found && ( // Only render if progress is found
                      <p className="text-xs text-color-secondary mt-2">
                        {t('dashboard.quizProgress', { answered: quizProgress.answeredQuestions, total: quizProgress.totalQuestions })}
                      </p>
                    )}
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