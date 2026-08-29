import { useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { useGrammarLesson } from '../../hooks/useLessons';
import { useStartOrResumeWithStatusFilter } from '../../hooks/useQuiz';
import { getQuizCategory, LessonType } from '../../types/quiz';
import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsTab } from '../../components/lesson/LessonStatsTab';
import CaseMeaningsLessonContent from './CaseMeaningsLessonContent';
import { quizApi } from '../../api/quizApi';

const CaseMeaningsLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const { i18n } = useTranslation();
  const navigate = useNavigate();
  const { data: lesson, isLoading, isError } = useGrammarLesson(slug || '');
  const startOrResumeWithFilter = useStartOrResumeWithStatusFilter();

  const handleStartQuiz = useCallback((statusFilter: string) => {
    if (!lesson) return;
    const lessonType = lesson.type as LessonType;
    const quizCategory = getQuizCategory(lessonType);
    startOrResumeWithFilter.mutate(
      { quizId: lesson.lessonId, lessonType, statusFilter },
      {
        onSuccess: (data) => {
          window.open(`/quiz/${quizCategory}/${slug}/${data.sessionId}`, '_blank');
        },
        onError: (e) => console.error('startOrResume failed:', e),
      },
    );
  }, [lesson, slug, navigate, startOrResumeWithFilter]);

  const handleCaseQuiz = async (caseType: string) => {
    if (!slug) return;
    try {
      const response = await quizApi.composeSession({
        topicCode: slug,
        progressTagSetId: caseType,
        limit: 10,
        userLocale: i18n.language,
      });
      window.open(`/quiz/grammar/${slug}/${response.data.sessionId}`, '_blank');
    } catch { /* ignore */ }
  };

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{i18n.language === 'ru' ? 'Ошибка загрузки' : 'Load error'}</div>
      </div>
    );
  }

  return (
    <div className="p-4">
      {isLoading || !lesson ? (
        <div className="p-4">
          <Skeleton width="100%" height="40px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="200px" />
        </div>
      ) : (
        <>
          <div className="card mb-3">
            <div className="flex align-items-center justify-content-between">
              <LessonHeader title={lesson.titleRu} titleEn={lesson.titleEn} />
              {lesson.statusSummary && (
                <div className="flex align-items-center gap-1">
                  <span className="text-2xl font-bold">{lesson.statusSummary.total}</span>
                  <span className="text-base">{i18n.language === 'ru' ? 'вопросов' : 'questions'}</span>
                </div>
              )}
            </div>
          </div>

          {lesson.statusSummary && (
            <div className="mb-3">
              <LessonStatsTab
                statusSummary={lesson.statusSummary}
                onStartQuiz={handleStartQuiz}
              />
            </div>
          )}

          <div className="mt-4">
            <CaseMeaningsLessonContent
              caseAggregations={lesson.caseAggregations}
              onStartQuiz={handleCaseQuiz}
            />
          </div>
        </>
      )}
    </div>
  );
};

export default CaseMeaningsLessonPage;