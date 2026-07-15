import React, { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useVocabularyLesson } from '../../hooks/useLessons';
import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsBadges } from '../../components/lesson/LessonStatsBadges';
import { WordHistoryDialog } from '../../components/lesson/WordHistoryDialog';
import { SessionsTab } from '../../components/lesson/SessionsTab';
import { DataTable, DataTableSelectionMultipleChangeEvent } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { TabView, TabPanel } from 'primereact/tabview';
import { Button } from 'primereact/button';
import { Skeleton } from 'primereact/skeleton';
import type { VocabularyWordProgress } from '../../types/lesson';

export const VocabularyLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const navigate = useNavigate();
  const { t, i18n } = useTranslation();
  const { data: lesson, isLoading, isError } = useVocabularyLesson(slug || '');
  const [selectedWord, setSelectedWord] = useState<string | null>(null);
  const [wordHistoryDialogVisible, setWordHistoryDialogVisible] = useState(false);
  const [sortField, setSortField] = useState<string>('word');
  const [sortOrder, setSortOrder] = useState<number>(1);
  const [activeTab, setActiveTab] = useState<number>(0);
  const [selectedWords, setSelectedWords] = useState<VocabularyWordProgress[]>([]);

  const getQuizCount = (): number => {
    const words = lesson?.words || [];
    return selectedWords.length > 0 ? selectedWords.length : words.length;
  };
  
  const handleWordHistoryClick = (wordId: string) => {
    setSelectedWord(wordId);
    setWordHistoryDialogVisible(true);
  };
  
    const handleStartQuiz = () => {
    if (lesson) {
      navigate(`/quiz/vocabulary/${slug}`);
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

  const sortedWords = useMemo(() => {
    if (!lesson?.words) return [];
    const words = [...lesson.words];
    if (sortField) {
      words.sort((a, b) => {
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
    return words;
  }, [lesson?.words, sortField, sortOrder]);
  
  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t('common.errorLoadingLesson')}</div>
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
                  quizPath={`/quiz/vocabulary/${slug}`}
                />
                {activeTab !== 1 && (
                  <Button
                    label={`${t('common.startQuiz')} (${getQuizCount()})`}
                    icon="pi pi-play"
                    onClick={handleStartQuiz}
                    disabled={lesson.totalWords === 0}
                  />
                )}
              </div>
            </div>
          </div>

                    <div className="p-4 mt-4">
            <TabView activeIndex={activeTab} onTabChange={(e) => setActiveTab(e.index)}>
              <TabPanel header={i18n.language === 'ru' ? 'Слова' : 'Words'}>
            <div className="flex justify-content-between align-items-center mb-4">
              <h3>{i18n.language === 'ru' ? 'Слова урока' : 'Lesson Words'}</h3>
            </div>

                        <DataTable
              value={sortedWords}
              paginator
              rows={10}
              responsiveLayout="scroll"
              selectionMode="multiple"
              selection={selectedWords}
              onSelectionChange={(e: DataTableSelectionMultipleChangeEvent<VocabularyWordProgress[]>) =>
                setSelectedWords(e.value as VocabularyWordProgress[])
              }
              dataKey="wordId"
            >
              <Column selectionMode="multiple" headerStyle={{ width: '3rem' }} />
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
                    {rowData.score > 0 ? `${rowData.score}%` : '0%'}
                  </span>
                )}
                style={{ width: '15%' }}
                sortable
                sortField="score"
                onSort={(e) => handleSort('score')}
              />
            </DataTable>
          </TabPanel>

              <TabPanel header={i18n.language === 'ru' ? 'Сессии' : 'Sessions'}>
                <SessionsTab
                  quizId={lesson.lessonId}
                  slug={slug || ''}
                  lessonType="vocabulary"
                />
              </TabPanel>
            </TabView>
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

