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
const GRAMMAR_SUBTAB_STORAGE_KEY = 'grammar-lesson-active-subtab';

function readSavedTab(): number {
  try {
    const raw = localStorage.getItem(GRAMMAR_TAB_STORAGE_KEY);
    if (raw !== null) {
      const parsed = parseInt(raw, 10);
      if (!isNaN(parsed) && parsed >= 0 && parsed <= 1) return parsed;
    }
  } catch { /* ignore */ }
  return 0;
}

function saveTab(index: number) {
  try {
    localStorage.setItem(GRAMMAR_TAB_STORAGE_KEY, String(index));
  } catch { /* ignore */ }
}

function readSavedSubTab(): number {
  try {
    const raw = localStorage.getItem(GRAMMAR_SUBTAB_STORAGE_KEY);
    if (raw !== null) {
      const parsed = parseInt(raw, 10);
      if (!isNaN(parsed) && parsed >= 0 && parsed <= 1) return parsed;
    }
  } catch { /* ignore */ }
  return 0;
}

function saveSubTab(index: number) {
  try {
    localStorage.setItem(GRAMMAR_SUBTAB_STORAGE_KEY, String(index));
  } catch { /* ignore */ }
}

const GrammarLessonPage = () => {
  const { slug } = useParams<{ slug: string }>();
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const isAdmin = useAuthStore((s) => s.user?.roles?.includes('ADMIN') ?? false);
  const { data: lesson, isLoading, isError } = useGrammarLesson(slug || '');

  const [activeTab, setActiveTab] = useState<number>(readSavedTab);
  const [activeSubTab, setActiveSubTab] = useState<number>(readSavedSubTab);

  const lessonTabVisible = activeTab === 0;
  const paradigmsSubTabVisible = activeTab === 0 && activeSubTab === 0;
  const examplesSubTabVisible = activeTab === 0 && activeSubTab === 1;

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

  // Filter state derived from the endings reference table (for examples tab).
  const [filterCaseKey, setFilterCaseKey] = useState<string | null>(null);
  const [filterColumnKey, setFilterColumnKey] = useState<string | null>(null);
  const filterCaseType = filterCaseKey ? CASE_KEY_TO_CASE_TYPE[filterCaseKey] : null;
  const filterNumberType = filterColumnKey ? ENDINGS_COLUMN_TO_NUMBER_GENDER[filterColumnKey]?.numberType ?? null : null;

  // Fetch first paradigm page to determine stem type for the endings reference table.
  // React Query deduplicates this with the carousel's own fetch for index 0.
  const { data: firstParadigmPage } = useDeclensionParadigm(slug || '', 0, lessonTabVisible);
  const vowelType = firstParadigmPage?.paradigm?.vowelType ?? '';
  const gender = firstParadigmPage?.paradigm?.gender ?? '';
  const endingsTableData = vowelType ? vowelTypeToEndingsTable[vowelType] : undefined;

  // Examples data is fetched lazily; the request goes directly to sangraha-service
  // with the lesson's stem class (vowelType, gender) resolved from the paradigm page.
  const { data: examplesData } = useDeclensionExamples(
    slug || '',
    vowelType,
    gender,
    examplesSubTabVisible && !!vowelType && !!gender,
  );

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
    setFilterCaseKey(caseKey);
    setFilterColumnKey(columnKey);
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

  const handleCaseClick = (caseKey: string) => {
    setFilterCaseKey(caseKey === filterCaseKey ? null : caseKey);
    setFilterColumnKey(null);
    setSelectedEndingCell(null);
    setSelectedEndingCellKey(null);
  };

  const handleNumberClick = (columnKey: string) => {
    setFilterColumnKey(columnKey === filterColumnKey ? null : columnKey);
    setFilterCaseKey(null);
    setSelectedEndingCell(null);
    setSelectedEndingCellKey(null);
  };

  const handleTabChange = (e: { index: number }) => {
    setActiveTab(e.index);
    saveTab(e.index);
  };

  const handleSubTabChange = (e: { index: number }) => {
    setActiveSubTab(e.index);
    saveSubTab(e.index);
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
              <TabPanel header={i18n.language === 'ru' ? 'Урок' : 'Lesson'}>
                {/* Static reference table of case endings — shown only when stem type is recognized */}
                {endingsTableData && (
                  <DeclensionEndingsReferenceTable
                    data={endingsTableData}
                    onCellClick={handleEndingCellClick}
                    onCaseClick={handleCaseClick}
                    onNumberClick={handleNumberClick}
                    selectedCell={selectedEndingCellKey}
                    activeFilter={{ caseKeyFilter: filterCaseKey ?? undefined, columnKeyFilter: filterColumnKey ?? undefined }}
                  />
                )}
                <TabView
                  activeIndex={activeSubTab}
                  onTabChange={handleSubTabChange}
                  className="grammar-lesson-subtabs"
                >
                  <TabPanel header={i18n.language === 'ru' ? 'Парадигмы' : 'Paradigms'}>
                    {selectedEndingCell !== null && (
                      <div className="mb-3">
                        <DeclensionEndingWordsTable
                          selection={selectedEndingCell}
                          slug={slug || ''}
                          totalCount={firstParadigmPage?.totalCount ?? 0}
                          onBack={() => {
                            setSelectedEndingCell(null);
                            setSelectedEndingCellKey(null);
                          }}
                        />
                      </div>
                    )}
                    <GrammarParadigmCarousel slug={slug || ''} enabled={paradigmsSubTabVisible} />
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
                    <DeclensionExamplesPanel
                      slug={slug || ''}
                      vowelType={vowelType}
                      gender={gender}
                      enabled={examplesSubTabVisible}
                      filterCaseType={filterCaseType}
                      filterNumberType={filterNumberType}
                    />
                  </TabPanel>
                </TabView>
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
