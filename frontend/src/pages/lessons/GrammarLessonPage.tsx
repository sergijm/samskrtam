import React, { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useGrammarLesson } from '../../hooks/useLessons';
import { useQuestionHistory } from '../../hooks/useLessons';
import { LessonHeader } from '../../components/lesson/LessonHeader';
import { WordStatusIcon } from '../../components/lesson/WordStatusIcon';
import { QuestionHistoryDialog } from '../../components/lesson/QuestionHistoryDialog';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { Tag } from 'primereact/tag';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';
import { useTranslation } from 'react-i18next';

const GrammarLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const { data: lesson, isLoading, isError } = useGrammarLesson(slug || '');
  const [selectedQuestion, setSelectedQuestion] = useState<string | null>(null);
  const [questionHistoryDialogVisible, setQuestionHistoryDialogVisible] = useState(false);
  const [sortField, setSortField] = useState<string>('caseType');
  const [sortOrder, setSortOrder] = useState<number>(1);
  
  const handleQuestionHistoryClick = (questionId: string) => {
    setSelectedQuestion(questionId);
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
          <LessonHeader 
            title={lesson.titleRu} 
            titleEn={lesson.titleEn}
            difficulty={lesson.difficulty}
            progress={lesson.progressPercent}
            total={lesson.totalQuestions}
            learned={lesson.learnedQuestions}
          />
          
          <div className="p-4 mt-4">
            <div className="flex justify-content-between align-items-center mb-4">
              <h3>Вопросы урока</h3>
              <Button 
                label="Начать квиз" 
                icon="pi pi-play"
                onClick={handleStartQuiz}
                disabled={lesson.totalQuestions === 0}
              />
            </div>
            
            <DataTable 
              value={sortedForms}
              paginator 
              rows={20}
              responsiveLayout="scroll"
            >
              <Column 
                header="Статус" 
                body={(rowData) => <WordStatusIcon status={rowData.status} />} 
                style={{ width: '8%' }}
                sortable
                sortField="status"
                onSort={() => handleSort('status')}
              />
              <Column 
                header="Падеж" 
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
                header="Число" 
                body={(rowData) => (i18n.language === 'ru' ? rowData.numberRu : rowData.numberEn)}
                style={{ width: '12%' }}
                sortable
                sortField="numberType"
                onSort={() => handleSort('numberType')}
              />
              <Column 
                header="Род" 
                body={(rowData) => (i18n.language === 'ru' ? rowData.genderRu : rowData.genderEn)}
                style={{ width: '12%' }}
                sortable
                sortField="gender"
                onSort={() => handleSort('gender')}
              />
              <Column 
                header="Форма (IAST)" 
                body={(rowData) => (
                  <div>
                    <div className="font-bold">{rowData.correctAnswerRu}</div>
                    {rowData.correctAnswerEn && rowData.correctAnswerEn !== rowData.correctAnswerRu && (
                      <div className="text-sm text-color-secondary">{rowData.correctAnswerEn}</div>
                    )}
                  </div>
                )}
                style={{ width: '25%' }}
              />
              <Column 
                header="Изучено" 
                body={(rowData) => (
                  <span 
                    className="cursor-pointer underline text-primary"
                    onClick={() => handleQuestionHistoryClick(rowData.questionId)}
                  >
                    {rowData.successRate > 0 ? `${rowData.successRate.toFixed(0)}%` : '0%'}
                  </span>
                )}
                style={{ width: '13%' }}
                sortable
                sortField="successRate"
                onSort={() => handleSort('successRate')}
              />
            </DataTable>
          </div>
          
          <QuestionHistoryDialog 
            visible={questionHistoryDialogVisible} 
            onHide={() => setQuestionHistoryDialogVisible(false)} 
            questionId={selectedQuestion} 
            lessonSlug={slug}
          />
        </>
      )}
    </div>
  );
};

export default GrammarLessonPage;
