import { useTranslation } from 'react-i18next';
import { DataView } from 'primereact/dataview';
import { Button } from 'primereact/button';
import { Toast } from 'primereact/toast';
import { useRef, useState } from 'react';
import { Skeleton } from 'primereact/skeleton';
import { useNavigate } from 'react-router-dom';

import { useWorks, useCreateWork, useDeleteWork } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import type { WorkSummaryDto } from '../../types/sangraha';

import WorkFormDialog from '../../components/sangraha/WorkFormDialog';
import DeleteConfirmDialog from '../../components/sangraha/DeleteConfirmDialog';
import { CreateButton, IconButton } from '../../components/common/buttons';

const WorksPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const { data: works, isLoading, isError } = useWorks();
  const createWork = useCreateWork();
  const deleteWork = useDeleteWork();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  const [dialogVisible, setDialogVisible] = useState(false);
  const [deleteSlug, setDeleteSlug] = useState<string | null>(null);
  const [form, setForm] = useState({ title: '', description: '' });

  const handleCreate = async () => {
    try {
      await createWork.mutateAsync({
        title: form.title,
        description: form.description || undefined,
      });
      setDialogVisible(false);
      setForm({ title: '', description: '' });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.addWork') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  };

  const handleDelete = async () => {
    if (!deleteSlug) return;
    try {
      await deleteWork.mutateAsync(deleteSlug);
      setDeleteSlug(null);
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.deleteWork') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  };

  const listItem = (work: WorkSummaryDto) => {
    const title = i18n.language === 'ru' ? (work.titleRu || work.titleEn) : work.titleEn;

    return (
      <div
        className="flex align-items-center gap-3 p-3 border-1 border-round-lg surface-border surface-card cursor-pointer hover:surface-hover transition-all transition-duration-200"
        onClick={() => navigate(`/sangraha/${work.slug}`)}
      >
        <div
          className="flex align-items-center justify-content-center border-circle flex-shrink-0"
          style={{ width: '3rem', height: '3rem', backgroundColor: 'var(--surface-ground)' }}
        >
          <i className="pi pi-bookmark text-xl text-primary" />
        </div>
        <div className="flex flex-column flex-1 gap-1">
          <div className="font-bold text-lg">{title}</div>
          <div className="flex align-items-center gap-2 flex-wrap">
            {work.author && (
              <span className="text-color-secondary text-sm">{work.author}</span>
            )}
            {work.author && (work.descriptionEn || work.descriptionRu) && (
              <span className="text-color-secondary">·</span>
            )}
            <span className="text-color-secondary text-sm">
              {i18n.language === 'ru' ? work.descriptionRu : work.descriptionEn}
            </span>
          </div>
        </div>
        <div className="flex align-items-center gap-2">
          {isAdmin && (
            <IconButton
              iconName="pi-trash"
              className="p-button-danger p-button-text"
              onClick={(e) => {
                e.stopPropagation();
                setDeleteSlug(work.slug);
              }}
            />
          )}
          <Button
            icon="pi pi-arrow-right"
            rounded
            text
            severity="info"
            aria-label={t('common.go')}
          />
        </div>
      </div>
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
        <h1>{t('sangraha.works')}</h1>
        {isAdmin && <CreateButton labelKey="sangraha.addWork" onClick={() => setDialogVisible(true)} />}
      </div>

      {isLoading || !works ? (
        <div className="flex flex-column gap-2 mx-auto w-full" style={{ maxWidth: '900px' }}>
          {[1, 2, 3].map((i) => (
            <Skeleton key={i} width="100%" height="72px" />
          ))}
        </div>
      ) : (
        <div className="mx-auto w-full" style={{ maxWidth: '900px' }}>
          <DataView
            value={works}
            layout="list"
            listTemplate={(items) => (
              <div className="flex flex-column gap-2">
                {items.map((work) => listItem(work))}
              </div>
            )}
            paginator={false}
            emptyMessage={
              <div className="text-center text-color-secondary p-4">{t('sangraha.noWorks')}</div>
            }
          />
        </div>
      )}

      <WorkFormDialog
        visible={dialogVisible}
        onHide={() => setDialogVisible(false)}
        form={form}
        onFormChange={setForm}
        onSave={handleCreate}
        loading={createWork.isPending}
      />

      <DeleteConfirmDialog
        visible={!!deleteSlug}
        onHide={() => setDeleteSlug(null)}
        onConfirm={handleDelete}
        loading={deleteWork.isPending}
      />
    </div>
  );
};

export default WorksPage;
