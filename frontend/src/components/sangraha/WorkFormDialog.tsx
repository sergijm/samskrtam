import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Button } from 'primereact/button';

interface WorkForm {
  slug: string;
  titleRu: string;
  titleEn: string;
  descriptionRu: string;
  descriptionEn: string;
  author: string;
}

interface WorkFormDialogProps {
  visible: boolean;
  onHide: () => void;
  form: WorkForm;
  onFormChange: (form: WorkForm) => void;
  onSave: () => void;
  loading: boolean;
}

export default function WorkFormDialog({ visible, onHide, form, onFormChange, onSave, loading }: WorkFormDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog header={t('sangraha.addWork')} visible={visible} onHide={onHide} style={{ width: '500px' }}>
      <div className="flex flex-column gap-3">
        <div>
          <label>{t('sangraha.fields.slug')}</label>
          <InputText value={form.slug} onChange={(e) => onFormChange({ ...form, slug: e.target.value })} className="w-full" />
        </div>
        <div>
          <label>{t('sangraha.fields.titleRu')}</label>
          <InputText value={form.titleRu} onChange={(e) => onFormChange({ ...form, titleRu: e.target.value })} className="w-full" />
        </div>
        <div>
          <label>{t('sangraha.fields.titleEn')}</label>
          <InputText value={form.titleEn} onChange={(e) => onFormChange({ ...form, titleEn: e.target.value })} className="w-full" />
        </div>
        <div>
          <label>{t('sangraha.fields.descriptionRu')}</label>
          <InputTextarea value={form.descriptionRu} onChange={(e) => onFormChange({ ...form, descriptionRu: e.target.value })} className="w-full" rows={3} />
        </div>
        <div>
          <label>{t('sangraha.fields.descriptionEn')}</label>
          <InputTextarea value={form.descriptionEn} onChange={(e) => onFormChange({ ...form, descriptionEn: e.target.value })} className="w-full" rows={3} />
        </div>
        <div>
          <label>{t('sangraha.fields.author')}</label>
          <InputText value={form.author} onChange={(e) => onFormChange({ ...form, author: e.target.value })} className="w-full" />
        </div>
        <Button label={t('common.create')} onClick={onSave} loading={loading} />
      </div>
    </Dialog>
  );
}