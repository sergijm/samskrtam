import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { CancelButton, PageButton } from '../common/buttons';

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
      style={{ width: '800px' }}
      footer={
        <div>
          <CancelButton labelKey="common.cancel" onClick={onHide} />
          <PageButton variant="dialog-action" labelKey="common.save" iconName="pi-check" onClick={onSave} loading={loading} />
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