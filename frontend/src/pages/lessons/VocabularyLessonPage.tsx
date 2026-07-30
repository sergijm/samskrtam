import React, { useState, useMemo } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useVocabularyLesson } from "../../hooks/useLessons";
import { LessonHeader } from "../../components/lesson/LessonHeader";
import { LessonStatsTab } from "../../components/lesson/LessonStatsTab";
import { WordHistoryDialog } from "../../components/lesson/WordHistoryDialog";
import { SessionsTab } from "../../components/lesson/SessionsTab";
import { MiniProgressBar } from "../../components/common/MiniProgressBar";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { TabView, TabPanel } from "primereact/tabview";
import { Skeleton } from "primereact/skeleton";

export const VocabularyLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const { t, i18n } = useTranslation();
  const { data: lesson, isLoading, isError } = useVocabularyLesson(slug || "");
  const [selectedWord, setSelectedWord] = useState<string | null>(null);
  const [wordHistoryDialogVisible, setWordHistoryDialogVisible] = useState(false);
  const [sortField, setSortField] = useState<string>("word");
  const [sortOrder, setSortOrder] = useState<number>(1);
  const [activeTab, setActiveTab] = useState<number>(0);

  const handleWordHistoryClick = (wordId: string) => {
    setSelectedWord(wordId);
    setWordHistoryDialogVisible(true);
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
        if (typeof valueA === "string" && typeof valueB === "string") {
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
        <div className="p-error">{t("common.errorLoadingLesson")}</div>
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
                  <span className="text-base">{i18n.language === "ru" ? "Всего слов" : "Total words"}</span>
                </div>
              )}
            </div>
          </div>

          {lesson.statusSummary && (
            <div className="mb-3">
              <LessonStatsTab statusSummary={lesson.statusSummary} quizPath={`/quiz/vocabulary/${slug}`} />
            </div>
          )}

          <div className="p-4 mt-4">
            <TabView activeIndex={activeTab} onTabChange={(e) => setActiveTab(e.index)}>
              <TabPanel header={i18n.language === "ru" ? "Слова" : "Words"}>
                <div className="flex justify-content-between align-items-center mb-4">
                  <h3>{i18n.language === "ru" ? "Слова урока" : "Lesson Words"}</h3>
                </div>

                <DataTable value={sortedWords} paginator rows={10} responsiveLayout="scroll">
                  <Column
                    header={i18n.language === "ru" ? "Слово" : "Word"}
                    body={(rowData) => (
                      <div>
                        <div className="font-bold">{rowData.word}</div>
                        {rowData.wordDevanagari && (
                          <div className="text-sm text-color-secondary">{rowData.wordDevanagari}</div>
                        )}
                      </div>
                    )}
                    style={{ width: "30%" }}
                    sortable
                    sortField="word"
                    onSort={(e) => handleSort("word")}
                  />
                  <Column
                    header={i18n.language === "ru" ? "Перевод" : "Translation"}
                    body={(rowData) => (
                      <div>
                        <div>{rowData.translationRu}</div>
                        <div className="text-sm text-color-secondary">{rowData.translationEn}</div>
                      </div>
                    )}
                    style={{ width: "30%" }}
                  />
                  <Column
                    header={i18n.language === "ru" ? "Изучено" : "Learned"}
                    body={(rowData) => (
                      <MiniProgressBar
                        value={rowData.score ?? 0}
                        status={rowData.status}
                        onClick={() => handleWordHistoryClick(rowData.wordId)}
                      />
                    )}
                    style={{ width: "18%" }}
                    sortable
                    sortField="score"
                    onSort={(e) => handleSort("score")}
                  />
                </DataTable>
              </TabPanel>

              <TabPanel header={i18n.language === "ru" ? "Сессии" : "Sessions"}>
                <SessionsTab quizId={lesson.lessonId} slug={slug || ""} lessonType="vocabulary" />
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
