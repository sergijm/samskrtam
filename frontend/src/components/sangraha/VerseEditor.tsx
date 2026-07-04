import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { InputTextarea } from 'primereact/inputtextarea';
import { Skeleton } from 'primereact/skeleton';

interface VerseEditorProps {
  editDevanagari: string;
  editIast: string;
  onDevanagariChange: (value: string) => void;
  onIastChange: (value: string) => void;
  isAdmin: boolean;
  isAnalyzing: boolean;
  onSave: () => void;
  onAnalyze: () => void;
  savePending: boolean;
  analyzePending: boolean;
}

export default function VerseEditor({
  editDevanagari,
  editIast,
  onDevanagariChange,
  onIastChange,
  isAdmin,
  isAnalyzing,
  onSave,
  onAnalyze,
  savePending,
  analyzePending,
}: VerseEditorProps) {
  const { t } = useTranslation();

  return (
    <div className="mb-4 verse-editor">
      <div className="mb-3">
        <label className="block mb-1 font-semibold">{t('sangraha.fields.textDevanagari')}</label>
        <InputTextarea
          value={editDevanagari}
          onChange={(e) => onDevanagariChange(e.target.value)}
          className="w-full"
          rows={4}
          placeholder={t('sangraha.placeholder.textDevanagari')}
        />
      </div>
      <div className="mb-3">
        <label className="block mb-1 font-semibold">{t('sangraha.fields.textIast')}</label>
        <InputTextarea
          value={editIast}
          onChange={(e) => onIastChange(e.target.value)}
          className="w-full"
          rows={4}
          placeholder={t('sangraha.placeholder.textIast')}
        />
      </div>
      {isAdmin && !isAnalyzing && (
        <div className="flex gap-2 mt-3">
          <Button
            label={t('sangraha.action.save')}
            icon="pi pi-save"
            onClick={onSave}
            loading={savePending}
          />
          <Button
            label={t('sangraha.action.analyze')}
            icon="pi pi-robot"
            className="p-button-success"
            onClick={onAnalyze}
            loading={analyzePending}
          />
        </div>
      )}
      {isAnalyzing && (
        <div className="mt-3">
          <Skeleton width="100%" height="50px" />
          <p className="mt-2 text-color-secondary">{t('sangraha.status.ANALYZING')}</p>
        </div>
      )}
    </div>
  );
}