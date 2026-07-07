import React, { useState, useMemo } from 'react';
import { useParams } from 'react-router-dom';
import { useVocabularyLesson } from '../../hooks/useLessons';
import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatusSummary } from '../../components/lesson/LessonStatusSummary';
import { WordStatusIcon } from '../../components/lesson/WordStatusIcon';
import { WordHistoryDialog } from '../../components/lesson/WordHistoryDialog';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { ProgressBar } from 'primereact/progressbar';
import { Tag } from 'primereact/tag';
import { useNavigate } from 'react-router-dom';
import { Skeleton } from 'primereact/skeleton';

export const VocabularyLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { data: lesson, isLoading, isError } = useVocabularyLesson(slug || '');
  const [selectedWord, setSelectedWord] = useState<string | null>(null);
  const [wordHistoryDialogVisible, setWordHistoryDialogVisible] = useState(false);
  const [sortField, setSortField] = useState<string>('word');
  const [sortOrder, setSortOrder] = useState<number>(1);
  
  const handleWordHistoryClick = (wordId: string) => {
    setSelectedWord(wordId);
    setWordHistoryDialogVisible(true);
  };
  
    const handleStartQuiz = () => {
    if (lesson) {
      navigate(`/quiz/vocabulary/${slug}`);
    }
  };

  const handleReviewQuiz = () => {
    if (lesson) {
      // Уточнить у Агента 2 параметр запуска — используем filterScope=REVIEW_DUE
      navigate(`/quiz/vocabulary/${slug}?filterScope=REVIEW_DUE`);
    }
  };
  
  // Функция для обработки сортировки
  const handleSort = (field: string) => {
    if (sortField === field) {
      // Переключение направления сортировки
      setSortOrder(sortOrder === 1 ? -1 : 1);
    } else {
      // Установка нового поля сортировки и направления
      setSortField(field);
      setSortOrder(1);
    }
  };

  // Сортировка данных перед отображением
  const sortedWords = useMemo(() => {
    if (!lesson?.words) return [];
    
    const words = [...lesson.words];
    
    if (sortField) {
      words.sort((a, b) => {
        let valueA: any = a[sortField as keyof typeof a];
        let valueB: any = b[sortField as keyof typeof b];
        
        // Для строк используем localeCompare для правильной сортировки
        if (typeof valueA === 'string' && typeof valueB === 'string') {
          return sortOrder * valueA.localeCompare(valueB);
        }
        
        // Для чисел и других типов
        if (valueA < valueB) return sortOrder * -1;
        if (valueA > valueB) return sortOrder * 1;
        return 0;
      });
    }
    
    return words;
  }, [lesson?.words, sortField, sortOrder]);
  
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
            total={lesson.totalWords}
            learned={lesson.learnedWords}
          />

          <div className="mt-1 mb-3">
            <LessonStatusSummary statusSummary={lesson.statusSummary} total={lesson.totalWords} learned={lesson.learnedWords} />
          </div>
          
          <div className="p-4 mt-4">
            <div className="flex justify-content-between align-items-center mb-4">
              <h3>Слова урока</h3>
              <div className="flex gap-2">
                {lesson.statusSummary && lesson.statusSummary.reviewDue > 0 && (
                  <Button 
                    label="Повторить"
                    icon="pi pi-refresh"
                    className="p-button-outlined p-button-warning"
                    onClick={handleReviewQuiz}
                  />
                )}
                <Button 
                  label="Начать квиз" 
                  icon="pi pi-play"
                  onClick={handleStartQuiz}
                  disabled={lesson.totalWords === 0}
                />
              </div>
            </div>
            
            <DataTable 
              value={sortedWords}
              paginator 
              rows={10}
              responsiveLayout="scroll"
            >
              <Column 
                header="Статус" 
                body={(rowData) => <WordStatusIcon status={rowData.status} />} 
                style={{ width: '10%' }}
                sortable
                sortField="status"
                onSort={(e) => handleSort('status')}
              />
              <Column 
                header="Слово" 
                body={(rowData) => (
                  <div>
                    <div className="font-bold">{rowData.word}</div>
                    {rowData.wordDevanagari && (
                      <div className="text-sm text-color-secondary">{rowData.wordDevanagari}</div>
                    )}
                  </div>
                )} 
                style={{ width: '30%' }}
                sortable
                sortField="word"
                onSort={(e) => handleSort('word')}
              />
              <Column 
                header="Перевод" 
                body={(rowData) => (
                  <div>
                    <div>{rowData.translationRu}</div>
                    <div className="text-sm text-color-secondary">{rowData.translationEn}</div>
                  </div>
                )} 
                style={{ width: '30%' }}
              />

              <Column 
                header="Изучено" 
                body={(rowData) => (
                  <span 
                    className="cursor-pointer underline text-primary"
                    onClick={() => handleWordHistoryClick(rowData.wordId)}
                  >
                    {rowData.successRate > 0 ? `${rowData.successRate.toFixed(0)}%` : '0%'}
                  </span>
                )}
                style={{ width: '15%' }}
                sortable
                sortField="successRate"
                onSort={(e) => handleSort('successRate')}
              />
            </DataTable>
          </div>
          
          <WordHistoryDialog 
            visible={wordHistoryDialogVisible} 
            onHide={() => setWordHistoryDialogVisible(false)} 
            wordId={selectedWord} 
            slug={lesson.slug}
          />
        </>
      )}
    </div>
  );
};

export default VocabularyLessonPage;
