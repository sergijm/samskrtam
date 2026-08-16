import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Skeleton } from 'primereact/skeleton';
import { TabView, TabPanel } from 'primereact/tabview';
import { useGrammarLesson } from '../../hooks/useLessons';
import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsTab } from '../../components/lesson/LessonStatsTab';
import { MiniProgressBar } from '../../components/common/MiniProgressBar';
import CaseMeaningsLessonContent from './CaseMeaningsLessonContent';
import { quizApi } from '../../api/quizApi';
import type { CaseAggregation } from '../../types/lesson';

const CaseMeaningsLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const { i18n } = useTranslation();
  const navigate = useNavigate();
  const { data: lesson, isLoading, isError } = useGrammarLesson(slug || '');
  const [activeTab, setActiveTab] = useState(0);

  const handleCaseQuiz = async (caseType: string) => {
    if (!slug) return;
    try {
      const response = await quizApi.composeSession({
        topicCode: slug,
        progressTagSetId: caseType,
        limit: 10,
        userLocale: i18n.language,
      });
      navigate(`/quiz/grammar/${slug}/${response.data.sessionId}`);
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
                quizPath={`/quiz/grammar/${slug}`}
              />
            </div>
          )}

          <div className="mt-4">
            <TabView activeIndex={activeTab} onTabChange={(e) => setActiveTab(e.index)}>
              <TabPanel header={i18n.language === 'ru' ? 'Урок' : 'Lesson'}>
                <CaseMeaningsLessonContent />
              </TabPanel>
              <TabPanel header={i18n.language === 'ru' ? 'Прогресс' : 'Progress'}>
                <div className="flex flex-column gap-1">
                  {(lesson.caseAggregations ?? []).map((agg: CaseAggregation) => (
                    <div
                      key={agg.caseType}
                      className="flex align-items-center gap-2 p-1 hover:surface-200 cursor-pointer"
                      onClick={() => handleCaseQuiz(agg.caseType)}
                    >
                      <span className="font-medium text-sm w-8rem">
                        {i18n.language === 'ru' ? agg.caseRu : agg.caseEn}
                      </span>
                      <MiniProgressBar
                        value={agg.aggregatedProgress}
                        status={agg.status}
                        width="110px"
                        className="justify-content-start"
                      />
                      <i
                        className="pi pi-angle-double-right text-xl text-orange-500"
                        style={{ color: '#f97316' }}
                      />
                    </div>
                  ))}
                  {(!lesson.caseAggregations || lesson.caseAggregations.length === 0) && (
                    <div className="text-color-secondary text-center p-4">
                      {i18n.language === 'ru' ? 'Нет данных по падежам' : 'No case data available'}
                    </div>
                  )}
                </div>
              </TabPanel>
            </TabView>
          </div>
        </>
      )}
    </div>
  );
};

export default CaseMeaningsLessonPage;