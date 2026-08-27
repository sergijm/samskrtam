import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Paginator, PaginatorPageChangeEvent } from 'primereact/paginator';
import { Button } from 'primereact/button';
import { QuizSessionSummary, SessionStatus } from '../../types/quiz';

interface QuizSessionsTableProps {
  sessions: QuizSessionSummary[];
  totalRecords: number;
  page: number;
  size: number;
  sortBy: string;
  sortDirection: string;
  loading: boolean;
  onPageChange: (event: PaginatorPageChangeEvent) => void;
  onSort: (event: { sortField: string; sortOrder: number }) => void;
  onRowClick: (sessionId: string) => void;
}

const quizTypeBodyTemplate = (t: (key: string) => string) => (rowData: QuizSessionSummary) => t(`lessonType.${rowData.lessonType}`);
const statusBodyTemplate = (t: (key: string) => string) => (rowData: QuizSessionSummary) => t(`sessionStatus.${rowData.status}`);
const startedAtBodyTemplate = (rowData: QuizSessionSummary) => new Date(rowData.startedAt).toLocaleString();
const completedAtBodyTemplate = (t: (key: string) => string) => (rowData: QuizSessionSummary) =>
  rowData.completedAt ? new Date(rowData.completedAt).toLocaleString() : t('common.inProgress');
const durationBodyTemplate = (rowData: QuizSessionSummary) => {
  if (!rowData.durationMs) return '';
  const seconds = Math.floor(rowData.durationMs / 1000);
  return `${Math.floor(seconds / 60)}m ${seconds % 60}s`;
};

export default function QuizSessionsTable({
  sessions,
  totalRecords,
  page,
  size,
  sortBy,
  sortDirection,
  loading,
  onPageChange,
  onSort,
  onRowClick,
}: QuizSessionsTableProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const actionBodyTemplate = (rowData: QuizSessionSummary) => {
    const isCompleted = rowData.status === SessionStatus.COMPLETED;
    const quizCategory = rowData.lessonType.toLowerCase();

    return (
      <div className="flex gap-2">
        <Button
          icon="pi pi-info-circle"
          className="p-button-rounded p-button-text"
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/quiz-sessions/${rowData.sessionId}/history`);
          }}
          tooltip={t('common.viewDetails')}
        />
        {!isCompleted && (
          <Button
            icon="pi pi-play"
            className="p-button-rounded p-button-text p-button-success"
            onClick={(e) => {
              e.stopPropagation();
              window.open(`/quiz/${quizCategory}/${rowData.slug}/${rowData.sessionId}`, '_blank');
            }}
            tooltip={t('common.continue')}
          />
        )}
      </div>
    );
  };

  const handleSort = (event: { sortField: string; sortOrder: number }) => {
    onSort(event);
  };

  return (
    <>
      <DataTable
        value={sessions}
        lazy
        paginator={false}
        first={page * size}
        rows={size}
        totalRecords={totalRecords}
        onSort={handleSort}
        sortField={sortBy}
        sortOrder={sortDirection === 'asc' ? 1 : -1}
        loading={loading}
        emptyMessage={t('userProfile.noQuizSessionsFound')}
        selectionMode="single"
        onRowClick={(e: any) => onRowClick(e.data.sessionId)}
        rowClassName={() => 'cursor-pointer'}
      >
        <Column field="quizTitle" header={t('userProfile.quizTitle')} sortable />
        <Column field="lessonType" header={t('userProfile.lessonType')} body={quizTypeBodyTemplate(t)} sortable />
        <Column field="score" header={t('userProfile.score')} sortable />
        <Column field="totalQuestions" header={t('userProfile.totalQuestions')} sortable />
        <Column field="status" header={t('userProfile.status')} body={statusBodyTemplate(t)} sortable />
        <Column field="startedAt" header={t('userProfile.startedAt')} body={startedAtBodyTemplate} sortable />
        <Column field="completedAt" header={t('userProfile.completedAt')} body={completedAtBodyTemplate(t)} sortable />
        <Column field="durationMs" header={t('userProfile.duration')} body={durationBodyTemplate} sortable />
        <Column body={actionBodyTemplate} header={t('common.actions')} style={{ width: '8rem' }} />
      </DataTable>
      <Paginator
        first={page * size}
        rows={size}
        totalRecords={totalRecords}
        onPageChange={onPageChange}
        rowsPerPageOptions={[10, 20, 50]}
        template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
        currentPageReportTemplate="{first}-{last} of {totalRecords}"
      />
    </>
  );
}