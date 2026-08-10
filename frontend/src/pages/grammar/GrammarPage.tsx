import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useDeclensionLessons, useConjugationLessons } from '../../hooks/useQuiz';
import { Skeleton } from 'primereact/skeleton';
import { Message } from 'primereact/message';
import { QuizListItem } from '../../types/quiz';

const GrammarPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  // Данные с бэкенда
  const { data: declensions = [], isLoading: declLoading } = useDeclensionLessons();
  const { data: conjugations = [], isLoading: conjLoading } = useConjugationLessons();

  // Секции Сандхи
  const sandhiItems = [
    { title: t('curriculum.sandhi.vowel'),   link: '/lessons/grammar/sandhi-vowels-external', icon: 'pi pi-sort-alpha-down' },
    { title: t('curriculum.sandhi.consonant'), link: '/lessons/grammar/sandhi-consonants',   icon: 'pi pi-sort-alpha-down-alt' },
    { title: t('curriculum.sandhi.visarga'),  link: '/lessons/grammar/sandhi-visarga',       icon: 'pi pi-asterisk' },
  ];

  // Рендер одной карточки из API (склонения / спряжения)
  const renderLessonCard = (lesson: QuizListItem) => {
    const title = i18n.language === 'ru' ? lesson.titleRu : lesson.titleEn;
    const description = i18n.language === 'ru' ? lesson.descriptionRu : lesson.descriptionEn;
    return (
      <div key={lesson.id} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
        <div
          onClick={() => navigate(`/lessons/grammar/${lesson.slug}`)}
              className="p-card p-component lesson-card flex flex-column text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
            >
              <div className="p-card-body flex flex-column flex-grow-1">
            <div className="p-card-title">{title}</div>
            <div className="p-card-subtitle">{description}</div>
            <div className="p-card-footer" style={{ marginTop: 'auto' }}>
              <span className="text-lg font-bold">
                {lesson.totalQuestions} {t('quizzes.questions')}
              </span>
              </div>
            </div>
          </div>
      </div>
  );
};

  // Рендер хардкодной карточки (Сандхи)
  const renderStaticCard = (item: typeof sandhiItems[0], index: number) => (
    <div key={index} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
      <div
        onClick={() => navigate(item.link)}
        className="p-card p-component lesson-card flex flex-column text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
      >
        <div className="p-card-body flex flex-column flex-grow-1">
          <div className="p-card-title">{item.title}</div>
          <div className="flex-grow-1 flex align-items-center justify-content-center">
            <i className={`${item.icon} text-5xl text-primary`} />
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="flex flex-column p-4" style={{ maxWidth: '1600px', margin: '0 auto' }}>
      <h1 className="text-center mb-5">{t('nav.grammar')}</h1>

      {/* Секция 1: Сандхи */}
      <section className="mb-5">
        <h2 className="mb-3">{t('section.sandhi')}</h2>
        <div className="grid">
          {sandhiItems.map(renderStaticCard)}
        </div>
      </section>

      {/* Секция 2: Склонения */}
      <section className="mb-5">
        <h2 className="mb-3">{t('grammar.declensionsSectionTitle')}</h2>
        {declLoading ? (
          <div className="grid">
            {[1,2,3].map(i => (
              <div key={i} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2">
                <Skeleton width="100%" height="120px" />
              </div>
            ))}
          </div>
        ) : declensions.length === 0 ? (
          <Message severity="info" text={t('grammar.noLessonsFound')} />
        ) : (
          <div className="grid">{declensions.map(renderLessonCard)}</div>
        )}
      </section>

      {/* Секция 3: Спряжения */}
      <section className="mb-5">
        <h2 className="mb-3">{t('grammar.conjugationsSectionTitle')}</h2>
        {conjLoading ? (
          <div className="grid">
            {[1,2].map(i => (
              <div key={i} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2">
                <Skeleton width="100%" height="120px" />
              </div>
            ))}
          </div>
        ) : conjugations.length === 0 ? (
          <Message severity="info" text={t('grammar.noLessonsFound')} />
        ) : (
          <div className="grid">{conjugations.map(renderLessonCard)}</div>
        )}
      </section>
    </div>
  );
};

export default GrammarPage;

