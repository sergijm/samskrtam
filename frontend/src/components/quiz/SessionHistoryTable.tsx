import { useTranslation } from 'react-i18next';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Tag } from 'primereact/tag';
import { AnswerHistory } from '../../types/quiz';

interface SessionHistoryTableProps {
  answers: AnswerHistory[];
}

export default function SessionHistoryTable({ answers }: SessionHistoryTableProps) {
  const { t, i18n } = useTranslation();

  const resultBodyTemplate = (rowData: AnswerHistory) => {
    if (rowData.isCorrect === undefined || rowData.isCorrect === null) return null;
    return (
      <Tag
        value={rowData.isCorrect ? t('common.correct') : t('common.incorrect')}
        severity={rowData.isCorrect ? 'success' : 'danger'}
      />
    );
  };

  const answeredAtBody = (row: AnswerHistory) =>
    row.answeredAt ? new Date(row.answeredAt).toLocaleString() : null;

  const explanationBody = (row: AnswerHistory) =>
    i18n.language === 'ru' ? row.explanationRu : row.explanationEn || t('quiz.noExplanation');

  const selectedAnswerBody = (row: AnswerHistory) =>
    row.selectedAnswerIast || t('sessionHistory.notAnswered');

  const correctOptionBody = (row: AnswerHistory) =>
    row.correctOptionIast || t('sessionHistory.notApplicable');

  return (
    <DataTable value={answers} sortMode="single" emptyMessage={t('sessionHistory.noAnswersFound')}>
      <Column field="questionText" header={t('sessionHistory.question')} sortable />
      <Column field="selectedAnswerIast" header={t('sessionHistory.yourAnswer')} body={selectedAnswerBody} sortable />
      <Column field="correctOptionIast" header={t('sessionHistory.correctAnswer')} body={correctOptionBody} sortable />
      <Column field="isCorrect" header={t('sessionHistory.result')} body={resultBodyTemplate} sortable />
      <Column field="responseTimeMs" header={t('sessionHistory.responseTime')} sortable />
      <Column field="answeredAt" header={t('sessionHistory.answeredAt')} body={answeredAtBody} sortable />
      <Column field="explanation" header={t('sessionHistory.explanation')} body={explanationBody} sortable />
    </DataTable>
  );
}