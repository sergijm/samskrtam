import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { useNavigate, useParams } from 'react-router-dom';
import { useQuizList, useStartOrResumeQuizSession } from '../hooks/useQuiz';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useAuthStore } from '../store/authStore';
import { QuizListItem } from '../types/quiz';

const QuizzesPage = () => {
  const { t, i18n } = useTranslation();
  const { category } = useParams<{ category?: string }>();
  const navigate = useNavigate();
  const { data: quizList, isLoading, isError, error } = useQuizList(category);
  const { user } = useAuthStore();
  const startOrResumeMutation = useStartOrResumeQuizSession();

  const handleQuizClick = (quiz: QuizListItem) => {
    if (!user) {
      navigate('/login');
      return;
    }

    startOrResumeMutation.mutate(
      { quizId: quiz.id, quizType: quiz.quizType },
      {
        onSuccess: (data) => {
          const quizCategory = data.quizType.toLowerCase();
          // Pass the entire data object as state
          navigate(`/quiz/${quizCategory}/${quiz.slug}/${data.sessionId}`, { state: { sessionData: data } });
        },
        onError: (err) => {
          console.error('Failed to start or resume quiz session:', err);
          // Optionally, show an error message to the user
        },
      }
    );
  };

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
          const localizedTitle = i18n.language === 'ru' ? quiz.titleRu : quiz.titleEn;
          const localizedDescription = i18n.language === 'ru' ? quiz.descriptionRu : quiz.descriptionEn;

          return (
            <div key={quiz.id} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
              <div onClick={() => handleQuizClick(quiz)} className="w-full">
                <Card
                  title={localizedTitle}
                  subTitle={localizedDescription}
                  className="quiz-card flex flex-column align-items-center justify-content-between text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
                >
                  {/*<div className="flex flex-column align-items-center justify-content-center flex-grow-1">*/}
                  {/*  <i className="pi pi-question-circle text-5xl mb-3" />*/}
                  {/*</div>*/}
                </Card>
              </div>
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
