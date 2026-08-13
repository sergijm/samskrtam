import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useDeclensionLessons } from '../../hooks/useQuiz';
import { ProgressBar } from 'primereact/progressbar';
import { Skeleton } from 'primereact/skeleton';
import { Message } from 'primereact/message';
import { ProgressSpinner } from 'primereact/progressspinner';
import { LessonItemDto } from '../../types/quiz';

const GrammarPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const isRu = i18n.language === 'ru';

  const { data: declensions = [], isLoading, isError } = useDeclensionLessons();

  const sandhiItems = [
    { title: t('grammar.sandhiExercisesTitle'), description: t('grammar.sandhiExercisesDescription'), link: '/grammar/emeneau-exercises', icon: 'pi-pencil' },
    { title: t('grammar.sandhiQuizzesTitle'),   description: t('grammar.sandhiQuizzesDescription'),   link: '/grammar/emeneau-quizzes',   icon: 'pi-question-circle' },
    { title: t('grammar.sandhiRulesTitle'),     description: t('grammar.sandhiRulesDescription'),     link: '/grammar/emeneau-rules',     icon: 'pi-book' },
  ];

  const totalQuestions = declensions.reduce((sum, d) => sum + d.totalQuestions, 0);

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
        <Message severity="error" text={t('quizzes.fetchError')} />
      </div>
    );
  }

  return (
    <div className="grammar-page p-3 md:p-4">
      {/* Hero */}
      <div className="lexicon-hero mb-5">
        <div className="flex flex-column md:flex-row md:align-items-end md:justify-content-between gap-3">
          <div>
            <h1 className="m-0 text-3xl">{t('nav.grammar')}</h1>
            <p className="m-0 text-500 mt-1">{t('grammar.subtitle')}</p>
          </div>
        </div>
        <div className="lexicon-hero-stats mt-4">
          <div className="flex justify-content-between align-items-center mb-1">
            <span className="font-semibold">
              {declensions.length} {isRu ? 'тем' : 'topics'}
            </span>
            <span className="font-semibold text-primary">
              {totalQuestions} {t('quizzes.questions')}
            </span>
          </div>
          <ProgressBar value={100} style={{ height: '0.75rem' }} />
        </div>
      </div>

      {/* Sandhi section */}
      <section className="mb-5">
        <div className="flex align-items-center gap-2 mb-3">
          <i className="pi pi-pencil text-lg text-primary" />
          <h2 className="m-0 text-xl">{t('grammar.sandhiSectionTitle')}</h2>
        </div>
        <div className="grid">
          {sandhiItems.map((item) => (
            <div key={item.icon} className="col-12 sm:col-6 lg:col-4 xl:col-3">
              <div
                className="grammar-card h-full cursor-pointer"
                onClick={() => navigate(item.link)}
              >
                <div className="flex align-items-center justify-content-between gap-2">
                  <span className="font-semibold">{item.title}</span>
                  <i className={`pi ${item.icon} text-2xl text-primary`} />
                </div>
                <div className="text-sm text-500 mt-1">
                  {item.description}
                </div>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* Declensions section */}
      <section className="mb-5">
        <div className="flex align-items-center gap-2 mb-3">
          <i className="pi pi-table text-lg text-primary" />
          <h2 className="m-0 text-xl">{t('grammar.declensionsSectionTitle')}</h2>
        </div>
        {declensions.length === 0 ? (
          <Message severity="info" text={t('grammar.noLessonsFound')} />
        ) : (
          <div className="grid">
            {declensions.map((lesson) => (
              <GrammarCard key={lesson.id} lesson={lesson} onClick={() => navigate(`/lessons/grammar/${lesson.slug}`)} />
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

const GrammarCard = ({ lesson, onClick }: { lesson: LessonItemDto; onClick: () => void }) => {
  const { i18n } = useTranslation();
  const isRu = i18n.language === 'ru';
  const title = isRu ? lesson.titleRu : lesson.titleEn;

  return (
    <div className="col-12 sm:col-6 lg:col-4 xl:col-3">
      <div className="grammar-card h-full cursor-pointer" onClick={onClick}>
        <div className="flex align-items-center justify-content-between gap-2">
          <span className="font-semibold">{title}</span>
          <span className="text-sm text-500">
            {lesson.totalQuestions} {isRu ? 'вопр.' : 'q.'}
          </span>
        </div>
        {lesson.difficulty && (
          <div className="text-sm text-500 mt-1">
            {lesson.difficulty}
          </div>
        )}
        <ProgressBar
          value={lesson.totalQuestions > 0 ? Math.round((lesson.learnedWords / (lesson.learnedWords + 1 || lesson.totalQuestions)) * 100) : 0}
          style={{ height: '0.4rem' }}
          className="mt-2"
        />
      </div>
    </div>
  );
};

export default GrammarPage;