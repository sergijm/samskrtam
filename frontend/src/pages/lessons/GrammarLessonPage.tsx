import React, { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useGrammarLesson, useDeclensionParadigm, useDeclensionExamples } from '../../hooks/useLessons';

import { LessonHeader } from '../../components/lesson/LessonHeader';
import { LessonStatsTab } from '../../components/lesson/LessonStatsTab';
import GrammarParadigmCarousel from '../../components/lesson/GrammarParadigmCarousel';
import GrammarProgressGrid from '../../components/lesson/GrammarProgressGrid';
import GrammarProgressTagSets from '../../components/lesson/GrammarProgressTagSets';
import DeclensionExamplesPanel from '../../components/lesson/DeclensionExamplesPanel';
import DeclensionEndingsReferenceTable from '../../components/lesson/DeclensionEndingsReferenceTable';
import DeclensionEndingWordsTable from '../../components/lesson/DeclensionEndingWordsTable';
import { TabView, TabPanel } from 'primereact/tabview';
import { Skeleton } from 'primereact/skeleton';
import { saveVerseBatchIds } from '../../utils/verseBatchIds';
import { useAuthStore } from '../../store/authStore';
import {
  vowelTypeToEndingsTable,
  ENDINGS_COLUMN_TO_NUMBER_GENDER,
  CASE_KEY_TO_CASE_TYPE,
} from '../../data/aStemEndingsTable';

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
  const navigate = useNavigate();
  const isAdmin = useAuthStore((s) => s.user?.roles?.includes('ADMIN') ?? false);
  const { data: lesson, isLoading, isError } = useGrammarLesson(slug || '');

  const [activeTab, setActiveTab] = useState<number>(readSavedTab);

  // Tab 0 (Paradigms) is the default active tab — fetch immediately if it's the saved tab
  const [paradigmsTabOpened, setParadigmsTabOpened] = useState(readSavedTab() === 0);
  // Tab 1 (Examples) — lazy, only when clicked
  const [examplesTabOpened, setExamplesTabOpened] = useState(readSavedTab() === 1);

  const [selectedEndingCell, setSelectedEndingCell] = useState<{
    caseType: string;
    numberType: string;
    gender?: string;
    endingText: string;
  } | null>(null);
  const [selectedEndingCellKey, setSelectedEndingCellKey] = useState<{
    caseKey: string;
    columnKey: string;
  } | null>(null);

  // Fetch first paradigm page to determine stem type for the endings reference table.
  // React Query deduplicates this with the carousel's own fetch for index 0.
  const { data: firstParadigmPage } = useDeclensionParadigm(slug || '', 0, paradigmsTabOpened);
  const endingsTableData = firstParadigmPage?.paradigm?.vowelType
    ? vowelTypeToEndingsTable[firstParadigmPage.paradigm.vowelType]
    : undefined;

  // Examples data is fetched lazily in DeclensionExamplesPanel; React Query
  // deduplicates the key, so fetching here for the tab-header icon is free.
  const { data: examplesData } = useDeclensionExamples(slug || '', examplesTabOpened);

  // Все verseId примеров урока — для иконки в заголовке таба «Примеры».
  const allExampleVerseIds = useMemo(
    () => [...new Set((examplesData?.groups ?? []).flatMap((g) => g.examples.map((e) => e.verseId)))],
    [examplesData],
  );

  const openAllExamples = () => {
    saveVerseBatchIds(allExampleVerseIds);
    navigate('/sangraha/verses');
  };

  const handleEndingCellClick = (caseKey: string, columnKey: string) => {
    const numGender = ENDINGS_COLUMN_TO_NUMBER_GENDER[columnKey];
    if (!numGender || !endingsTableData) return;
    const row = endingsTableData.rows.find(r => r.caseKey === caseKey);
    const cell = row?.cells[columnKey];
    if (!cell) return;
    setSelectedEndingCell({
      caseType: CASE_KEY_TO_CASE_TYPE[caseKey],
      numberType: numGender.numberType,
      gender: numGender.gender,
      endingText: cell.text,
    });
    setSelectedEndingCellKey({ caseKey, columnKey });
  };

  const handleTabChange = (e: { index: number }) => {
    setActiveTab(e.index);
    saveTab(e.index);
    if (e.index === 0) {
      setParadigmsTabOpened(true);
    }
    if (e.index === 1) {
      setExamplesTabOpened(true);
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

          <div className="mt-4">
            <TabView activeIndex={activeTab} onTabChange={handleTabChange} className="grammar-lesson-tabs">
              <TabPanel header={i18n.language === 'ru' ? 'Парадигмы' : 'Paradigms'}>
                {/* Static reference table of case endings — shown only when stem type is recognized */}
                {endingsTableData && (
                  <DeclensionEndingsReferenceTable
                    data={endingsTableData}
                    onCellClick={handleEndingCellClick}
                    selectedCell={selectedEndingCellKey}
                  />
                )}
                {selectedEndingCell !== null ? (
                  <DeclensionEndingWordsTable
                    selection={selectedEndingCell}
                    slug={slug || ''}
                    totalCount={firstParadigmPage?.totalCount ?? 0}
                    onBack={() => {
                      setSelectedEndingCell(null);
                      setSelectedEndingCellKey(null);
                    }}
                  />
                ) : (
                  <GrammarParadigmCarousel slug={slug || ''} enabled={paradigmsTabOpened} />
                )}
              </TabPanel>
              <TabPanel
                header={
                  <span className="flex align-items-center gap-1">
                    {i18n.language === 'ru' ? 'Примеры' : 'Examples'}
                    {isAdmin && allExampleVerseIds.length > 0 && (
                      <i
                        className="pi pi-external-link cursor-pointer"
                        title={t('examples.openAll', { count: allExampleVerseIds.length })}
                        onClick={openAllExamples}
                      />
                    )}
                  </span>
                }
                className="declension-examples-tab-panel"
              >
                <DeclensionExamplesPanel slug={slug || ''} enabled={examplesTabOpened} />
              </TabPanel>
              <TabPanel header={i18n.language === 'ru' ? 'Прогресс' : 'Progress'}>
                <GrammarProgressGrid
                  aggregations={lesson.grid ?? []}
                  caseNames={lesson.caseAggregations ?? []}
                  numberNames={lesson.numberAggregations ?? []}
                  quizSlug={slug || ''}
                />
                <GrammarProgressTagSets
                  cases={lesson.caseAggregations ?? []}
                  numbers={lesson.numberAggregations ?? []}
                  pairs={lesson.pairAggregations ?? []}
                  quizSlug={slug || ''}
                />
              </TabPanel>
            </TabView>
          </div>
        </>
      )}
    </div>
  );
};

export default GrammarLessonPage;
