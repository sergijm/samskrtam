import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { PageButton } from '../common/buttons';

interface WorkEditForm {
  titleRu: string;
  titleEn: string;
  descriptionRu: string;
  descriptionEn: string;
  author: string;
}

interface WorkEditDialogProps {
  visible: boolean;
  onHide: () => void;
  form: WorkEditForm;
  onFormChange: (form: WorkEditForm) => void;
  onSave: () => void;
  loading: boolean;
}

export default function WorkEditDialog({ visible, onHide, form, onFormChange, onSave, loading }: WorkEditDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog header={t('sangraha.editWork')} visible={visible} onHide={onHide} style={{ width: '800px' }}>
      <div className="flex flex-column gap-3">
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
        <PageButton variant="dialog-action" labelKey="common.save" onClick={onSave} loading={loading} />
      </div>
    </Dialog>
  );
}
