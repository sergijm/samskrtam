import React, { useState } from 'react';
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

const GrammarLessonPage = () => {
  const { type } = useParams<{ type: string }>();
  const navigate = useNavigate();
  const { data: lesson, isLoading, isError } = useGrammarLesson(type || '');
  const [selectedQuestion, setSelectedQuestion] = useState<string | null>(null);
  const [questionHistoryDialogVisible, setQuestionHistoryDialogVisible] = useState(false);
  
  const handleQuestionHistoryClick = (questionId: string) => {
    setSelectedQuestion(questionId);
    setQuestionHistoryDialogVisible(true);
  };
  
  const handleStartQuiz = () => {
    if (lesson) {
      navigate(`/quiz/grammar/${type}`);
    }
  };
  
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
              value={lesson.questions}
              paginator 
              rows={10}
              responsiveLayout="scroll"
            >
              <Column 
                header="Статус" 
                body={(rowData) => <WordStatusIcon status={rowData.status} />} 
                style={{ width: '10%' }}
              />
              <Column 
                header="Вопрос" 
                body={(rowData) => (
                  <div>
                    <div>{rowData.textRu}</div>
                    <div className="text-sm text-color-secondary">{rowData.textEn}</div>
                  </div>
                )} 
                style={{ width: '40%' }}
              />
              <Column 
                header="Правильный ответ" 
                body={(rowData) => (
                  <div>
                    <div>{rowData.correctAnswerRu}</div>
                    <div className="text-sm text-color-secondary">{rowData.correctAnswerEn}</div>
                  </div>
                )} 
                style={{ width: '30%' }}
              />
              <Column 
                header="Попытки" 
                body={(rowData) => (
                  <span 
                    className="cursor-pointer underline text-primary"
                    onClick={() => handleQuestionHistoryClick(rowData.questionId)}
                  >
                    {rowData.nSuccess}/{rowData.nAll}
                  </span>
                )} 
                style={{ width: '20%' }}
              />
            </DataTable>
          </div>
          
          <QuestionHistoryDialog 
            visible={questionHistoryDialogVisible} 
            onHide={() => setQuestionHistoryDialogVisible(false)} 
            questionId={selectedQuestion} 
            quizId={lesson.quizId}
          />
        </>
      )}
    </div>
  );
};

export default GrammarLessonPage;

