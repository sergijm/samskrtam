import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useWorkTree, useCreateChapter, useDeleteChapter, useCreateVerse, useDeleteVerse } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { useRef, useState, useMemo, useCallback } from 'react';
import type { ChapterTreeDto, VerseTreeDto } from '../../types/sangraha';
import './WorkPage.css';

const statusSeverity: Record<string, 'success' | 'info' | 'warn' | 'danger'> = {
  ANALYZED: 'success',
  ANALYZING: 'info',
  DRAFT: 'warn',
  FAILED: 'danger',
};

const WorkPage = () => {
  const { t } = useTranslation();
  const { workSlug } = useParams<{ workSlug: string }>();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const { data: work, isLoading, isError } = useWorkTree(workSlug || '');
  const createChapter = useCreateChapter();
  const deleteChapter = useDeleteChapter();
  const createVerse = useCreateVerse();
  const deleteVerse = useDeleteVerse();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  const [chapterDialog, setChapterDialog] = useState(false);
  const [verseDialog, setVerseDialog] = useState(false);
  const [selectedChapterId, setSelectedChapterId] = useState<string | null>(null);
  const [expandedChapters, setExpandedChapters] = useState<Set<string>>(new Set());
  const [chapterForm, setChapterForm] = useState({ slug: '', titleRu: '', titleEn: '', orderIndex: 0 });
  const [verseForm, setVerseForm] = useState({ orderIndex: 0 });

  const toggleChapter = useCallback((chapterId: string) => {
    setExpandedChapters(prev => {
      const next = new Set(prev);
      if (next.has(chapterId)) {
        next.delete(chapterId);
      } else {
        next.add(chapterId);
      }
      return next;
    });
  }, []);

  const handleAddChapter = useCallback(async () => {
    if (!workSlug) return;
    try {
      await createChapter.mutateAsync({ workSlug, data: chapterForm });
      setChapterDialog(false);
      setChapterForm({ slug: '', titleRu: '', titleEn: '', orderIndex: 0 });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.chapterAdded') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error'), detail: t('common.error') });
    }
  }, [workSlug, chapterForm, createChapter, t]);

  const handleAddVerse = useCallback(async () => {
    if (!selectedChapterId) return;
    try {
      await createVerse.mutateAsync({ chapterId: selectedChapterId, data: verseForm });
      setVerseDialog(false);
      setSelectedChapterId(null);
      setVerseForm({ orderIndex: 0 });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.verseAdded') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error'), detail: t('common.error') });
    }
  }, [selectedChapterId, verseForm, createVerse, t]);

  const handleDeleteChapter = useCallback((chapterId: string) => {
    deleteChapter.mutate(chapterId);
  }, [deleteChapter]);

  const handleDeleteVerse = useCallback((verseId: string) => {
    deleteVerse.mutate(verseId);
  }, [deleteVerse]);

  const handleChapterDialog = useCallback(() => {
    setChapterDialog(true);
  }, []);

  const handleVerseDialog = useCallback((chapterId: string) => {
    setSelectedChapterId(chapterId);
    setVerseDialog(true);
  }, []);

  const isExpanded = useCallback((chapterId: string) => expandedChapters.has(chapterId), [expandedChapters]);

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

  if (isError || !work) {
    return (
      <div className="p-4 text-center">
        <i className="pi pi-exclamation-triangle text-4xl text-red-500 mb-3" />
        <h3>{t('common.error')}</h3>
        <p>{t('sangraha.workNotFound')}</p>
        <Button label={t('common.back')} icon="pi pi-arrow-left" onClick={() => navigate('/sangraha')} />
      </div>
    );
  }

  return (
    <div className="p-4">
      <Toast ref={toast} />

      <div className="flex align-items-center justify-content-between mb-3">
        <div className="flex align-items-center gap-3">
          <Button icon="pi pi-arrow-left" className="p-button-rounded p-button-text" onClick={() => navigate('/sangraha')} />
          <h1 className="text-2xl font-bold m-0">{work.titleEn || work.titleRu}</h1>
        </div>
        {isAdmin && (
          <Button
            label={t('sangraha.addChapter')}
            icon="pi pi-plus"
            onClick={handleChapterDialog}
          />
        )}
      </div>

      <div className="work-tree">
        {work.chapters?.map((ch: ChapterTreeDto) => (
          <div key={ch.id} className="work-tree-chapter">
            <div className="work-tree-row" onClick={() => toggleChapter(ch.id)}>
              <div className="work-tree-row-left">
                <i className={`pi ${isExpanded(ch.id) ? 'pi-chevron-down' : 'pi-chevron-right'} text-sm`} />
                <i className="pi pi-book text-primary" />
                <span className="font-bold">{ch.titleEn} ({ch.titleRu})</span>
              </div>
              {isAdmin && (
                <div className="work-tree-row-right">
                  <Button
                    icon="pi pi-plus"
                    className="p-button-rounded p-button-text p-button-sm"
                    tooltip={t('sangraha.addVerse')}
                    onClick={(e) => { e.stopPropagation(); handleVerseDialog(ch.id); }}
                  />
                  <Button
                    icon="pi pi-trash"
                    className="p-button-rounded p-button-text p-button-sm p-button-danger"
                    tooltip={t('common.delete')}
                    onClick={(e) => { e.stopPropagation(); handleDeleteChapter(ch.id); }}
                  />
                </div>
              )}
            </div>
            {isExpanded(ch.id) && ch.verses?.map((v: VerseTreeDto) => (
              <div
                key={v.id}
                className="work-tree-row work-tree-verse"
                onClick={() => navigate(`/sangraha/${workSlug}/verses/${v.id}`)}
              >
                <div className="work-tree-row-left">
                  <i className="pi pi-file text-color-secondary" />
                  <span className="text-sm">{v.textIastPreview || `Verse ${v.orderIndex}`}</span>
                  <Tag value={t(`sangraha.status.${v.status}`)} severity={statusSeverity[v.status] || 'info'} />
                </div>
                {isAdmin && (
                  <div className="work-tree-row-right">
                    <Button
                      icon="pi pi-trash"
                      className="p-button-rounded p-button-text p-button-sm p-button-danger"
                      tooltip={t('common.delete')}
                      onClick={(e) => { e.stopPropagation(); handleDeleteVerse(v.id); }}
                    />
                  </div>
                )}
              </div>
            ))}
          </div>
        ))}
      </div>

      <Dialog
        header={t('sangraha.addChapter')}
        visible={chapterDialog}
        onHide={() => setChapterDialog(false)}
        style={{ width: '400px' }}
        footer={
          <div>
            <Button label={t('common.cancel')} icon="pi pi-times" className="p-button-text" onClick={() => setChapterDialog(false)} />
            <Button label={t('common.save')} icon="pi pi-check" onClick={handleAddChapter} loading={createChapter.isPending} />
          </div>
        }
      >
        <div className="flex flex-column gap-3">
          <div>
            <label htmlFor="ch-slug">{t('sangraha.slug')}</label>
            <InputText id="ch-slug" value={chapterForm.slug} onChange={(e) => setChapterForm({ ...chapterForm, slug: e.target.value })} className="w-full" />
          </div>
          <div>
            <label htmlFor="ch-titleRu">{t('sangraha.titleRu')}</label>
            <InputText id="ch-titleRu" value={chapterForm.titleRu} onChange={(e) => setChapterForm({ ...chapterForm, titleRu: e.target.value })} className="w-full" />
          </div>
          <div>
            <label htmlFor="ch-titleEn">{t('sangraha.titleEn')}</label>
            <InputText id="ch-titleEn" value={chapterForm.titleEn} onChange={(e) => setChapterForm({ ...chapterForm, titleEn: e.target.value })} className="w-full" />
          </div>
          <div>
            <label htmlFor="ch-order">{t('sangraha.orderIndex')}</label>
            <InputNumber id="ch-order" value={chapterForm.orderIndex} onValueChange={(e) => setChapterForm({ ...chapterForm, orderIndex: e.value ?? 0 })} className="w-full" />
          </div>
        </div>
      </Dialog>

      <Dialog
        header={t('sangraha.addVerse')}
        visible={verseDialog}
        onHide={() => { setVerseDialog(false); setSelectedChapterId(null); }}
        style={{ width: '400px' }}
        footer={
          <div>
            <Button label={t('common.cancel')} icon="pi pi-times" className="p-button-text" onClick={() => { setVerseDialog(false); setSelectedChapterId(null); }} />
            <Button label={t('common.save')} icon="pi pi-check" onClick={handleAddVerse} loading={createVerse.isPending} />
          </div>
        }
      >
        <div className="flex flex-column gap-3">
          <div>
            <label htmlFor="v-order">{t('sangraha.orderIndex')}</label>
            <InputNumber id="v-order" value={verseForm.orderIndex} onValueChange={(e) => setVerseForm({ orderIndex: e.value ?? 0 })} className="w-full" />
          </div>
        </div>
      </Dialog>
    </div>
  );
};

export default WorkPage;