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
import { InputTextarea } from 'primereact/inputtextarea';
import { useRef, useState, useCallback, useEffect } from 'react';

const VersePage = () => {
  const { t, i18n } = useTranslation();
  const { workSlug, verseId } = useParams<{ workSlug: string; verseId: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const { data: verse, isLoading, isError } = useVerseDetail(verseId || '');
  const updateText = useUpdateVerseText();
  const analyze = useAnalyzeVerse();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  const [isEditing, setIsEditing] = useState(false);
  const [editText, setEditText] = useState('');

  useEffect(() => {
    if (verse) {
      setEditText(verse.textDevanagari || verse.textIast || '');
      if (verse.status === 'DRAFT' || verse.status === 'FAILED') {
        setIsEditing(true);
      }
    }
  }, [verse]);

  const handleSaveText = useCallback(async () => {
    if (!verseId) return;
    try {
      await updateText.mutateAsync({ verseId, data: { textDevanagari: editText, textIast: editText } });
      toast.current?.show({ severity: 'success', summary: t('common.saved') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [verseId, editText, updateText, t]);

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

  const startEditing = useCallback(() => {
    if (verse) {
      setEditText(verse.textDevanagari || verse.textIast || '');
    }
    setIsEditing(true);
  }, [verse]);

  const isAnalyzed = verse?.status === 'ANALYZED';
  const isAnalyzing = verse?.status === 'ANALYZING';

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
        <Button
          icon="pi pi-arrow-left"
          className="p-button-text p-button-rounded mr-2"
          onClick={() => navigate(`/sangraha/${workSlug}`)}
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

      {/* Режим просмотра — два отдельных поля */}
      {!isEditing && !isAnalyzing && (
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
      )}

      {/* Режим редактирования — одно поле (деванагари, но можно ввести IAST) */}
      {isEditing && !isAnalyzing && (
        <div className="mb-4">
          <label className="block mb-1 font-semibold">{t('sangraha.fields.text')}</label>
          <InputTextarea
            value={editText}
            onChange={(e) => setEditText(e.target.value)}
            className="w-full"
            rows={4}
            placeholder={t('sangraha.placeholder.text')}
          />
        </div>
      )}

      {!isAnalyzing && isEditing && isAdmin && (
        <div className="flex gap-2 mb-4">
          <Button label={t('sangraha.action.save')} icon="pi pi-save" onClick={handleSaveText} loading={updateText.isPending} />
          <Button label={t('sangraha.action.analyze')} icon="pi pi-robot" className="p-button-success" onClick={handleAnalyze} loading={analyze.isPending} />
        </div>
      )}

      {isAnalyzed && !isEditing && (
        <>
          {verse.analysis && (
            <div className="mb-4">
                            <div className="mb-3">
                <label className="block mb-1 font-semibold">{t('sangraha.fields.translation')}</label>
                <div className="p-3 border-1 border-round surface-border surface-ground">
                  <p className="m-0">{(i18n.language === 'ru' ? verse.analysis.translationRu : verse.analysis.translationEn) || '-'}</p>
                </div>
              </div>
              {verse.analysis.sandhiSplits && verse.analysis.sandhiSplits.length > 0 && (
                <div className="mb-3">
                  <label className="block mb-1 font-semibold">{t('sangraha.fields.sandhiSplits')}</label>
                  <div className="p-3 border-1 border-round surface-border surface-ground">
                    {verse.analysis.sandhiSplits.map((s, i) => (
                      <div key={i} className="mb-2">
                        <span className="font-medium">{s.surface}</span>
                        <span className="mx-2">→</span>
                        <span>{s.components.join(' + ')}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {verse.words && verse.words.length > 0 && (
            <div className="mb-4">
              <label className="block mb-1 font-semibold">{t('sangraha.fields.words')}</label>
              <div className="p-3 border-1 border-round surface-border surface-ground">
                {verse.words.map((w) => (
                  <div key={w.id} className="flex align-items-center gap-2 mb-1">
                    <span className="font-medium">{w.surfaceIast}</span>
                    <span className="text-color-secondary">({w.pos || '-'})</span>
                    {w.stem && <span className="text-sm">stem: {w.stem}</span>}
                    {w.glossRu && <span className="text-sm">— {w.glossRu}</span>}
                  </div>
                ))}
              </div>
            </div>
          )}

          {isAdmin && (
            <Button label={t('sangraha.action.edit')} icon="pi pi-pencil" className="p-button-outlined" onClick={startEditing} />
          )}
        </>
      )}
    </div>
  );
};

export default VersePage;