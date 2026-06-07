import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom'; // Import useNavigate
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { Paginator, PaginatorPageChangeEvent } from 'primereact/paginator';
import { Dropdown } from 'primereact/dropdown';
import { Button } from 'primereact/button';

import { useUserQuizSessions } from '../hooks/useUserQuizSessions';
import { useAuthStore } from '../store/authStore';
import { QuizType, SessionStatus, QuizSessionSummary } from '../types/quiz';

const UserQuizSessionsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate(); // Initialize useNavigate
  const { user } = useAuthStore();

  const currentUserId = user?.id; // Get current user's ID directly from auth store

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sortBy, setSortBy] = useState('startedAt');
  const [sortDirection, setSortDirection] = useState('desc');
  const [quizTypeFilter, setQuizTypeFilter] = useState<QuizType | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<SessionStatus | undefined>(undefined);

  const { data, isLoading, isError, error } = useUserQuizSessions(
    currentUserId || '', // Pass currentUserId directly
    page,
    size,
    sortBy,
    sortDirection,
    quizTypeFilter,
    statusFilter
  );

  const onPageChange = (event: PaginatorPageChangeEvent) => {
    setPage(event.page);
    setSize(event.rows);
  };

  const onSort = (event: any) => {
    setSortBy(event.sortField);
    setSortDirection(event.sortOrder === 1 ? 'asc' : 'desc');
  };

  const quizTypeOptions = [
    { label: t('common.all'), value: undefined },
    { label: t('quizType.VOCABULARY'), value: 'VOCABULARY' },
    { label: t('quizType.DECLENSIONS'), value: 'DECLENSIONS' },
    { label: t('quizType.CONJUGATIONS'), value: 'CONJUGATIONS' },
    // Add other specific declension types if needed
    { label: t('quizType.A_STEM_DECLENSIONS'), value: 'A_STEM_DECLENSIONS' },
    { label: t('quizType.AA_STEM_DECLENSIONS'), value: 'AA_STEM_DECLENSIONS' },
    { label: t('quizType.I_STEM_DECLENSIONS'), value: 'I_STEM_DECLENSIONS' },
    { label: t('quizType.II_STEM_DECLENSIONS'), value: 'II_STEM_DECLENSIONS' },
    { label: t('quizType.U_STEM_DECLENSIONS'), value: 'U_STEM_DECLENSIONS' },
    { label: t('quizType.UU_STEM_DECLENSIONS'), value: 'UU_STEM_DECLENSIONS' },
    { label: t('quizType.R_STEM_DECLENSIONS'), value: 'R_STEM_DECLENSIONS' },
  ];

  const sessionStatusOptions = [
    { label: t('common.all'), value: undefined },
    { label: t('sessionStatus.IN_PROGRESS'), value: 'IN_PROGRESS' },
    { label: t('sessionStatus.COMPLETED'), value: 'COMPLETED' },
    { label: t('sessionStatus.ABANDONED'), value: 'ABANDONED' },
  ];

  const quizTypeBodyTemplate = (rowData: QuizSessionSummary) => {
    return t(`quizType.${rowData.quizType}`);
  };

  const statusBodyTemplate = (rowData: QuizSessionSummary) => {
    return t(`sessionStatus.${rowData.status}`);
  };

  const startedAtBodyTemplate = (rowData: QuizSessionSummary) => {
    return new Date(rowData.startedAt).toLocaleString();
  };

  const completedAtBodyTemplate = (rowData: QuizSessionSummary) => {
    return rowData.completedAt ? new Date(rowData.completedAt).toLocaleString() : t('common.inProgress');
  };

  const durationBodyTemplate = (rowData: QuizSessionSummary) => {
    if (rowData.durationMs) {
      const seconds = Math.floor(rowData.durationMs / 1000);
      const minutes = Math.floor(seconds / 60);
      const remainingSeconds = seconds % 60;
      return `${minutes}m ${remainingSeconds}s`;
    }
    return '';
  };

  const actionBodyTemplate = (rowData: QuizSessionSummary) => {
    if (rowData.status === 'COMPLETED') {
      return (
        <Button
          icon="pi pi-info-circle"
          className="p-button-rounded p-button-text"
          onClick={(e) => {
            e.stopPropagation(); // Prevent row click from firing
            navigate(`/quiz-sessions/${rowData.sessionId}/history`);
          }}
          tooltip={t('common.viewDetails')}
        />
      );
    }
    return null;
  };

  const onRowClick = (event: any) => {
    const rowData: QuizSessionSummary = event.data;
    if (rowData.status === 'COMPLETED') {
      navigate(`/quiz-sessions/${rowData.sessionId}/history`);
    }
  };

  if (!currentUserId) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('userProfile.errorLoadingUser')} />
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
        <Message severity="error" text={t('userProfile.errorLoadingStatistics', { message: error?.message })} />
      </div>
    );
  }

  const sessions = data?.content || [];

  return (
    <div className="max-w-60rem mx-auto">
      <Card title={t('userProfile.quizSessions')} className="mb-4">
        <div className="flex flex-wrap gap-3 mb-4">
          <Dropdown
            value={quizTypeFilter}
            options={quizTypeOptions}
            onChange={(e) => setQuizTypeFilter(e.value)}
            placeholder={t('common.filterByQuizType')}
            className="w-12rem"
          />
          <Dropdown
            value={statusFilter}
            options={sessionStatusOptions}
            onChange={(e) => setStatusFilter(e.value)}
            placeholder={t('common.filterByStatus')}
            className="w-12rem"
          />
          <Button icon="pi pi-filter-slash" className="p-button-outlined" onClick={() => {
            setQuizTypeFilter(undefined);
            setStatusFilter(undefined);
          }} />
        </div>

        <DataTable
          value={sessions}
          lazy
          paginator={false}
          first={page * size}
          rows={size}
          totalRecords={data?.totalElements}
          onSort={onSort}
          sortField={sortBy}
          sortOrder={sortDirection === 'asc' ? 1 : -1}
          loading={isLoading}
          emptyMessage={t('userProfile.noQuizSessionsFound')}
          selectionMode="single" // Enable row selection
          onRowClick={onRowClick} // Handle row click
          rowClassName={(rowData) => rowData.status === 'COMPLETED' ? 'cursor-pointer' : ''} // Add cursor pointer for clickable rows
        >
          <Column field="quizTitle" header={t('userProfile.quizTitle')} sortable />
          <Column field="quizType" header={t('userProfile.quizType')} body={quizTypeBodyTemplate} sortable />
          <Column field="score" header={t('userProfile.score')} sortable />
          <Column field="totalQuestions" header={t('userProfile.totalQuestions')} sortable />
          <Column field="status" header={t('userProfile.status')} body={statusBodyTemplate} sortable />
          <Column field="startedAt" header={t('userProfile.startedAt')} body={startedAtBodyTemplate} sortable />
          <Column field="completedAt" header={t('userProfile.completedAt')} body={completedAtBodyTemplate} sortable />
          <Column field="durationMs" header={t('userProfile.duration')} body={durationBodyTemplate} sortable />
          <Column body={actionBodyTemplate} header={t('common.actions')} style={{ width: '6rem' }} />
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

export default UserQuizSessionsPage;
