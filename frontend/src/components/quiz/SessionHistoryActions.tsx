import { useTranslation } from 'react-i18next';
import { Message } from 'primereact/message';
import { PageButton, CtaButton } from '../common/buttons';

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
          <CtaButton
            labelKey="common.continue"
            className="p-button-success"
            onClick={onResume}
          />
        )}
        <PageButton
          variant="navigation"
          labelKey="quiz.retakeQuiz"
          className="p-button-secondary"
          onClick={onRetake}
          loading={retakeLoading}
        />
        <PageButton
          variant="page-action"
          labelKey="quiz.startNewQuiz"
          onClick={onStartNew}
          loading={startNewLoading}
        />
      </div>
    </>
  );
}