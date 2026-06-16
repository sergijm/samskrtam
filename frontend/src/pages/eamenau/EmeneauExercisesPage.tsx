import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { ProgressSpinner } from 'primereact/progressspinner';
import { EamenauExerciseDto } from '../../types'; // Updated import path
import { contentApi } from '../../api/contentApi'; // Updated import path

const EmeneauExercisesPage = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const { data: exercises, isLoading, error } = useQuery<EamenauExerciseDto[], Error>({
    queryKey: ['eamenau-exercises'],
    queryFn: () => contentApi.getAllEamenauExercises().then(res => res.data)
  });

  const handleRowClick = (event: any) => {
    navigate(`/grammar/emeneau-exercises/${event.data.id}`);
  };

  if (isLoading) {
    return <ProgressSpinner />;
  }

  if (error) {
    return <div>{t('quiz.fetchError', { message: error.message })}</div>;
  }

  const exerciseNumberBodyTemplate = (rowData: EamenauExerciseDto) => {
    return <strong>{`${rowData.exerciseNumber}${rowData.exerciseLetter ? ` ${rowData.exerciseLetter}` : ''}`}</strong>;
  };

  return (
    <div className="p-4">
      <h1 className="text-center mb-5">{t('grammar.sandhiExercisesTitle')}</h1>
      <DataTable value={exercises || []} onRowClick={handleRowClick} className="p-datatable-sm cursor-pointer no-header no-border" showGridlines={false} sortField="exerciseNumber" sortOrder={1}>
        <Column body={exerciseNumberBodyTemplate} style={{ width: '5rem' }} />
        <Column field="instructionText" />
      </DataTable>
    </div>
  );
};

export default EmeneauExercisesPage;
