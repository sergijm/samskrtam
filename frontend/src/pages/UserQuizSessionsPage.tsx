import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { Card } from 'primereact/card';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { PaginatorPageChangeEvent } from 'primereact/paginator';
import { useUserQuizSessions } from '../hooks/useUserQuizSessions';
import { useMe } from '../hooks/useUser';
import { LessonType, SessionStatus } from '../types/quiz';

import QuizSessionFilters from '../components/quiz/QuizSessionFilters';
import QuizSessionsTable from '../components/quiz/QuizSessionsTable';

const UserQuizSessionsPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: user, isLoading: isUserLoading } = useMe();
  const currentUserId = user?.id;

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(10);
  const [sortBy, setSortBy] = useState('startedAt');
  const [sortDirection, setSortDirection] = useState('desc');
  const [quizTypeFilter, setQuizTypeFilter] = useState<LessonType | undefined>(undefined);
  const [statusFilter, setStatusFilter] = useState<SessionStatus | undefined>(undefined);

  const { data, isLoading: isSessionsLoading, isError, error } = useUserQuizSessions(
    currentUserId || '',
    page, size, sortBy, sortDirection, quizTypeFilter, statusFilter
  );

  const handlePageChange = (event: PaginatorPageChangeEvent) => {
    setPage(event.page);
    setSize(event.rows);
  };

  const handleSort = (event: { sortField: string; sortOrder: number }) => {
    setSortBy(event.sortField);
    setSortDirection(event.sortOrder === 1 ? 'asc' : 'desc');
  };

  const handleRowClick = (sessionId: string) => {
    navigate(`/quiz-sessions/${sessionId}/history`);
  };

  if (isUserLoading || isSessionsLoading) {
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
        <QuizSessionFilters
          quizTypeFilter={quizTypeFilter}
          statusFilter={statusFilter}
          onQuizTypeChange={setQuizTypeFilter}
          onStatusChange={setStatusFilter}
          onReset={() => { setQuizTypeFilter(undefined); setStatusFilter(undefined); }}
        />
        <QuizSessionsTable
          sessions={sessions}
          totalRecords={data?.totalElements || 0}
          page={page}
          size={size}
          sortBy={sortBy}
          sortDirection={sortDirection}
          loading={isSessionsLoading}
          onPageChange={handlePageChange}
          onSort={handleSort}
          onRowClick={handleRowClick}
        />
      </Card>
    </div>
  );
};

export default UserQuizSessionsPage;

