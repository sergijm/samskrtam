import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { InputNumber } from 'primereact/inputnumber';
import { Button } from 'primereact/button';

interface VerseDialogProps {
  visible: boolean;
  onHide: () => void;
  orderIndex: number;
  onOrderIndexChange: (value: number) => void;
  onSave: () => void;
  loading: boolean;
}

export default function VerseDialog({
  visible,
  onHide,
  orderIndex,
  onOrderIndexChange,
  onSave,
  loading,
}: VerseDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog
      header={t('sangraha.addVerse')}
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
          <label htmlFor="v-order">{t('sangraha.orderIndex')}</label>
          <InputNumber id="v-order" value={orderIndex} onValueChange={(e) => onOrderIndexChange(e.value ?? 0)} className="w-full" />
        </div>
      </div>
    </Dialog>
  );
}