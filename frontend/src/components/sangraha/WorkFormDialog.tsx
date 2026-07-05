import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputTextarea } from 'primereact/inputtextarea';
import { Button } from 'primereact/button';

interface WorkForm {
  title: string;
  description: string;
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
          <label>{t('sangraha.fields.title')}</label>
          <InputText value={form.title} onChange={(e) => onFormChange({ ...form, title: e.target.value })} className="w-full" />
        </div>
        <div>
          <label>{t('sangraha.fields.description')}</label>
          <InputTextarea value={form.description} onChange={(e) => onFormChange({ ...form, description: e.target.value })} className="w-full" rows={3} />
        </div>
        <Button label={t('common.create')} onClick={onSave} loading={loading} />
      </div>
    </Dialog>
  );
}