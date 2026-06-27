import React from 'react';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { useNavigate, useParams } from 'react-router-dom'; // Keep useParams for potential future dynamic routes
import { useQuizList, useStartOrResumeQuizSession } from '../hooks/useQuiz';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useMe } from '../hooks/useUser';
import {isDeclensionsQuiz, isVocabularyQuiz, QuizListItem, LessonType} from '../types/quiz';

interface QuizzesPageProps {
  category?: string; // Accept category as a prop
}

const QuizzesPage: React.FC<QuizzesPageProps> = ({ category: propCategory }) => { // Rename prop to avoid conflict
  const { t, i18n } = useTranslation();
  // const { category: urlCategory } = useParams<{ category?: string }>(); // No longer needed for filtering
  const navigate = useNavigate();
  // Use propCategory for fetching lesson list
  const { data: quizList, isLoading: isQuizListLoading, isError: isQuizListError, error: quizListError } = useQuizList(propCategory);
  const { data: user, isLoading: isUserLoading } = useMe();
  const startOrResumeMutation = useStartOrResumeQuizSession();

  const handleQuizClick = (lesson: QuizListItem) => {
    if (!user) {
      navigate('/login');
      return;
    }

    // For vocabulary quizzes, navigate to lesson page instead of lesson page
    if ( isVocabularyQuiz(lesson.lessonType)) {
      navigate(`/lessons/vocabulary/${lesson.slug}`);
      return;
    }

    // For grammar quizzes, navigate to lesson page instead of lesson page
    if (isDeclensionsQuiz(lesson.lessonType)) {
      navigate(`/lessons/grammar/${lesson.type.toLowerCase()}`);
      return;
    }

    // Fallback to original behavior if needed
    startOrResumeMutation.mutate(
      { quizId: lesson.id, lessonType: lesson.lessonType },
      {
        onSuccess: (data) => {
          const quizCategory = data.lessonType.toLowerCase();
          navigate(`/lesson/${quizCategory}/${lesson.slug}/${data.sessionId}`, { state: { sessionData: data } });
        },
        onError: (err) => {
          console.error('Failed to start or resume lesson session:', err);
        },
      }
    );
  };

  if (isQuizListLoading || isUserLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isQuizListError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('quizzes.fetchError', { message: quizListError?.message })} />
      </div>
    );
  }

  // Determine page title based on propCategory
  let pageTitle = t('quizzes.title');
  if (propCategory === 'declensions') {
    pageTitle = t('grammar.declensionsTitle');
  } else if (propCategory === 'conjugations') {
    pageTitle = t('grammar.conjugationsTitle');
  } else if (propCategory === 'vocabulary') {
    pageTitle = t('nav.vocabulary');
  } else if (propCategory === 'grammar') {
    pageTitle = t('nav.grammar');
  }

  return (
    <div className="flex flex-column align-items-center justify-content-center p-4">
      <h1 className="text-center mb-5">{pageTitle}</h1>
      <div className="grid justify-content-center w-full" style={{ maxWidth: '1600px' }}>
        {quizList?.map((lesson) => {
          const localizedTitle = i18n.language === 'ru' ? lesson.titleRu : lesson.titleEn;
          const localizedDescription = i18n.language === 'ru' ? lesson.descriptionRu : lesson.descriptionEn;

          return (
            <div key={lesson.id} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
              <div
                onClick={() => handleQuizClick(lesson)}
                className="p-card p-component lesson-card flex flex-column text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
              >
                <div className="p-card-body flex flex-column flex-grow-1">
                  <div className="p-card-title">{localizedTitle}</div>
                  <div className="p-card-subtitle">{localizedDescription}</div>
                  <div className="p-card-footer" style={{ marginTop: 'auto' }}>
                    <div className="text-lg font-bold">
                      {isVocabularyQuiz(lesson.lessonType) ? (
                          <span>{lesson.wordCount} {t('quizzes.words')}</span>
                      ) : (
                          <span>{lesson.totalQuestions} {t('quizzes.questions')}</span>
                      )}
                    </div>
                  </div>
                </div>
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
