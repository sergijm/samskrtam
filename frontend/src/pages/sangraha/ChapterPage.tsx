import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useState, useCallback, useRef } from 'react';
import { InputTextarea } from 'primereact/inputtextarea';
import { Toast } from 'primereact/toast';
import { useChapterVerses, useAnalyzeAllVerses, useCreateVerse } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { verseStatusIcon } from '../../utils/verseStatus';
import { Skeleton } from 'primereact/skeleton';
import { IconButton, PageButton, SubmitButton } from '../../components/common/buttons';
import { Tooltip } from 'primereact/tooltip';
import type { VerseTreeDto } from '../../types/sangraha';
import './WorkPage.css';

const ChapterPage = () => {
  const { t, i18n } = useTranslation();
  const { workSlug, chapterId } = useParams<{ workSlug: string; chapterId: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const isAdmin = useAuthStore((s) => s.user?.roles?.includes('ADMIN') ?? false);

  const { data: chapter, isLoading, isError } = useChapterVerses(chapterId || '');
  const analyzeAll = useAnalyzeAllVerses();
  const createVerse = useCreateVerse();

  const [draftText, setDraftText] = useState('');
  const [addingVerse, setAddingVerse] = useState(false);

  const hasAnalyzableVerses = chapter?.verses?.some(
    (v) => v.status === 'DRAFT' || v.status === 'FAILED'
  );

  const handleAddVerse = useCallback(() => {
    setAddingVerse(true);
  }, []);

  const handleSaveVerse = useCallback(() => {
    if (!chapterId || !draftText.trim()) {
      toast.current?.show({ severity: 'warn', summary: t('sangraha.fields.text') + ' — ' + t('common.required') });
      return;
    }
    createVerse.mutate(
      { chapterId, text: draftText },
      {
        onSuccess: () => {
          setDraftText('');
          setAddingVerse(false);
          toast.current?.show({ severity: 'success', summary: t('common.saved') });
        },
        onError: () => toast.current?.show({ severity: 'error', summary: t('common.error') }),
      },
    );
  }, [chapterId, draftText, createVerse, t]);

  if (isLoading) {
    return (
      <div className="p-4">
        <Skeleton width="60%" height="2rem" className="mb-2" />
        <Skeleton width="100%" height="1.5rem" className="mb-1" />
        <Skeleton width="100%" height="1.5rem" className="mb-1" />
        <Skeleton width="100%" height="1.5rem" />
      </div>
    );
  }

  if (isError || !chapter) {
    return (
      <div className="p-4 text-center">
        <i className="pi pi-exclamation-triangle text-4xl text-red-500 mb-3" />
        <h3>{t('common.error')}</h3>
        <p>{t('sangraha.chapterNotFound')}</p>
        <IconButton iconName="pi-arrow-left" className="p-button-rounded" onClick={() => navigate(`/sangraha/${workSlug}`)} />
      </div>
    );
  }

  const translationFor = (translationRu?: string | null, translationEn?: string | null) =>
    i18n.language === 'ru' ? translationRu : translationEn;

  return (
    <div className="p-4">
      <Toast ref={toast} />
      <Tooltip />

      <div className="flex align-items-center mb-3">
        <IconButton
          iconName="pi-arrow-left"
          className="p-button-rounded mr-2"
          onClick={() => navigate(`/sangraha/${workSlug}`)}
        />
        <div style={{ flex: 1 }}>
          <h2 className="m-0">
            {chapter.titleIast || chapter.titleEn}
            {chapter.titleDevanagari ? ` (${chapter.titleDevanagari})` : ''}
          </h2>
          <p className="text-color-secondary text-sm m-0">
            {i18n.language === 'ru' ? chapter.titleRu : chapter.titleEn}
          </p>
        </div>
        <PageButton
          variant="cta-primary"
          iconName="pi-sync"
          labelKey="sangraha.action.analyzeAll"
          loading={analyzeAll.isPending}
          disabled={!hasAnalyzableVerses || analyzeAll.isPending}
          onClick={() => analyzeAll.mutate(chapterId!)}
        />
      </div>

      {/* Список стихов + добавление */}
      {chapter.verses && chapter.verses.length > 0 && (
        <div className="work-tree mb-3">
          {chapter.verses.map((v: VerseTreeDto) => (
            <div
              key={v.id}
              className="work-tree-row cursor-pointer hover:surface-hover"
              onClick={() => navigate(`/sangraha/${workSlug}/verses/${v.id}`)}
            >
              <div className="work-tree-row-left">
                <span className="font-bold text-sm" style={{ minWidth: '2rem' }}>{v.orderIndex}</span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', flex: 1 }}>
                  <span className="font-medium">
                    {v.textIast || `${t('sangraha.verse')} ${v.orderIndex}`}
                  </span>
                  {translationFor(v.translationRu, v.translationEn) && (
                    <span className="text-xs text-color-secondary font-italic">
                      {translationFor(v.translationRu, v.translationEn)}
                    </span>
                  )}
                </div>
              </div>
              <div className="work-tree-row-right">
                <i
                  className={verseStatusIcon[v.status]?.icon ?? 'pi pi-question-circle'}
                  style={{ color: verseStatusIcon[v.status]?.color ?? 'var(--text-color-secondary)' }}
                  data-pr-tooltip={t(`sangraha.status.${v.status}`)}
                  data-pr-position="top"
                />
                <i className="pi pi-chevron-right text-color-secondary ml-2" />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Черновик нового стиха */}
      {addingVerse && (
        <div className="work-tree mb-3">
          <div className="work-tree-row" style={{ alignItems: 'flex-start' }}>
            <div className="work-tree-row-left" style={{ flex: 1 }}>
              <InputTextarea
                value={draftText}
                onChange={(e) => setDraftText(e.target.value)}
                rows={3}
                autoResize
                placeholder={t('sangraha.placeholder.verseText')}
                className="w-full"
              />
            </div>
            <div className="work-tree-row-right">
              <SubmitButton
                labelKey="common.save"
                loading={createVerse.isPending}
                onClick={handleSaveVerse}
              />
              <IconButton
                iconName="pi-times"
                onClick={() => {
                  setAddingVerse(false);
                  setDraftText('');
                }}
              />
            </div>
          </div>
        </div>
      )}

      <div className="flex align-items-center justify-content-between mt-3">
        <span className="text-color-secondary text-sm">
          {chapter.verses && chapter.verses.length > 0
            ? t('sangraha.versesCount', { count: chapter.verses.length })
            : t('sangraha.noVerses')}
        </span>
        {isAdmin && !addingVerse && (
          <PageButton
            variant="page-action"
            labelKey="sangraha.action.addVerse"
            onClick={handleAddVerse}
          />
        )}
      </div>
    </div>
  );
};

export default ChapterPage;
