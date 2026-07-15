import React, { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useGrammarLesson } from '../../hooks/useLessons';

import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsBadges } from '../../components/lesson/LessonStatsBadges';
import { QuestionHistoryDialog } from '../../components/lesson/QuestionHistoryDialog';
import { CaseAggregationTable } from '../../components/lesson/CaseAggregationTable';
import { GrammarDetailsTable } from '../../components/lesson/GrammarDetailsTable';
import { TabView, TabPanel } from 'primereact/tabview';
import { Button } from 'primereact/button';
import { Skeleton } from 'primereact/skeleton';
import { aggregateByCase } from '../../utils/grammarAggregation';

const GrammarLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const { data: lesson, isLoading, isError } = useGrammarLesson(slug || '');

      const [selectedCaseType, setSelectedCaseType] = useState<string>('');
  const [selectedNumberType, setSelectedNumberType] = useState<string>('');
  const [selectedGender, setSelectedGender] = useState<string>('');
  const [activeTab, setActiveTab] = useState<number>(0);
  const [questionHistoryDialogVisible, setQuestionHistoryDialogVisible] = useState(false);
  const [sortField, setSortField] = useState<string>('caseType');
  const [sortOrder, setSortOrder] = useState<number>(1);

  const caseAggregations = useMemo(() => {
    if (!lesson?.questions) return [];
    return aggregateByCase(lesson.questions);
  }, [lesson?.questions]);

  const sortedForms = useMemo(() => {
    if (!lesson?.questions) return [];
    const forms = [...lesson.questions];
    if (sortField) {
      forms.sort((a, b) => {
        const valueA: any = a[sortField as keyof typeof a];
        const valueB: any = b[sortField as keyof typeof b];
        if (typeof valueA === 'string' && typeof valueB === 'string') {
          return sortOrder * valueA.localeCompare(valueB);
        }
        if (valueA < valueB) return sortOrder * -1;
        if (valueA > valueB) return sortOrder;
        return 0;
      });
    }
    return forms;
  }, [lesson?.questions, sortField, sortOrder]);
  
    const handleSort = (field: string) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 1 ? -1 : 1);
    } else {
      setSortField(field);
      setSortOrder(1);
    }
  };

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t('lesson.loadError')}</div>
      </div>
    );
  }

  return (
    <div className="p-4">
      {isLoading || !lesson ? (
        <div className="p-4">
          <Skeleton width="100%" height="40px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-4" />
          <Skeleton width="100%" height="200px" />
        </div>
      ) : (
        <>
          <div className="card mb-3">
            <div className="flex flex-wrap gap-3 align-items-center justify-content-between">
              <LessonHeader title={lesson.titleRu} titleEn={lesson.titleEn} />
              <div className="flex flex-wrap gap-3 align-items-center">
                <LessonStatsBadges
                  statusSummary={lesson.statusSummary}
                  quizPath={`/quiz/grammar/${slug}`}
                />
                <Button
                  label={i18n.language === 'ru' ? 'Начать квиз' : 'Start Quiz'}
                  icon="pi pi-play"
                  onClick={() => navigate(`/quiz/grammar/${slug}`)}
                  disabled={lesson.totalQuestions === 0}
                />
              </div>
            </div>
          </div>

          <div className="p-4 mt-4">
            <div className="flex justify-content-between align-items-center mb-4">
              <h3>{i18n.language === 'ru' ? 'Вопросы урока' : 'Lesson Questions'}</h3>
            </div>

            <TabView activeIndex={activeTab} onTabChange={(e) => setActiveTab(e.index)}>
              <TabPanel header={i18n.language === 'ru' ? 'По падежам' : 'By Case'}>
                <CaseAggregationTable aggregations={caseAggregations} quizSlug={slug || ''} />
              </TabPanel>
              <TabPanel header={i18n.language === 'ru' ? 'Подробно' : 'Details'}>
                <GrammarDetailsTable
                  forms={sortedForms}
                  quizSlug={slug || ''}
                  sortField={sortField}
                  sortOrder={sortOrder}
                  onSort={handleSort}
                />
              </TabPanel>
            </TabView>
          </div>

          <QuestionHistoryDialog
            visible={questionHistoryDialogVisible}
            onHide={() => setQuestionHistoryDialogVisible(false)}
            lessonSlug={slug}
            caseType={selectedCaseType}
            numberType={selectedNumberType}
            gender={selectedGender}
          />
        </>
      )}
    </div>
  );
};

export default GrammarLessonPage;

