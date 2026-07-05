import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Button } from 'primereact/button';

interface ChapterForm {
  title: string;
  orderIndex: number | null;
}

interface ChapterDialogProps {
  visible: boolean;
  onHide: () => void;
  form: ChapterForm;
  onFormChange: (form: ChapterForm) => void;
  onSave: () => void;
  loading: boolean;
}

export default function ChapterDialog({
  visible,
  onHide,
  form,
  onFormChange,
  onSave,
  loading,
}: ChapterDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog
      header={t('sangraha.addChapter')}
      visible={visible}
      onHide={onHide}
      style={{ width: '400px' }}
      footer={
        <div>
          <Button label={t('common.cancel')} icon="pi pi-times" className="p-button-text" onClick={onHide} />
          <Button label={t('common.save')} icon="pi pi-check" onClick={onSave} loading={loading} />
        </div>
      }
    >
      <div className="flex flex-column gap-3">
        <div>
          <label htmlFor="ch-title">{t('sangraha.fields.title')}</label>
          <InputText id="ch-title" value={form.title} onChange={(e) => onFormChange({ ...form, title: e.target.value })} className="w-full" />
          <small className="text-color-secondary">{t('sangraha.placeholder.text')}</small>
        </div>
        <div>
          <label htmlFor="ch-order">{t('sangraha.fields.orderIndex')}</label>
          <InputNumber id="ch-order" value={form.orderIndex} onValueChange={(e) => onFormChange({ ...form, orderIndex: e.value ?? null })} className="w-full" useGrouping={false} />
          <small className="text-color-secondary">{t('sangraha.orderIndexOptional')}</small>
        </div>
      </div>
    </Dialog>
  );
}