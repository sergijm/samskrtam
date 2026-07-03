import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useWorkTree, useCreateChapter, useDeleteChapter, useCreateVerse, useDeleteVerse } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { TreeTable } from 'primereact/treetable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Skeleton } from 'primereact/skeleton';
import { useRef, useState, useMemo, useCallback } from 'react';
import type { TreeNode } from 'primereact/treenode';
import type { ChapterTreeDto, VerseTreeDto } from '../../types/sangraha';

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
  const [chapterForm, setChapterForm] = useState({ slug: '', titleRu: '', titleEn: '', orderIndex: 0 });
  const [verseForm, setVerseForm] = useState({ orderIndex: 0 });

  const treeNodes: TreeNode[] = useMemo(() => {
    if (!work?.chapters) return [];
    return work.chapters.map((ch: ChapterTreeDto) => ({
      key: `ch-${ch.id}`,
      data: {
        id: ch.id,
        type: 'chapter',
        title: `${ch.titleEn} (${ch.titleRu})`,
        categoryCode: ch.categoryCode,
        slug: ch.slug,
        orderIndex: ch.orderIndex,
      },
      children: ch.verses.map((v: VerseTreeDto) => ({
        key: `v-${v.id}`,
        data: {
          id: v.id,
          type: 'verse',
          textIastPreview: v.textIastPreview || '',
          status: v.status,
          orderIndex: v.orderIndex,
        },
      })),
    }));
  }, [work]);

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

  const titleTemplate = useCallback((rowData: TreeNode) => {
    const d = rowData?.data;
    if (d?.type === 'chapter') {
      return (
        <div className="flex align-items-center gap-2">
          <i className="pi pi-book text-primary" />
          <span className="font-bold">{d.title}</span>
        </div>
      );
    }
    return (
      <div
        className="flex align-items-center gap-2 cursor-pointer hover:text-primary"
        onClick={(e) => {
          e.stopPropagation();
          navigate(`/sangraha/${workSlug}/verses/${d.id}`);
        }}
      >
        <i className="pi pi-file text-color-secondary" />
        <span className="text-sm">{d?.textIastPreview || `Verse ${d?.orderIndex}`}</span>
        <Tag value={t(`sangraha.status.${d?.status}`)} severity={statusSeverity[d?.status] || 'info'} />
      </div>
    );
  }, [navigate, workSlug, t]);

  const quizTemplate = useCallback((rowData: TreeNode) => {
    const d = rowData?.data;
    if (d?.type !== 'verse') return null;
    return (
      <Tag severity={d.status === 'ANALYZED' ? 'success' : 'warn'}>
        {d.status === 'ANALYZED' ? t('sangraha.quizReady') : t('sangraha.quizPending')}
      </Tag>
    );
  }, [t]);

  const actionsTemplate = useCallback((rowData: TreeNode) => {
    if (!isAdmin) return null;
    const d = rowData?.data;
    return (
      <div className="flex gap-1">
        <Button
          icon="pi pi-plus"
          className="p-button-rounded p-button-text p-button-sm"
          tooltip={d?.type === 'chapter' ? t('sangraha.addVerse') : t('sangraha.addChapter')}
          onClick={(e) => {
            e.stopPropagation();
            if (d?.type === 'chapter') {
              setSelectedChapterId(d.id);
              setVerseDialog(true);
            } else {
              setChapterDialog(true);
            }
          }}
        />
        <Button
          icon="pi pi-trash"
          className="p-button-rounded p-button-text p-button-sm p-button-danger"
          tooltip={t('common.delete')}
          onClick={(e) => {
            e.stopPropagation();
            if (d?.type === 'chapter') {
              deleteChapter.mutate(d.id);
            } else {
              deleteVerse.mutate(d.id);
            }
          }}
        />
      </div>
    );
  }, [isAdmin, deleteChapter, deleteVerse, t]);

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

      <TreeTable value={treeNodes} className="mt-3">
        <Column field="title" header={t('sangraha.chapter')} body={titleTemplate} expander style={{ width: '60%' }} />
        <Column header={t('sangraha.quiz')} body={quizTemplate} style={{ width: '20%' }} />
        {isAdmin && (
          <Column header={t('common.actions')} body={actionsTemplate} style={{ width: '20%' }} />
        )}
      </TreeTable>

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