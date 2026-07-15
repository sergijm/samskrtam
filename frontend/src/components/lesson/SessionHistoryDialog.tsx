import React from 'react';
import { Dialog } from 'primereact/dialog';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Skeleton } from 'primereact/skeleton';
import { Tag } from 'primereact/tag';
import { useTranslation } from 'react-i18next';
import { useMe } from '../../hooks/useUser';
import { useSessionAnswerHistory } from '../../hooks/useUserQuizSessions';
import type { AnswerHistory } from '../../types/quiz';

interface SessionHistoryDialogProps {
  visible: boolean;
  onHide: () => void;
  sessionId: string | null;
}

export const SessionHistoryDialog = ({ visible, onHide, sessionId }: SessionHistoryDialogProps) => {
  const { t } = useTranslation();
  const { data: user } = useMe();
  const userId = user?.id;

  const { data: answers, isLoading } = useSessionAnswerHistory(
    sessionId || '',
    userId || '',
  );

  const resultBody = (rowData: AnswerHistory) => {
    if (rowData.isCorrect === undefined || rowData.isCorrect === null) return null;
    return (
      <Tag
        value={rowData.isCorrect ? t('common.correct') : t('common.incorrect')}
        severity={rowData.isCorrect ? 'success' : 'danger'}
      />
    );
  };

  const correctAnswerBody = (rowData: AnswerHistory) =>
    rowData.correctOptionIast || '-';

  const userAnswerBody = (rowData: AnswerHistory) => {
    const isCorrect = rowData.isCorrect;
    const text = rowData.selectedAnswerIast || t('sessionHistory.notAnswered');
    if (isCorrect === undefined || isCorrect === null) return text;
    return (
      <span className={isCorrect ? 'text-green-600 font-bold' : 'text-red-600 font-bold'}>
        {text}
      </span>
    );
  };

  return (
    <Dialog
      visible={visible}
      onHide={onHide}
      header={t('sessionHistory.title', { sessionId: sessionId?.substring(0, 8) || '' })}
      style={{ width: '80vw' }}
      maximizable
    >
      {isLoading ? (
        <div className="p-4">
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
          <Skeleton width="100%" height="20px" className="mb-2" />
        </div>
      ) : (
        <DataTable
          value={answers || []}
          paginator
          rows={10}
          responsiveLayout="scroll"
          emptyMessage={t('sessionHistory.noAnswersFound')}
        >
          <Column
            header="#"
            body={(_row, options) => options.rowIndex + 1}
            style={{ width: '5%' }}
          />
          <Column
            field="questionText"
            header={t('sessionHistory.question')}
            style={{ width: '35%' }}
          />
          <Column
            field="correctOptionIast"
            header={t('sessionHistory.correctAnswer')}
            body={correctAnswerBody}
            style={{ width: '20%' }}
          />
          <Column
            field="selectedAnswerIast"
            header={t('sessionHistory.yourAnswer')}
            body={userAnswerBody}
            style={{ width: '20%' }}
          />
          <Column
            field="isCorrect"
            header={t('sessionHistory.result')}
            body={resultBody}
            style={{ width: '10%' }}
          />
        </DataTable>
      )}
    </Dialog>
  );
};
