import React, { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Paginator, PaginatorPageChangeEvent } from 'primereact/paginator';
import { Tag } from 'primereact/tag';

import { useSessionAnswerHistory } from '../hooks/useSessionAnswerHistory';
import { useAuthStore } from '../store/authStore';
import { AnswerHistory } from '../types/quiz';

const SessionHistoryPage = () => {
  const { t } = useTranslation();
  const { sessionId } = useParams<{ sessionId: string }>();
  const { user } = useAuthStore();

  const currentUserId = user?.id;

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sortBy, setSortBy] = useState('answeredAt');
  const [sortDirection, setSortDirection] = useState('asc');

  const { data, isLoading, isError, error } = useSessionAnswerHistory(
    sessionId || '',
    currentUserId || '',
    page,
    size,
    sortBy,
    sortDirection
  );

  const onPageChange = (event: PaginatorPageChangeEvent) => {
    setPage(event.page);
    setSize(event.rows);
  };

  const onSort = (event: any) => {
    setSortBy(event.sortField);
    setSortDirection(event.sortOrder === 1 ? 'asc' : 'desc');
  };

  const isCorrectBodyTemplate = (rowData: AnswerHistory) => {
    return (
      <Tag
        value={rowData.isCorrect ? t('common.correct') : t('common.incorrect')}
        severity={rowData.isCorrect ? 'success' : 'danger'}
      />
    );
  };

  const answeredAtBodyTemplate = (rowData: AnswerHistory) => {
    return new Date(rowData.answeredAt).toLocaleString();
  };

  if (!sessionId || !currentUserId) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory')} />
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('sessionHistory.errorLoadingHistory', { message: error?.message })} />
      </div>
    );
  }

  const answers = data?.content || [];

  return (
    <div className="max-w-60rem mx-auto">
      <Card title={t('sessionHistory.title', { sessionId: sessionId.substring(0, 8) })} className="mb-4">
        <DataTable
          value={answers}
          lazy
          paginator={false}
          first={page * size}
          rows={size}
          totalRecords={data?.totalElements}
          onSort={onSort}
          sortField={sortBy}
          sortOrder={sortDirection === 'asc' ? 1 : -1}
          loading={isLoading}
          emptyMessage={t('sessionHistory.noAnswersFound')}
        >
          <Column field="questionText" header={t('sessionHistory.question')} />
          <Column field="selectedAnswerIast" header={t('sessionHistory.yourAnswer')} />
          <Column field="correctOptionIast" header={t('sessionHistory.correctAnswer')} />
          <Column field="isCorrect" header={t('sessionHistory.result')} body={isCorrectBodyTemplate} />
          <Column field="responseTimeMs" header={t('sessionHistory.responseTime')} />
          <Column field="answeredAt" header={t('sessionHistory.answeredAt')} body={answeredAtBodyTemplate} sortable />
          <Column field="explanation" header={t('sessionHistory.explanation')} />
        </DataTable>

        <Paginator
          first={page * size}
          rows={size}
          totalRecords={data?.totalElements}
          onPageChange={onPageChange}
          rowsPerPageOptions={[10, 20, 50]}
          template="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink CurrentPageReport RowsPerPageDropdown"
          currentPageReportTemplate="{first}-{last} of {totalRecords}"
        />
      </Card>
    </div>
  );
};

export default SessionHistoryPage;
