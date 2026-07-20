import { useTranslation } from 'react-i18next';
import { Dialog } from 'primereact/dialog';
import { CancelButton, DangerButton } from '../common/buttons';

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
        <CancelButton labelKey="common.cancel" onClick={onHide} />
        <DangerButton labelKey="common.delete" onClick={onConfirm} loading={loading} />
      </div>
    </Dialog>
  );
}