import React, { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useGrammarLesson } from '../../hooks/useLessons';

import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsTab } from '../../components/lesson/LessonStatsTab';
import { QuestionHistoryDialog } from '../../components/lesson/QuestionHistoryDialog';
import { CaseAggregationTable } from '../../components/lesson/CaseAggregationTable';
import { NumberAggregationTable } from '../../components/lesson/NumberAggregationTable';
import { GrammarDetailsTable } from '../../components/lesson/GrammarDetailsTable';
import GrammarParadigmCarousel from '../../components/lesson/GrammarParadigmCarousel';
import DeclensionEndingsReferenceTable from '../../components/lesson/DeclensionEndingsReferenceTable';
import { TabView, TabPanel } from 'primereact/tabview';
import { Skeleton } from 'primereact/skeleton';
import { aggregateByCase, aggregateByNumber } from '../../utils/grammarAggregation';
import { useDeclensionParadigm } from '../../hooks/useLessons';
import { vowelTypeToEndingsTable } from '../../data/aStemEndingsTable';

const GRAMMAR_TAB_STORAGE_KEY = 'grammar-lesson-active-tab';

function readSavedTab(): number {
  try {
    const raw = localStorage.getItem(GRAMMAR_TAB_STORAGE_KEY);
    if (raw !== null) {
      const parsed = parseInt(raw, 10);
      if (!isNaN(parsed) && parsed >= 0) return parsed;
    }
  } catch { /* ignore */ }
  return 0;
}

function saveTab(index: number) {
  try {
    localStorage.setItem(GRAMMAR_TAB_STORAGE_KEY, String(index));
  } catch { /* ignore */ }
}

const GrammarLessonPage = () => {
    const { slug } = useParams<{ slug: string }>();
  const { t, i18n } = useTranslation();
  const { data: lesson, isLoading, isError } = useGrammarLesson(slug || '');

      const [selectedCaseType, setSelectedCaseType] = useState<string>('');
      const [selectedNumberType, setSelectedNumberType] = useState<string>('');
      const [selectedGender, setSelectedGender] = useState<string>('');
      const [activeTab, setActiveTab] = useState<number>(readSavedTab);
      const [questionHistoryDialogVisible, setQuestionHistoryDialogVisible] = useState(false);
      const [sortField, setSortField] = useState<string>('caseType');
      const [sortOrder, setSortOrder] = useState<number>(1);

            // Tab 0 (Paradigms) is the default active tab — fetch immediately if it's the saved tab
            const [paradigmsTabOpened, setParadigmsTabOpened] = useState(readSavedTab() === 0);

      // Fetch first paradigm page to determine stem type for the endings reference table.
      // React Query deduplicates this with the carousel's own fetch for index 0.
      const { data: firstParadigmPage } = useDeclensionParadigm(slug || '', 0, paradigmsTabOpened);
      const endingsTableData = firstParadigmPage?.paradigm?.vowelType
        ? vowelTypeToEndingsTable[firstParadigmPage.paradigm.vowelType]
        : undefined;

    const caseAggregations = useMemo(() => {
    if (!lesson?.questions) return [];
    return aggregateByCase(lesson.questions);
  }, [lesson?.questions]);

  const numberAggregations = useMemo(() => {
    if (!lesson?.questions) return [];
    return aggregateByNumber(lesson.questions);
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

        const handleTabChange = (e: { index: number }) => {
      setActiveTab(e.index);
      saveTab(e.index);
      if (e.index === 0) {
        setParadigmsTabOpened(true);
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
                      <div className="flex align-items-center justify-content-between">
                        <LessonHeader title={lesson.titleRu} titleEn={lesson.titleEn} />
                        {lesson.statusSummary && (
                          <div className="flex align-items-center gap-1">
                            <span className="text-2xl font-bold">{lesson.statusSummary.total}</span>
                            <span className="text-base">{i18n.language === 'ru' ? 'Всего' : 'Total'}</span>
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

                    <div className=" mt-4">
                                                <TabView activeIndex={activeTab} onTabChange={handleTabChange}>
                                                                                                        <TabPanel header={i18n.language === 'ru' ? 'Парадигмы' : 'Paradigms'}>
                            {/* Static reference table of case endings — shown only when stem type is recognized */}
                            {endingsTableData && (
                              <DeclensionEndingsReferenceTable data={endingsTableData} />
                            )}
                            <GrammarParadigmCarousel slug={slug || ''} enabled={paradigmsTabOpened} />
                          </TabPanel>
                          <TabPanel header={i18n.language === 'ru' ? 'По падежам' : 'By Case'}>
                            <CaseAggregationTable aggregations={caseAggregations} quizSlug={slug || ''} />
                          </TabPanel>
                          <TabPanel header={i18n.language === 'ru' ? 'По числам' : 'By Number'}>
                            <NumberAggregationTable aggregations={numberAggregations} quizSlug={slug || ''} />
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



