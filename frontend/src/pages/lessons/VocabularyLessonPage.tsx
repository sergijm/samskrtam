import { useState } from "react";
import { useParams } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useVocabularyLesson } from "../../hooks/useLessons";
import { LessonHeader } from "../../components/lesson/LessonHeader";
import { LessonStatsTab } from "../../components/lesson/LessonStatsTab";
import WordLemmaExamplesPanel from "../../components/lesson/WordLemmaExamplesPanel";
import { ProgressBar } from "primereact/progressbar";
import { DataTable } from "primereact/datatable";
import { Column } from "primereact/column";
import { Skeleton } from "primereact/skeleton";
import { statusToProgressColor } from "../../utils/statusColor";
import type { VocabularyWordProgress } from "../../types/lesson";

const WordProgressCell = ({ row }: { row: VocabularyWordProgress }) => {
  const color = statusToProgressColor(row.status);
  return (
    <ProgressBar
      value={row.score ?? 0}
      color={color}
      style={{ height: "6px", width: "60px" }}
      showValue={false}
    />
  );
};

const WordCell = ({ row }: { row: VocabularyWordProgress }) => (
  <div>
    <div className="font-bold">{row.word}</div>
    {row.wordDevanagari && (
      <div className="text-sm text-color-secondary">{row.wordDevanagari}</div>
    )}
  </div>
);

const TranslationCell = ({ row }: { row: VocabularyWordProgress }) => (
  <div>
    <div>{row.translationRu}</div>
    <div className="text-sm text-color-secondary">{row.translationEn}</div>
  </div>
);

export const VocabularyLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const { t, i18n } = useTranslation();
  const { data: lesson, isLoading, isError } = useVocabularyLesson(slug || "");
  const [expandedRows, setExpandedRows] = useState<VocabularyWordProgress[]>([]);
  const isRu = i18n.language === "ru";

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t("common.errorLoadingLesson")}</div>
      </div>
    );
  }

  if (isLoading || !lesson) {
    return (
      <div className="p-4">
        <Skeleton width="100%" height="40px" className="mb-2" />
        <Skeleton width="100%" height="20px" className="mb-2" />
        <Skeleton width="100%" height="20px" className="mb-4" />
        <Skeleton width="100%" height="200px" />
      </div>
    );
  }

  return (
    <div className="p-4">
      <div className="card mb-3">
        <div className="flex align-items-center justify-content-between">
          <LessonHeader title={lesson.titleRu} titleEn={lesson.titleEn} />
          {lesson.statusSummary && (
            <div className="flex align-items-center gap-1">
              <span className="text-2xl font-bold">{lesson.statusSummary.total}</span>
              <span className="text-base">{isRu ? "Всего слов" : "Total words"}</span>
            </div>
          )}
        </div>
      </div>

      {lesson.statusSummary && (
        <div className="mb-3">
          <LessonStatsTab statusSummary={lesson.statusSummary} quizPath={`/quiz/vocabulary/${slug}`} />
        </div>
      )}

      <div className="card mt-3">
        <DataTable
          value={lesson.words}
          paginator
          rows={15}
          responsiveLayout="scroll"
          sortField="status"
          sortOrder={-1}
          dataKey="wordId"
          expandedRows={expandedRows}
          onRowToggle={(e) => setExpandedRows(e.data as VocabularyWordProgress[])}
          rowExpansionTemplate={(rowData) => {
            const row = rowData as VocabularyWordProgress;
            return (
              <WordLemmaExamplesPanel
                lemma={row.word}
                enabled
              />
            );
          }}
          rowClassName={() =>
            `cursor-pointer hover:surface-hover transition-colors transition-duration-150`
          }
          emptyMessage={
            <div className="text-center p-4 text-color-secondary">
              {isRu ? "Нет слов в уроке" : "No words in this lesson"}
            </div>
          }
        >
          <Column expander style={{ width: "3em" }} />
          <Column
            field="word"
            header={isRu ? "Слово" : "Word"}
            body={(rowData) => <WordCell row={rowData} />}
            sortable
            style={{ width: "25%" }}
          />
          <Column
            field="translationRu"
            header={isRu ? "Перевод" : "Translation"}
            body={(rowData) => <TranslationCell row={rowData} />}
            sortable
          />
          <Column
            field="score"
            header={isRu ? "Прогресс" : "Progress"}
            body={(rowData) => <WordProgressCell row={rowData} />}
            sortable
            style={{ width: "90px" }}
          />
        </DataTable>
      </div>
    </div>
  );
};

export default VocabularyLessonPage;