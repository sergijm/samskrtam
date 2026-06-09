import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { Link, useParams } from 'react-router-dom';
import { useQuizList } from '../hooks/useQuiz';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useQueries } from '@tanstack/react-query';
import { useAuthStore } from '../store/authStore';
import { quizApi } from '../api/quizApi';
import { QuizProgress } from '../types/quiz';

const QuizzesPage = () => {
  const { t, i18n } = useTranslation(); // Get i18n instance for current language
  const { category } = useParams<{ category?: string }>();
  const { data: quizList, isLoading, isError, error } = useQuizList(category);
  const { user } = useAuthStore();
  const userId = user?.id;

  const quizProgressQueries = useQueries({
    queries: (quizList || []).map(quiz => ({
      queryKey: ['quizProgress', quiz.id, userId],
      queryFn: () => userId ? quizApi.getLatestUnfinishedQuizProgress(userId, quiz.id).then(res => res.data) : Promise.reject('User ID not available'),
      enabled: !!userId,
      staleTime: 5 * 60 * 1000,
    })),
  });

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

          // Determine localized title and description
          const localizedTitle = i18n.language === 'ru' ? quiz.titleRu : quiz.titleEn;
          const localizedDescription = i18n.language === 'ru' ? quiz.descriptionRu : quiz.descriptionEn;

          if (quizProgress && quizProgress.found && quizProgress.sessionId) {
            quizLink = `/quiz/${quizCategory}/${quiz.slug}/${quizProgress.sessionId}`;
          } else {
            quizLink = `/quiz/${quizCategory}/${quiz.slug}`;
          }

          return (
            <div key={quiz.id} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
              <Link to={quizLink} className="no-underline h-full flex w-full">
                <Card
                  title={localizedTitle}
                  subTitle={localizedDescription}
                  className="quiz-card flex flex-column align-items-center justify-content-between text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
                >
                  <div className="flex flex-column align-items-center justify-content-center flex-grow-1">
                    <i className="pi pi-question-circle text-5xl mb-3" />
                    {quizProgress && quizProgress.found && (
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
