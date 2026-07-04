import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { Toast } from 'primereact/toast';
import { useRef, useState } from 'react';
import { Skeleton } from 'primereact/skeleton';

import { useWorks, useCreateWork, useDeleteWork } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';

import WorkCard from '../../components/sangraha/WorkCard';
import WorkFormDialog from '../../components/sangraha/WorkFormDialog';
import DeleteConfirmDialog from '../../components/sangraha/DeleteConfirmDialog';
const WorksPage = () => {
  const { t } = useTranslation();
  const toast = useRef<Toast>(null);
  const { data: works, isLoading, isError } = useWorks();
  const createWork = useCreateWork();
  const deleteWork = useDeleteWork();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  const [dialogVisible, setDialogVisible] = useState(false);
  const [deleteSlug, setDeleteSlug] = useState<string | null>(null);
  const [form, setForm] = useState({ slug: '', titleRu: '', titleEn: '', descriptionRu: '', descriptionEn: '', author: '' });

  const handleCreate = async () => {
    try {
      await createWork.mutateAsync(form);
      setDialogVisible(false);
      setForm({ slug: '', titleRu: '', titleEn: '', descriptionRu: '', descriptionEn: '', author: '' });
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
        <Button label={t('sangraha.addWork')} icon="pi pi-plus" onClick={() => setDialogVisible(true)} />
      </div>

      {isLoading || !works ? (
        <div className="grid">
          {[1, 2, 3].map((i) => (
            <div key={i} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2">
              <Skeleton width="100%" height="200px" />
            </div>
          ))}
        </div>
      ) : works.length === 0 ? (
        <div className="text-center text-color-secondary p-4">{t('sangraha.noWorks')}</div>
      ) : (
        <div className="grid">
          {works.map((work) => (
            <WorkCard
              key={work.id}
              id={work.id}
              slug={work.slug}
              titleEn={work.titleEn}
              titleRu={work.titleRu}
              author={work.author}
              descriptionEn={work.descriptionEn}
              isAdmin={isAdmin}
              onDelete={setDeleteSlug}
            />
          ))}
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

