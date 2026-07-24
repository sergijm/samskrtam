import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputNumber } from 'primereact/inputnumber';
import { InputTextarea } from 'primereact/inputtextarea';
import { CancelButton, PageButton } from '../common/buttons';

interface VerseDialogProps {
  visible: boolean;
  onHide: () => void;
  orderIndex: number;
  onOrderIndexChange: (value: number) => void;
  textDevanagari: string;
  onTextDevanagariChange: (value: string) => void;
  textIast: string;
  onTextIastChange: (value: string) => void;
  onSave: () => void;
  loading: boolean;
}

export default function VerseDialog({
  visible,
  onHide,
  orderIndex,
  onOrderIndexChange,
  textDevanagari,
  onTextDevanagariChange,
  textIast,
  onTextIastChange,
  onSave,
  loading,
}: VerseDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog
      header={t('sangraha.addVerse')}
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
          <label htmlFor="v-order">{t('sangraha.orderIndex')}</label>
          <InputNumber id="v-order" value={orderIndex} onValueChange={(e) => onOrderIndexChange(e.value ?? 0)} className="w-full" />
        </div>
        <div>
          <label htmlFor="v-devanagari">{t('sangraha.fields.textDevanagari')}</label>
          <InputTextarea
            id="v-devanagari"
            value={textDevanagari}
            onChange={(e) => onTextDevanagariChange(e.target.value)}
            className="w-full"
            rows={3}
            placeholder={t('sangraha.placeholder.textDevanagari')}
          />
        </div>
        <div>
          <label htmlFor="v-iast">{t('sangraha.fields.textIast')}</label>
          <InputTextarea
            id="v-iast"
            value={textIast}
            onChange={(e) => onTextIastChange(e.target.value)}
            className="w-full"
            rows={3}
            placeholder={t('sangraha.placeholder.textIast')}
          />
        </div>
      </div>
    </Dialog>
  );
}
