import React, { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useGrammarLesson } from '../../hooks/useLessons';

import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsBadges } from '../../components/lesson/LessonStatsBadges';
import { WordStatusIcon } from '../../components/lesson/WordStatusIcon';
import { QuestionHistoryDialog } from '../../components/lesson/QuestionHistoryDialog';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { TabView, TabPanel } from 'primereact/tabview';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';
import { useTranslation } from 'react-i18next';
import type { WordStatus, GrammarQuestionProgress } from '../../types/lesson';

const CASE_TYPES = ['NOMINATIVE', 'ACCUSATIVE', 'INSTRUMENTAL', 'DATIVE', 'ABLATIVE', 'GENITIVE', 'LOCATIVE', 'VOCATIVE'];
const MASTERY_THRESHOLD = 90;

interface CaseAggregation {
  caseType: string;
  caseRu: string;
  caseEn: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: WordStatus;
}

const aggregateByCase = (questions: GrammarQuestionProgress[]): CaseAggregation[] => {
  const grouped = new Map<string, GrammarQuestionProgress[]>();
  for (const q of questions) {
    const ct = q.caseType;
    if (!grouped.has(ct)) grouped.set(ct, []);
    grouped.get(ct)!.push(q);
  }
  const result: CaseAggregation[] = [];
  for (const caseType of CASE_TYPES) {
    const items = grouped.get(caseType);
    if (!items || items.length === 0) continue;
    const total = items.length;
    const learned = items.filter(q => q.score >= MASTERY_THRESHOLD).length;
    const progress = total > 0 ? Math.round((learned / total) * 100) : 0;
    const firstItem = items[0];
    const status: WordStatus = progress >= MASTERY_THRESHOLD ? 'MASTERED' : 'LEARNING';
    result.push({ caseType, caseRu: firstItem.caseRu, caseEn: firstItem.caseEn, aggregatedProgress: progress, totalCombinations: total, learnedCombinations: learned, status });
  }
  return result;
};

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

  const handleQuestionHistoryClick = (caseType: string, numberType: string, gender: string) => {
    setSelectedCaseType(caseType);
    setSelectedNumberType(numberType);
    setSelectedGender(gender);
    setQuestionHistoryDialogVisible(true);
  };
  
    const handleStartQuiz = () => {
      if (lesson) {
        navigate(`/quiz/grammar/${slug}`);
      }
    };

    const handleSort = (field: string) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 1 ? -1 : 1);
    } else {
      setSortField(field);
      setSortOrder(1);
    }
  };

  const sortedForms = useMemo(() => {
    if (!lesson?.questions) return [];
    const forms = [...lesson.questions];
    if (sortField) {
      forms.sort((a, b) => {
        let valueA: any = a[sortField as keyof typeof a];
        let valueB: any = b[sortField as keyof typeof b];
        if (typeof valueA === 'string' && typeof valueB === 'string') {
          return sortOrder * valueA.localeCompare(valueB);
        }
        if (valueA < valueB) return sortOrder * -1;
        if (valueA > valueB) return sortOrder * 1;
        return 0;
      });
    }
    return forms;
  }, [lesson?.questions, sortField, sortOrder]);
  
  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">Ошибка загрузки урока</div>
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
              <LessonHeader 
                title={lesson.titleRu} 
                titleEn={lesson.titleEn}
              />
              <div className="flex flex-wrap gap-3 align-items-center">
                <LessonStatsBadges 
                  statusSummary={lesson.statusSummary} 
                  quizPath={`/quiz/grammar/${slug}`}
                />
                <Button 
                  label="Начать квиз" 
                  icon="pi pi-play"
                  onClick={handleStartQuiz}
                  disabled={lesson.totalQuestions === 0}
                />
              </div>
            </div>
          </div>
          
          <div className="p-4 mt-4">
            <div className="flex justify-content-between align-items-center mb-4">
              <h3>Вопросы урока</h3>
            </div>
            
                        <TabView activeIndex={activeTab} onTabChange={(e) => setActiveTab(e.index)}>
              <TabPanel header={i18n.language === 'ru' ? 'По падежам' : 'By Case'}>
                <DataTable 
                  value={caseAggregations}
                  paginator 
                  rows={20}
                  responsiveLayout="scroll"
                >
                  <Column 
                    header={i18n.language === 'ru' ? 'Статус' : 'Status'} 
                    body={(rowData) => <WordStatusIcon status={rowData.status} />} 
                    style={{ width: '10%' }}
                    sortable
                    sortField="status"
                  />
                  <Column 
                    header={i18n.language === 'ru' ? 'Падеж' : 'Case'} 
                    body={(rowData) => (
                      <div>{i18n.language === 'ru' ? rowData.caseRu : rowData.caseEn}</div>
                    )}
                    style={{ width: '30%' }}
                    sortable
                    sortField="caseType"
                  />
                  <Column 
                    header={i18n.language === 'ru' ? 'Изучено' : 'Learned'} 
                    body={(rowData) => (
                      <div className="flex align-items-center gap-2">
                        <ProgressBar 
                          value={rowData.aggregatedProgress} 
                          style={{ height: '8px', width: '80px' }}
                          showValue={false}
                        />
                        <span 
                          className="cursor-pointer underline text-primary"
                          onClick={() => navigate(`/quiz/grammar/${slug}?filterScope=CASE_ONLY&filterCaseType=${rowData.caseType}`)}
                        >
                          {rowData.aggregatedProgress}%
                        </span>
                      </div>
                    )}
                    style={{ width: '25%' }}
                    sortable
                    sortField="aggregatedProgress"
                  />
                </DataTable>
              </TabPanel>
              <TabPanel header={i18n.language === 'ru' ? 'Подробно' : 'Details'}>
                <DataTable 
                  value={sortedForms}
                  paginator 
                  rows={20}
                  responsiveLayout="scroll"
                >
                  <Column 
                    header={i18n.language === 'ru' ? 'Статус' : 'Status'} 
                    body={(rowData) => <WordStatusIcon status={rowData.status} />} 
                    style={{ width: '8%' }}
                    sortable
                    sortField="status"
                    onSort={() => handleSort('status')}
                  />
                  <Column 
                    header={i18n.language === 'ru' ? 'Падеж' : 'Case'} 
                    body={(rowData) => (
                      <div>
                        <div>{i18n.language === 'ru' ? rowData.caseRu : rowData.caseEn}</div>
                      </div>
                    )}
                    style={{ width: '15%' }}
                    sortable
                    sortField="caseType"
                    onSort={() => handleSort('caseType')}
                  />
                  <Column 
                    header={i18n.language === 'ru' ? 'Число' : 'Number'} 
                    body={(rowData) => (i18n.language === 'ru' ? rowData.numberRu : rowData.numberEn)}
                    style={{ width: '12%' }}
                    sortable
                    sortField="numberType"
                    onSort={() => handleSort('numberType')}
                  />
                  <Column 
                    header={i18n.language === 'ru' ? 'Род' : 'Gender'} 
                    body={(rowData) => (i18n.language === 'ru' ? rowData.genderRu : rowData.genderEn)}
                    style={{ width: '12%' }}
                    sortable
                    sortField="gender"
                    onSort={() => handleSort('gender')}
                  />
                  <Column 
                    header={i18n.language === 'ru' ? 'Окончание' : 'Ending'} 
                    body={(rowData) => (
                      <span className="font-bold">{rowData.caseEnding ?? '-'}</span>
                    )}
                    style={{ width: '15%' }}
                  />
                  <Column 
                    header={i18n.language === 'ru' ? 'Изучено' : 'Learned'} 
                    body={(rowData) => (
                      <span 
                        className="cursor-pointer underline text-primary"
                        onClick={() => navigate(`/quiz/grammar/${slug}?filterScope=CASE_NUMBER_GENDER&filterCaseType=${rowData.caseType}&filterNumberType=${rowData.numberType}&filterGender=${rowData.gender}`)}
                      >
                        {rowData.score > 0 ? `${rowData.score}%` : '0%'}
                      </span>
                    )}
                    style={{ width: '13%' }}
                    sortable
                                        sortField="score"
                    onSort={() => handleSort('score')}
                  />
                </DataTable>
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
