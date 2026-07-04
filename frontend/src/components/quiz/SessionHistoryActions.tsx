import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { Message } from 'primereact/message';

interface SessionHistoryActionsProps {
  isCompleted: boolean;
  onResume: () => void;
  onRetake: () => void;
  onStartNew: () => void;
  retakeLoading: boolean;
  startNewLoading: boolean;
  completeError?: string;
}

export default function SessionHistoryActions({
  isCompleted,
  onResume,
  onRetake,
  onStartNew,
  retakeLoading,
  startNewLoading,
  completeError,
}: SessionHistoryActionsProps) {
  const { t } = useTranslation();

  return (
    <>
      {completeError && (
        <Message severity="error" text={t('quiz.completeSessionError', { message: completeError })} className="mt-3" />
      )}
      <div className="flex justify-content-end mt-4 gap-2">
        {!isCompleted && (
          <Button
            label={t('common.continue')}
            icon="pi pi-play"
            className="p-button-success"
            onClick={onResume}
          />
        )}
        <Button
          label={t('quiz.retakeQuiz')}
          icon="pi pi-refresh"
          className="p-button-secondary"
          onClick={onRetake}
          loading={retakeLoading}
        />
        <Button
          label={t('quiz.startNewQuiz')}
          icon="pi pi-plus"
          onClick={onStartNew}
          loading={startNewLoading}
        />
      </div>
    </>
  );
}