import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useVerseDetail,
} from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { sangrahaApi } from '../../api/sangraha';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { InputTextarea } from 'primereact/inputtextarea';
import { useRef, useState, useCallback, useEffect, useMemo } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import VerseWordsList from '../../components/sangraha/VerseWordsList';
import SandhiSplitsList from '../../components/sangraha/SandhiSplitsList';
import './VersePage.css';
import { IconButton, CtaButton } from '../../components/common/buttons';

const VersePage = () => {
  const { t, i18n } = useTranslation();
  const { workSlug, verseId } = useParams<{ workSlug: string; verseId: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const queryClient = useQueryClient();
  const { data: verse, isLoading, isError } = useVerseDetail(verseId || '');
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  // Study icon
  const studyIcon = useMemo(() => {
    return 'pi-book';
  }, []);

  const [editText, setEditText] = useState('');
  const [analyzePending, setAnalyzePending] = useState(false);
  const [analyzeMorphologyPending, setAnalyzeMorphologyPending] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

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
      setIsEditing(false);
    }
  }, [verseId, editText, queryClient, t]);

  const handleStudy = useCallback(async () => {
    if (!verseId) return;
    try {
      const res = await sangrahaApi.studyVerse(verseId);
      const { verseTopicCode: code } = res.data;
      navigate(`/lessons/vocabulary/${code}`);
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [verseId, navigate, t]);

  const handleAnalyzeMorphology = useCallback(async () => {
    if (!verseId) return;
    setAnalyzeMorphologyPending(true);
    try {
      await sangrahaApi.analyzeVerseInternalSandhi(verseId);
      queryClient.invalidateQueries({ queryKey: ['sangraha', 'verse', verseId] });
      toast.current?.show({ severity: 'success', summary: t('sangraha.action.analyzeMorphology') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    } finally {
      setAnalyzeMorphologyPending(false);
    }
  }, [verseId, queryClient, t]);

  const isAnalyzed = verse?.status === 'ANALYZED';
  const isAnalyzing = verse?.status === 'ANALYZING';
  const isDraftOrFailed = verse?.status === 'DRAFT' || verse?.status === 'FAILED';

  const statusSeverity = verse?.status === 'ANALYZED' ? 'success' : verse?.status === 'FAILED' ? 'danger' : 'warning';

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
        {isAnalyzed && isAdmin && (
          <div className="flex align-items-center gap-2 ml-auto">
            <CtaButton
              labelKey="sangraha.action.analyze"
              iconName="pi-robot"
              className="p-button-text"
              onClick={handleAnalyze}
              loading={analyzePending}
            />
            <CtaButton
              labelKey="common.edit"
              iconName="pi-pencil"
              className="p-button-text"
              onClick={() => setIsEditing(true)}
            />
          </div>
        )}
      </div>

      {isAnalyzing && (
        <div className="mb-4">
          <Skeleton width="100%" height="50px" />
          <p className="mt-2 text-color-secondary">{t('sangraha.status.ANALYZING')}</p>
        </div>
      )}

      {/* DRAFT/FAILED or editing: input + Analyze button */}
      {(isDraftOrFailed || isEditing) && !isAnalyzing && (
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
            <div className="flex align-items-center gap-2">
              <CtaButton
                labelKey="sangraha.action.analyze"
                iconName="pi-robot"
                className="p-button-success"
                onClick={handleAnalyze}
                loading={analyzePending}
              />
              {isEditing && (
                <CtaButton
                  labelKey="common.cancel"
                  iconName="pi-times"
                  className="p-button-text"
                  onClick={() => setIsEditing(false)}
                />
              )}
            </div>
          )}
        </div>
      )}

      {/* ANALYZED: read-only view */}
      {isAnalyzed && (
        <>
          <div className="mb-4">
            <label className="block mb-1 font-semibold">{t('sangraha.fields.text')}</label>
            <div className="p-3 border-1 border-round surface-border surface-ground">
              <p className="m-0 verse-devanagari">{verse.textDevanagari || '-'}</p>
              <p className="m-0 mt-1 text-lg text-color-secondary">{verse.textIast || '-'}</p>
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
              headerActions={
                <>
                  <CtaButton
                    labelKey="sangraha.action.analyzeMorphology"
                    iconName="pi-sitemap"
                    className="p-button-text"
                    onClick={handleAnalyzeMorphology}
                    loading={analyzeMorphologyPending}
                  />
                  <CtaButton
                    labelKey="sangraha.action.study"
                    iconName={studyIcon}
                    className="p-button-text"
                    onClick={handleStudy}
                  />
                </>
              }
            />
          )}
        </>
      )}
    </div>
  );
};

export default VersePage;
