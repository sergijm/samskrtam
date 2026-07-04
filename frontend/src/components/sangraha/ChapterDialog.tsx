import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputText } from 'primereact/inputtext';
import { InputNumber } from 'primereact/inputnumber';
import { Button } from 'primereact/button';

interface ChapterForm {
  slug: string;
  titleRu: string;
  titleEn: string;
  orderIndex: number;
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
          <label htmlFor="ch-slug">{t('sangraha.slug')}</label>
          <InputText id="ch-slug" value={form.slug} onChange={(e) => onFormChange({ ...form, slug: e.target.value })} className="w-full" />
        </div>
        <div>
          <label htmlFor="ch-titleRu">{t('sangraha.titleRu')}</label>
          <InputText id="ch-titleRu" value={form.titleRu} onChange={(e) => onFormChange({ ...form, titleRu: e.target.value })} className="w-full" />
        </div>
        <div>
          <label htmlFor="ch-titleEn">{t('sangraha.titleEn')}</label>
          <InputText id="ch-titleEn" value={form.titleEn} onChange={(e) => onFormChange({ ...form, titleEn: e.target.value })} className="w-full" />
        </div>
        <div>
          <label htmlFor="ch-order">{t('sangraha.orderIndex')}</label>
          <InputNumber id="ch-order" value={form.orderIndex} onValueChange={(e) => onFormChange({ ...form, orderIndex: e.value ?? 0 })} className="w-full" />
        </div>
      </div>
    </Dialog>
  );
}