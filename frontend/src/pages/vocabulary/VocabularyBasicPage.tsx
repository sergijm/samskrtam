import { useTranslation } from 'react-i18next';
import { DataView } from 'primereact/dataview';
import { ProgressBar } from 'primereact/progressbar';
import { Button } from 'primereact/button';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useNavigate } from 'react-router-dom';
import { useQuizList, useStartOrResumeQuizSession } from '../../hooks/useQuiz';
import { useMe } from '../../hooks/useUser';
import { LessonItemDto } from '../../types/quiz';

const VocabularyBasicPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { data: quizList, isLoading, isError, error } = useQuizList('vocabulary-basic');
  const { data: user, isLoading: isUserLoading } = useMe();
  const startOrResumeMutation = useStartOrResumeQuizSession();

  const handleClick = (lesson: LessonItemDto) => {
    if (!user) {
      navigate('/login');
      return;
    }
    startOrResumeMutation.mutate(
      { quizId: lesson.id, lessonType: lesson.lessonType },
      {
        onSuccess: (data) => {
          const quizCategory = data.lessonType.toLowerCase();
          window.open(`/quiz/${quizCategory}/${lesson.slug}/${data.sessionId}`, '_blank');
        },
        onError: (err) => {
          console.error('Failed to start or resume lesson session:', err);
        },
      }
    );
  };

  if (isLoading || isUserLoading) {
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

  const listItem = (lesson: LessonItemDto) => {
    const title = i18n.language === 'ru' ? lesson.titleRu : lesson.titleEn;
    const description = i18n.language === 'ru' ? lesson.descriptionRu : lesson.descriptionEn;
    const pct = lesson.totalWordsOwn > 0
      ? Math.round((lesson.learnedWords / lesson.totalWordsOwn) * 100)
      : 0;

    return (
      <div
        className="flex flex-column p-3 gap-2 cursor-pointer border-1 border-round-lg surface-border surface-card hover:surface-hover transition-all transition-duration-200"
        onClick={() => handleClick(lesson)}
      >
        <div className="flex align-items-center justify-content-between gap-3">
          <div className="flex flex-column flex-1 gap-1">
            <div className="font-bold text-lg">{title}</div>
            <div className="text-color-secondary text-sm">{description}</div>
          </div>
          <div className="flex align-items-center gap-3">
            <div className="flex flex-column align-items-end gap-1">
              <ProgressBar
                value={pct}
                style={{ height: '8px', width: '120px' }}
                showValue={false}
              />
              <span className="text-sm">
                <span style={{ color: 'var(--green-500)', fontWeight: 600 }}>
                  {lesson.learnedWords}
                </span>
                /{lesson.totalWordsOwn} {t('quizzes.words')}
              </span>
            </div>
            <Button
              icon="pi pi-arrow-right"
              rounded
              text
              severity="info"
              aria-label={t('common.go')}
            />
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="flex flex-column p-4">
      <h1 className="text-center mb-4">{t('vocabulary.basicVocabularyTitle')}</h1>
      <div className="mx-auto w-full" style={{ maxWidth: '900px' }}>
        <DataView
          value={quizList ?? []}
          layout="list"
          listTemplate={(items) => (
            <div className="flex flex-column gap-2">
              {items.map((lesson) => listItem(lesson))}
            </div>
          )}
          paginator={false}
          emptyMessage={
            <div className="text-center p-4">
              <Message severity="info" text={t('quizzes.noQuizzesFound')} />
            </div>
          }
        />
      </div>
    </div>
  );
};

export default VocabularyBasicPage;