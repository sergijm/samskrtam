import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';

interface DeleteConfirmDialogProps {
  visible: boolean;
  onHide: () => void;
  onConfirm: () => void;
  loading: boolean;
}

export default function DeleteConfirmDialog({ visible, onHide, onConfirm, loading }: DeleteConfirmDialogProps) {
  const { t } = useTranslation();

  return (
    <Dialog header={t('sangraha.deleteWork')} visible={visible} onHide={onHide} style={{ width: '400px' }}>
      <p>{t('common.confirm')}</p>
      <div className="flex justify-content-end gap-2 mt-3">
        <Button label={t('common.cancel')} onClick={onHide} />
        <Button label={t('common.delete')} className="p-button-danger" onClick={onConfirm} loading={loading} />
      </div>
    </Dialog>
  );
}