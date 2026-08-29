import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { Message } from 'primereact/message';

interface SessionHistoryActionsProps {
  isCompleted: boolean;
  onBackToLesson: () => void;
  completeError?: string;
}

export default function SessionHistoryActions({
  isCompleted,
  onBackToLesson,
  completeError,
}: SessionHistoryActionsProps) {
  const { t } = useTranslation();

  return (
    <>
      {completeError && (
        <Message severity="error" text={t('quiz.completeSessionError', { message: completeError })} className="mt-3" />
      )}
      <div className="flex justify-content-end mt-4 gap-2">
        <Button
          label={t('common.backToLesson')}
          icon="pi pi-arrow-left"
          className="p-button-outlined"
          onClick={onBackToLesson}
        />
      </div>
    </>
  );
}