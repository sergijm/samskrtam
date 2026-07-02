import { useTranslation } from 'react-i18next';
import { useParams, useNavigate } from 'react-router-dom';
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
import { useRef, useState, useMemo } from 'react';
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
    if (!work) return [];
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

  const handleAddChapter = async () => {
    if (!work) return;
    try {
      await createChapter.mutateAsync({ workId: work.id, data: chapterForm });
      setChapterDialog(false);
      setChapterForm({ slug: '', titleRu: '', titleEn: '', orderIndex: 0 });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.addChapter') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error'), detail: t('common.error') });
    }
  };

  const handleAddVerse = async () => {
    if (!selectedChapterId) return;
    try {
      await createVerse.mutateAsync({ chapterId: selectedChapterId, data: verseForm });
      setVerseDialog(false);
      setSelectedChapterId(null);
      setVerseForm({ orderIndex: 0 });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.addVerse') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error'), detail: t('common.error') });
    }
  };

  const titleTemplate = (rowData: any) => {
    if (rowData.type === 'chapter') {
      return (
        <div className="flex align-items-center gap-2">
          <i className="pi pi-book text-primary" />
          <span className="font-bold">{rowData.title}</span>
        </div>
      );
    }
    return (
      <div
        className="flex align-items-center gap-2 cursor-pointer hover:text-primary"
        onClick={(e) => {
          e.stopPropagation();
          navigate(`/sangraha/${workSlug}/verses/${rowData.id}`);
        }}
      >
        <i className="pi pi-file text-color-secondary" />
        <span className="text-sm">{rowData.textIastPreview || `Verse ${rowData.orderIndex}`}</span>
        <Tag value={t(`sangraha.status.${rowData.status}`)} severity={statusSeverity[rowData.status] || 'info'} />
      </div>
    );
  };

  const quizTemplate = (rowData: any) => {
    if (rowData.type !== 'chapter') return null;
    return (
      <Button
        icon="pi pi-book"
        tooltip={t('sangraha.vocabularyQuiz')}
        tooltipOptions={{ position: 'left' }}
        className="p-button-text p-button-rounded"
        onClick={(e) => {
          e.stopPropagation();
          navigate(`/quizzes/vocabulary/${rowData.categoryCode}`);
        }}
      />
    );
  };

  const actionsTemplate = (rowData: any) => {
    if (!isAdmin) return null;
    if (rowData.type === 'chapter') {
      return (
        <div className="flex gap-1">
          <Button
            icon="pi pi-plus"
            tooltip={t('sangraha.addVerse')}
            className="p-button-text p-button-rounded"
            onClick={(e) => {
              e.stopPropagation();
              setSelectedChapterId(rowData.id);
              setVerseDialog(true);
            }}
          />
          <Button
            icon="pi pi-trash"
            className="p-button-text p-button-rounded p-button-danger"
            onClick={(e) => {
              e.stopPropagation();
              deleteChapter.mutate(rowData.id);
            }}
          />
        </div>
      );
    }
    return (
      <Button
        icon="pi pi-trash"
        className="p-button-text p-button-rounded p-button-danger"
        onClick={(e) => {
          e.stopPropagation();
          deleteVerse.mutate(rowData.id);
        }}
      />
    );
  };

  if (isError) {
    return (
      <div className="p-4">
        <div className="p-error">{t('common.error')}</div>
      </div>
    );
  }

  return (
    <div className="p-4">
      <Toast ref={toast} />
      <div className="flex justify-content-between align-items-center mb-4">
        <div>
          <Button
            icon="pi pi-arrow-left"
            className="p-button-text p-button-rounded mr-2"
            onClick={() => navigate('/sangraha')}
          />
          <h1 className="inline">{work?.titleEn || ''}</h1>
          {work?.titleRu && <span className="text-color-secondary ml-2">({work.titleRu})</span>}
        </div>
        {isAdmin && (
          <Button
            label={t('sangraha.addChapter')}
            icon="pi pi-plus"
            onClick={() => setChapterDialog(true)}
          />
        )}
      </div>

      {isLoading || !work ? (
        <Skeleton width="100%" height="400px" />
      ) : (
        <TreeTable value={treeNodes} responsiveLayout="scroll">
          <Column
            header={t('sangraha.fields.versePreview')}
            body={titleTemplate}
            expander
            style={{ width: '60%' }}
          />
          <Column
            header={t('sangraha.vocabularyQuiz')}
            body={quizTemplate}
            style={{ width: '20%' }}
          />
          {isAdmin && (
            <Column
              header={t('common.actions')}
              body={actionsTemplate}
              style={{ width: '20%' }}
            />
          )}
        </TreeTable>
      )}

      {/* Add Chapter Dialog */}
      <Dialog
        header={t('sangraha.addChapter')}
        visible={chapterDialog}
        onHide={() => setChapterDialog(false)}
        style={{ width: '450px' }}
      >
        <div className="flex flex-column gap-3">
          <div>
            <label>{t('sangraha.fields.slug')}</label>
            <InputText
              value={chapterForm.slug}
              onChange={(e) => setChapterForm((f) => ({ ...f, slug: e.target.value }))}
              className="w-full"
            />
          </div>
          <div>
            <label>{t('sangraha.fields.titleRu')}</label>
            <InputText
              value={chapterForm.titleRu}
              onChange={(e) => setChapterForm((f) => ({ ...f, titleRu: e.target.value }))}
              className="w-full"
            />
          </div>
          <div>
            <label>{t('sangraha.fields.titleEn')}</label>
            <InputText
              value={chapterForm.titleEn}
              onChange={(e) => setChapterForm((f) => ({ ...f, titleEn: e.target.value }))}
              className="w-full"
            />
          </div>
          <div>
            <label>{t('sangraha.fields.orderIndex')}</label>
            <InputNumber
              value={chapterForm.orderIndex}
              onValueChange={(e) => setChapterForm((f) => ({ ...f, orderIndex: e.value || 0 }))}
              className="w-full"
            />
          </div>
          <Button
            label={t('common.create')}
            onClick={handleAddChapter}
            loading={createChapter.isPending}
          />
        </div>
      </Dialog>

      {/* Add Verse Dialog */}
      <Dialog
        header={t('sangraha.addVerse')}
        visible={verseDialog}
        onHide={() => { setVerseDialog(false); setSelectedChapterId(null); }}
        style={{ width: '350px' }}
      >
        <div className="flex flex-column gap-3">
          <div>
            <label>{t('sangraha.fields.orderIndex')}</label>
            <InputNumber
              value={verseForm.orderIndex}
              onValueChange={(e) => setVerseForm((f) => ({ ...f, orderIndex: e.value || 0 }))}
              className="w-full"
            />
          </div>
          <Button
            label={t('common.create')}
            onClick={handleAddVerse}
            loading={createVerse.isPending}
          />
        </div>
      </Dialog>
    </div>
  );
};

export default WorkPage;
