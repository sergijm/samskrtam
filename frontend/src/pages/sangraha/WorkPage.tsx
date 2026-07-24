import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useWorkTree, useCreateChapter, useDeleteChapter, useCreateVerse, useDeleteVerse, useUpdateWork } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { useRef, useState, useCallback } from 'react';

import ChapterTreeBrowser from '../../components/sangraha/ChapterTreeBrowser';
import ChapterDialog from '../../components/sangraha/ChapterDialog';
import WorkEditDialog from '../../components/sangraha/WorkEditDialog';
import { IconButton, CreateButton, PageButton } from '../../components/common/buttons';
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
  const updateWork = useUpdateWork();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

    const [chapterDialog, setChapterDialog] = useState(false);
  const [expandedChapters, setExpandedChapters] = useState<Set<string>>(new Set());
  const [chapterForm, setChapterForm] = useState({ title: '', orderIndex: null as number | null });
  const [editDialogVisible, setEditDialogVisible] = useState(false);
  const [editForm, setEditForm] = useState({ titleRu: '', titleEn: '', descriptionRu: '', descriptionEn: '', author: '' });

  const openEditDialog = useCallback(() => {
    if (!work) return;
    setEditForm({
      titleRu: work.titleRu || '',
      titleEn: work.titleEn || '',
      descriptionRu: work.descriptionRu || '',
      descriptionEn: work.descriptionEn || '',
      author: work.author || '',
    });
    setEditDialogVisible(true);
  }, [work]);

  const handleEditSave = useCallback(async () => {
    if (!workSlug) return;
    try {
      await updateWork.mutateAsync({ workSlug, data: editForm });
      setEditDialogVisible(false);
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.editWork') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [workSlug, editForm, updateWork, t]);

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
      await createChapter.mutateAsync({ workSlug, data: { title: chapterForm.title, orderIndex: chapterForm.orderIndex ?? undefined } });
            setChapterDialog(false);
            setChapterForm({ title: '', orderIndex: null });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.chapterAdded') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [workSlug, chapterForm, createChapter, t]);

        const handleAddVerse = useCallback(async (chapterId: string) => {
    if (!workSlug) return;
    try {
      const verse = await createVerse.mutateAsync({
        chapterId,
        data: { orderIndex: 0 },
      });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.verseAdded') });
      navigate(`/sangraha/${workSlug}/verses/${verse.data.id}`);
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [workSlug, createVerse, navigate, t]);

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
        <PageButton variant="navigation" labelKey="common.back" onClick={() => navigate('/sangraha')} />
      </div>
    );
  }

  return (
    <div className="p-4">
      <Toast ref={toast} />

      <div className="flex align-items-center justify-content-between mb-3">
        <div className="flex align-items-center gap-3">
          <IconButton iconName="pi-arrow-left" className="p-button-rounded" onClick={() => navigate('/sangraha')} />
          <h1 className="text-2xl font-bold m-0">{work.titleEn || work.titleRu}</h1>
        </div>
                {isAdmin && (
                    <div className="flex gap-2">
            <CreateButton labelKey="common.edit" iconName="pi-pencil" className="p-button-outlined" onClick={openEditDialog} />
            <CreateButton labelKey="sangraha.addChapter" onClick={() => setChapterDialog(true)} />
          </div>
        )}
      </div>

      <ChapterTreeBrowser
        chapters={work.chapters || []}
        workSlug={workSlug || ''}
        isAdmin={isAdmin}
        expandedChapters={expandedChapters}
        onToggleChapter={toggleChapter}
        onAddVerse={handleAddVerse}
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

      <WorkEditDialog
        visible={editDialogVisible}
        onHide={() => setEditDialogVisible(false)}
        form={editForm}
        onFormChange={setEditForm}
        onSave={handleEditSave}
        loading={updateWork.isPending}
      />
    </div>
  );
};

export default WorkPage;