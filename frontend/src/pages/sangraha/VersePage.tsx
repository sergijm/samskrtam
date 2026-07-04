import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
import {
  useVerseDetail,
  useUpdateVerseText,
  useAnalyzeVerse,
} from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { Button } from 'primereact/button';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { useRef, useState, useCallback, useEffect } from 'react';

import VerseEditor from '../../components/sangraha/VerseEditor';
import VerseAnalysisPanel from '../../components/sangraha/VerseAnalysisPanel';
const VersePage = () => {
  const { t } = useTranslation();
  const { workSlug, verseId } = useParams<{ workSlug: string; verseId: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const { data: verse, isLoading, isError } = useVerseDetail(verseId || '');
  const updateText = useUpdateVerseText();
  const analyze = useAnalyzeVerse();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  const [isEditing, setIsEditing] = useState(false);
  const [editDevanagari, setEditDevanagari] = useState('');
  const [editIast, setEditIast] = useState('');

  useEffect(() => {
    if (verse) {
      setEditDevanagari(verse.textDevanagari || '');
      setEditIast(verse.textIast || '');
      if (verse.status === 'DRAFT' || verse.status === 'FAILED') {
        setIsEditing(true);
      }
    }
  }, [verse]);

  const handleSaveText = useCallback(async () => {
    if (!verseId) return;
    try {
      await updateText.mutateAsync({ verseId, data: { textDevanagari: editDevanagari, textIast: editIast } });
      toast.current?.show({ severity: 'success', summary: t('common.saved') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [verseId, editDevanagari, editIast, updateText, t]);

  const handleAnalyze = useCallback(async () => {
    if (!verseId) return;
    try {
      await analyze.mutateAsync(verseId);
      setIsEditing(false);
      toast.current?.show({ severity: 'success', summary: t('sangraha.action.analyze') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [verseId, analyze, t]);

  const isDraft = verse?.status === 'DRAFT' || verse?.status === 'FAILED';
  const isAnalyzed = verse?.status === 'ANALYZED';
  const isAnalyzing = verse?.status === 'ANALYZING';

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
        <Button
          icon="pi pi-arrow-left"
          className="p-button-text p-button-rounded mr-2"
          onClick={() => navigate(`/sangraha/${workSlug}`)}
        />
        <h2 className="m-0">{t('sangraha.verse')} #{verse.orderIndex}</h2>
        <Tag
          value={t(`sangraha.status.${verse.status}`)}
          severity={verse.status === 'ANALYZED' ? 'success' : verse.status === 'FAILED' ? 'danger' : 'warn'}
          className="ml-2"
        />
      </div>

      {(isEditing || isDraft) && (
        <VerseEditor
          editDevanagari={editDevanagari}
          editIast={editIast}
          onDevanagariChange={setEditDevanagari}
          onIastChange={setEditIast}
          isAdmin={isAdmin}
          isAnalyzing={isAnalyzing}
          onSave={handleSaveText}
          onAnalyze={handleAnalyze}
          savePending={updateText.isPending}
          analyzePending={analyze.isPending}
        />
      )}
      {isAnalyzed && verse.analysis && (
        <VerseAnalysisPanel
          textDevanagari={verse.textDevanagari}
          textIast={verse.textIast}
          analysis={verse.analysis}
          words={verse.words}
          isAdmin={isAdmin}
          onEdit={() => {
                  setEditDevanagari(verse.textDevanagari || '');
                  setEditIast(verse.textIast || '');
                  setIsEditing(true);
                }}
              />
          )}
        </div>
  );
};

export default VersePage;

