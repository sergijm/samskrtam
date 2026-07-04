import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useWorkTree, useCreateChapter, useDeleteChapter, useCreateVerse, useDeleteVerse } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { Button } from 'primereact/button';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { useRef, useState, useCallback } from 'react';

import ChapterTreeBrowser from '../../components/sangraha/ChapterTreeBrowser';
import ChapterDialog from '../../components/sangraha/ChapterDialog';
import VerseDialog from '../../components/sangraha/VerseDialog';
import './WorkPage.css';

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
  const [verseOrderIndex, setVerseOrderIndex] = useState(0);
  const toggleChapter = useCallback((chapterId: string) => {
    setExpandedChapters(prev => {
      const next = new Set(prev);
      if (next.has(chapterId)) next.delete(chapterId);
      else next.add(chapterId);
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
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [workSlug, chapterForm, createChapter, t]);

  const handleAddVerse = useCallback(async () => {
    if (!selectedChapterId) return;
    try {
      await createVerse.mutateAsync({ chapterId: selectedChapterId, data: { orderIndex: verseOrderIndex } });
      setVerseDialog(false);
      setSelectedChapterId(null);
      setVerseOrderIndex(0);
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.verseAdded') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [selectedChapterId, verseOrderIndex, createVerse, t]);

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
            onClick={() => setChapterDialog(true)}
          />
        )}
      </div>

      <ChapterTreeBrowser
        chapters={work.chapters || []}
        workSlug={workSlug || ''}
        isAdmin={isAdmin}
        expandedChapters={expandedChapters}
        onToggleChapter={toggleChapter}
        onAddVerse={(chapterId) => { setSelectedChapterId(chapterId); setVerseDialog(true); }}
        onDeleteChapter={(chapterId) => deleteChapter.mutate(chapterId)}
        onDeleteVerse={(verseId) => deleteVerse.mutate(verseId)}
      />

      <ChapterDialog
        visible={chapterDialog}
        onHide={() => setChapterDialog(false)}
        form={chapterForm}
        onFormChange={setChapterForm}
        onSave={handleAddChapter}
        loading={createChapter.isPending}
      />

      <VerseDialog
        visible={verseDialog}
        onHide={() => { setVerseDialog(false); setSelectedChapterId(null); }}
        orderIndex={verseOrderIndex}
        onOrderIndexChange={setVerseOrderIndex}
        onSave={handleAddVerse}
        loading={createVerse.isPending}
      />
    </div>
  );
};

export default WorkPage;