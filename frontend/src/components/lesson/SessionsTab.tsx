import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { Tag } from 'primereact/tag';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Paginator, PaginatorPageChangeEvent } from 'primereact/paginator';
import { useLessonSessions } from '../../hooks/useLessonSessions';
import { useMe } from '../../hooks/useUser';
import { SessionHistoryDialog } from './SessionHistoryDialog';
import type { QuizSessionSummary } from '../../types/quiz';
import { SessionStatus } from '../../types/quiz';

interface SessionsTabProps {
  quizId: string;
  slug: string;
  lessonType: string; // e.g. 'declensions', 'vocabulary'
}

export const SessionsTab = ({ quizId, slug, lessonType }: SessionsTabProps) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const { data: user } = useMe();
  const userId = user?.id || '';

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [selectedSessionId, setSelectedSessionId] = useState<string | null>(null);
  const [historyDialogVisible, setHistoryDialogVisible] = useState(false);

  const { data, isLoading } = useLessonSessions(quizId, userId, page, size);

  const handlePageChange = (event: PaginatorPageChangeEvent) => {
    setPage(event.page);
    setSize(event.rows);
  };

  const handleRowClick = (sessionId: string) => {
    setSelectedSessionId(sessionId);
    setHistoryDialogVisible(true);
  };

  const statusBody = (rowData: QuizSessionSummary) => {
    const severity =
      rowData.status === SessionStatus.COMPLETED ? 'success' :
      rowData.status === SessionStatus.IN_PROGRESS ? 'warning' : 'secondary';
    return (
      <Tag
        value={t(`sessionStatus.${rowData.status}`)}
        severity={severity}
      />
    );
  };

  const scoreBody = (rowData: QuizSessionSummary) => {
    const pct = rowData.totalQuestions > 0
      ? Math.round((rowData.score / rowData.totalQuestions) * 100)
      : 0;
    return `${pct}%`;
  };

  const correctBody = (rowData: QuizSessionSummary) =>
    `${rowData.score}/${rowData.totalQuestions}`;

  const startedAtBody = (rowData: QuizSessionSummary) =>
    rowData.startedAt ? new Date(rowData.startedAt).toLocaleString() : '-';

  const actionBody = (rowData: QuizSessionSummary) => {
    if (rowData.status !== SessionStatus.IN_PROGRESS) return null;
    const quizCategory = lessonType.toLowerCase();
    return (
      <Button
        label={t('common.continue')}
        icon="pi pi-play"
        className="p-button-sm p-button-success"
        onClick={(e) => {
          e.stopPropagation();
          navigate(`/quiz/${quizCategory}/${slug}/${rowData.sessionId}`);
        }}
      />
    );
  };

  const sessions = data?.content || [];
  const totalRecords = data?.totalElements || 0;

  return (
    <div>
      {isLoading ? (
        <div className="flex justify-content-center p-4">
          <ProgressSpinner style={{ width: '40px', height: '40px' }} />
        </div>
      ) : (
        <>
          <DataTable
            value={sessions}
            lazy
            paginator={false}
            first={page * size}
            rows={size}
            totalRecords={totalRecords}
            loading={isLoading}
            emptyMessage={t('userProfile.noQuizSessionsFound')}
            onRowClick={(e: any) => handleRowClick(e.data.sessionId)}
            rowClassName={() => 'cursor-pointer'}
          >
            <Column
              field="startedAt"
              header={t('userProfile.startedAt')}
              body={startedAtBody}
              sortable
            />
            <Column
              field="totalQuestions"
              header={t('userProfile.totalQuestions')}
              body={(rowData: QuizSessionSummary) =>
                `${rowData.totalQuestions} ${i18n.language === 'ru' ? 'вопросов' : 'questions'}`
              }
              sortable
            />
            <Column
              field="score"
              header="%"
              body={scoreBody}
              sortable
            />
            <Column
              header={t('common.correct')}
              body={correctBody}
            />
            <Column
              field="status"
              header={t('userProfile.status')}
              body={statusBody}
              sortable
            />
            <Column
              header={t('common.actions')}
              body={actionBody}
              style={{ width: '8rem' }}
            />
          </DataTable>
          <Paginator
            first={page * size}
            rows={size}
            totalRecords={totalRecords}
            onPageChange={handlePageChange}
            rowsPerPageOptions={[10, 20, 50]}
            template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
            currentPageReportTemplate="{first}-{last} of {totalRecords}"
          />
        </>
      )}

      <SessionHistoryDialog
        visible={historyDialogVisible}
        onHide={() => setHistoryDialogVisible(false)}
        sessionId={selectedSessionId}
      />
    </div>
  );
};
