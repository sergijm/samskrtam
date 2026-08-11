import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useVerseDetail,
  useGetOrCreateVocabularyQuiz,
} from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { useLocaleStore } from '../../store/localeStore';
import { quizApi } from '../../api/quizApi';
import { sangrahaApi } from '../../api/sangraha';
import { LessonType } from '../../types/quiz';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { InputTextarea } from 'primereact/inputtextarea';
import { useRef, useState, useCallback, useEffect, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useVocabularyLesson } from '../../hooks/useLessons';
import type { VocabularyWordProgress } from '../../types/lesson';
import VerseWordsList from '../../components/sangraha/VerseWordsList';
import SandhiSplitsList from '../../components/sangraha/SandhiSplitsList';
import { IconButton, CtaButton } from '../../components/common/buttons';

const VersePage = () => {
  const { t, i18n } = useTranslation();
  const { workSlug, verseId } = useParams<{ workSlug: string; verseId: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const queryClient = useQueryClient();
  const { data: verse, isLoading, isError } = useVerseDetail(verseId || '');
  const getOrCreateVocabularyQuiz = useGetOrCreateVocabularyQuiz();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  // Load lesson progress if quizSlug already exists
  const vocabSlug = verse?.vocabularyQuizSlug;
  const { data: vocabularyLesson } = useVocabularyLesson(vocabSlug || '');

  // Build map vocabularyWordId -> progress
  const wordProgressMap = useMemo<Record<string, VocabularyWordProgress> | null>(() => {
    if (!vocabularyLesson?.words) return null;
    const map: Record<string, VocabularyWordProgress> = {};
    for (const wp of vocabularyLesson.words) {
      map[wp.wordId] = wp;
    }
    return map;
  }, [vocabularyLesson]);

  // Study icon based on statusSummary
  const studyIcon = useMemo(() => {
    if (!vocabularyLesson?.statusSummary) return 'pi-book';
    const { total, mastered, learning, reviewDue } = vocabularyLesson.statusSummary;
    if (mastered === total) return 'pi-check-circle';
    if (learning > 0 || reviewDue > 0) return 'pi-caret-right';
    return 'pi-book';
  }, [vocabularyLesson]);

  const [editText, setEditText] = useState('');
  const [analyzePending, setAnalyzePending] = useState(false);

  useEffect(() => {
    if (verse) {
      setEditText(verse.rawText ?? verse.textDevanagari ?? verse.textIast ?? '');
    }
  }, [verse]);

  const handleAnalyze = useCallback(async () => {
    if (!verseId) return;
    setAnalyzePending(true);
    try {
      await sangrahaApi.analyzeVerse(verseId, { text: editText });
      queryClient.invalidateQueries({ queryKey: ['sangraha', 'verse', verseId] });
      queryClient.invalidateQueries({ queryKey: ['sangraha', 'work'] });
      toast.current?.show({ severity: 'success', summary: t('sangraha.action.analyze') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    } finally {
      setAnalyzePending(false);
    }
  }, [verseId, editText, queryClient, t]);

  const handleStudy = useCallback(async () => {
    if (!verseId) return;
    try {
      const quizRes = await getOrCreateVocabularyQuiz.mutateAsync(verseId);
      const { quizSlug, quizId } = quizRes.data;

      queryClient.invalidateQueries({ queryKey: ['lesson', 'vocabulary', quizSlug] });

      const locale = useLocaleStore.getState().locale;
      const sessionRes = await quizApi.startOrResumeWithStatusFilter(
        quizId,
        LessonType.VOCABULARY,
        locale,
        'NEW',
      );
      const sessionData = sessionRes.data;

      navigate(`/quiz/vocabulary/${quizSlug}/${sessionData.sessionId}`, {
        state: { sessionData },
      });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [verseId, getOrCreateVocabularyQuiz, navigate, t, queryClient]);

  const isAnalyzed = verse?.status === 'ANALYZED';
  const isAnalyzing = verse?.status === 'ANALYZING';
  const isDraftOrFailed = verse?.status === 'DRAFT' || verse?.status === 'FAILED';

  const statusSeverity = verse?.status === 'ANALYZED' ? 'success' : verse?.status === 'FAILED' ? 'danger' : 'warn';

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t('common.error')}</div>
      </div>
    );
  }

  if (isLoading || !verse) {
    return (
      <div className="p-4">
        <Skeleton width="100%" height="30px" className="mb-3" />
        <Skeleton width="100%" height="200px" className="mb-3" />
        <Skeleton width="100%" height="300px" />
      </div>
    );
  }

  return (
    <div className="p-4">
      <Toast ref={toast} />
      <div className="flex align-items-center mb-3">
                <IconButton
          iconName="pi-arrow-left"
          className="p-button-rounded mr-2"
          onClick={() => {
            const chapterId = verse.chapterId;
            if (chapterId) {
              navigate(`/sangraha/${workSlug}/chapters/${chapterId}`);
            } else {
              navigate(`/sangraha/${workSlug}`);
            }
          }}
        />
        <h2 className="m-0">{t('sangraha.verse')} #{verse.orderIndex}</h2>
        <Tag value={t(`sangraha.status.${verse.status}`)} severity={statusSeverity} className="ml-2" />
      </div>

      {isAnalyzing && (
        <div className="mb-4">
          <Skeleton width="100%" height="50px" />
          <p className="mt-2 text-color-secondary">{t('sangraha.status.ANALYZING')}</p>
        </div>
      )}

      {/* DRAFT/FAILED: input + Analyze button */}
      {isDraftOrFailed && !isAnalyzing && (
        <div className="mb-4">
          <div className="mb-3">
            <label className="block mb-1 font-semibold">{t('sangraha.fields.text')}</label>
            <InputTextarea
              value={editText}
              onChange={(e) => setEditText(e.target.value)}
              className="w-full"
              rows={4}
              placeholder={t('sangraha.placeholder.text')}
            />
          </div>
          {isAdmin && (
            <CtaButton
              labelKey="sangraha.action.analyze"
              iconName="pi-robot"
              className="p-button-success"
              onClick={handleAnalyze}
              loading={analyzePending}
            />
          )}
        </div>
      )}

      {/* ANALYZED: read-only view */}
      {isAnalyzed && (
        <>
          <div className="mb-4">
            <div className="mb-3">
              <label className="block mb-1 font-semibold">{t('sangraha.fields.textDevanagari')}</label>
              <div className="p-3 border-1 border-round surface-border surface-ground">
                <p className="m-0 text-lg">{verse.textDevanagari || '-'}</p>
              </div>
            </div>
            <div className="mb-3">
              <label className="block mb-1 font-semibold">{t('sangraha.fields.textIast')}</label>
              <div className="p-3 border-1 border-round surface-border surface-ground">
                <p className="m-0 text-lg">{verse.textIast || '-'}</p>
              </div>
            </div>
          </div>

          {verse.analysis && (
            <div className="mb-4">
              <div className="mb-3">
                <label className="block mb-1 font-semibold">{t('sangraha.fields.translation')}</label>
                <div className="p-3 border-1 border-round surface-border surface-ground">
                  <p className="m-0">{(i18n.language === 'ru' ? verse.analysis.translationRu : verse.analysis.translationEn) || '-'}</p>
                </div>
              </div>
              <SandhiSplitsList sandhiSplits={verse.analysis.sandhiSplits} />
            </div>
          )}

          {verse.words && verse.words.length > 0 && (
            <VerseWordsList
              words={verse.words}
              wordProgressMap={wordProgressMap}
              headerActions={
                <CtaButton
                  labelKey="sangraha.action.study"
                  iconName={studyIcon}
                  className="p-button-text"
                  onClick={handleStudy}
                  loading={getOrCreateVocabularyQuiz.isPending}
                />
              }
            />
          )}
        </>
      )}
    </div>
  );
};

export default VersePage;
