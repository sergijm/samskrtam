import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { Button } from 'primereact/button';
import { useNavigate } from 'react-router-dom';
import { useWorks, useCreateWork, useDeleteWork } from '../../hooks/useSangraha';
import { useAuthStore } from '../../store/authStore';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Toast } from 'primereact/toast';
import { useRef, useState } from 'react';
import { Skeleton } from 'primereact/skeleton';
import { ConfirmDialog } from 'primereact/confirmdialog';

const WorksPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const { data: works, isLoading, isError } = useWorks();
  const createWork = useCreateWork();
  const deleteWork = useDeleteWork();
  const user = useAuthStore((s) => s.user);
  const isAdmin = user?.roles?.includes('ADMIN') ?? false;

  const [dialogVisible, setDialogVisible] = useState(false);
  const [deleteWorkId, setDeleteWorkId] = useState<string | null>(null);
  const [form, setForm] = useState({ slug: '', titleRu: '', titleEn: '', descriptionRu: '', descriptionEn: '', author: '' });

  const handleCreate = async () => {
    try {
      await createWork.mutateAsync(form);
      setDialogVisible(false);
      setForm({ slug: '', titleRu: '', titleEn: '', descriptionRu: '', descriptionEn: '', author: '' });
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.addWork') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error'), detail: t('common.error') });
    }
  };

  const handleDelete = async () => {
    if (!deleteWorkId) return;
    try {
      await deleteWork.mutateAsync(deleteWorkId);
      setDeleteWorkId(null);
      toast.current?.show({ severity: 'success', summary: t('common.success'), detail: t('sangraha.deleteWork') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error'), detail: t('common.error') });
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
      <ConfirmDialog />
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
            <div key={work.id} className="col-12 sm:col-6 md:col-4 lg:col-3 p-2 flex">
              <div
                onClick={() => navigate(`/sangraha/${work.slug}`)}
                className="p-card p-component lesson-card flex flex-column text-center h-full cursor-pointer hover:shadow-8 transition-all transition-duration-200 w-full"
              >
                <div className="p-card-body">
                  <div className="p-card-title">{work.titleEn}</div>
                  <div className="p-card-subtitle">{work.titleRu}</div>
                  {work.author && <div className="mt-2 text-sm text-color-secondary">{work.author}</div>}
                  {work.descriptionEn && (
                    <div className="mt-2 text-sm text-color-secondary">{work.descriptionEn}</div>
                  )}
                </div>
                {isAdmin && (
                  <div className="p-card-footer flex justify-content-end">
                    <Button
                      icon="pi pi-trash"
                      className="p-button-danger p-button-text"
                      onClick={(e) => {
                        e.stopPropagation();
                        setDeleteWorkId(work.slug);
                      }}
                    />
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}

      <Dialog
        header={t('sangraha.addWork')}
        visible={dialogVisible}
        onHide={() => setDialogVisible(false)}
        style={{ width: '500px' }}
      >
        <div className="flex flex-column gap-3">
          <div>
            <label>{t('sangraha.fields.slug')}</label>
            <InputText
              value={form.slug}
              onChange={(e) => setForm((f) => ({ ...f, slug: e.target.value }))}
              className="w-full"
            />
          </div>
          <div>
            <label>{t('sangraha.fields.titleRu')}</label>
            <InputText
              value={form.titleRu}
              onChange={(e) => setForm((f) => ({ ...f, titleRu: e.target.value }))}
              className="w-full"
            />
          </div>
          <div>
            <label>{t('sangraha.fields.titleEn')}</label>
            <InputText
              value={form.titleEn}
              onChange={(e) => setForm((f) => ({ ...f, titleEn: e.target.value }))}
              className="w-full"
            />
          </div>
          <div>
            <label>{t('sangraha.fields.descriptionRu')}</label>
            <InputTextarea
              value={form.descriptionRu}
              onChange={(e) => setForm((f) => ({ ...f, descriptionRu: e.target.value }))}
              className="w-full"
              rows={3}
            />
          </div>
          <div>
            <label>{t('sangraha.fields.descriptionEn')}</label>
            <InputTextarea
              value={form.descriptionEn}
              onChange={(e) => setForm((f) => ({ ...f, descriptionEn: e.target.value }))}
              className="w-full"
              rows={3}
            />
          </div>
          <div>
            <label>{t('sangraha.fields.author')}</label>
            <InputText
              value={form.author}
              onChange={(e) => setForm((f) => ({ ...f, author: e.target.value }))}
              className="w-full"
            />
          </div>
          <Button
            label={t('common.create')}
            onClick={handleCreate}
            loading={createWork.isPending}
          />
        </div>
      </Dialog>

      <Dialog
        header={t('sangraha.deleteWork')}
        visible={!!deleteWorkId}
        onHide={() => setDeleteWorkId(null)}
        style={{ width: '400px' }}
      >
        <p>{t('common.confirm')}</p>
        <div className="flex justify-content-end gap-2 mt-3">
          <Button label={t('common.cancel')} onClick={() => setDeleteWorkId(null)} />
          <Button
            label={t('common.delete')}
            className="p-button-danger"
            onClick={handleDelete}
            loading={deleteWork.isPending}
          />
        </div>
      </Dialog>
    </div>
  );
};

export default WorksPage;
