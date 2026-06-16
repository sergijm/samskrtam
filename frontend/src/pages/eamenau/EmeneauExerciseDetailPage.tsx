import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { useParams, useNavigate, Link } from 'react-router-dom'; // Added Link import
import { ProgressSpinner } from 'primereact/progressspinner';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { EamenauExerciseDetailDto, EamenauExerciseDto, EamenauTaskDto, SandhiRuleInfo } from '../../types';
import { contentApi } from '../../api/contentApi';
import SolutionPanel from '../../components/eamenau/SolutionPanel';
import { Tooltip } from 'primereact/tooltip';

const EmeneauExerciseDetailPage = () => {
  const { t } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [expandedRows, setExpandedRows] = useState<any>(null);

  const { data: exercises, isLoading: isLoadingExercises } = useQuery<EamenauExerciseDto[], Error>({
    queryKey: ['eamenau-exercises'],
    queryFn: () => contentApi.getAllEamenauExercises().then(res => res.data),
  });

  const { data: exercise, isLoading: isLoadingExercise, error } = useQuery<EamenauExerciseDetailDto, Error>({
    queryKey: ['eamenau-exercise', id],
    queryFn: () => contentApi.getEamenauExerciseById(id!).then(res => res.data),
    enabled: !!id,
  });

  const { data: uniqueSandhiRules, isLoading: isLoadingUniqueSandhiRules } = useQuery<SandhiRuleInfo[], Error>({
    queryKey: ['unique-sandhi-rules', id],
    queryFn: () => contentApi.getUniqueSandhiRulesForExercise(parseInt(id!)).then(res => res.data),
    enabled: !!id,
  });

  const currentExerciseIndex = exercises?.findIndex(ex => ex.id === parseInt(id!));

  const handlePrev = () => {
    if (exercises && currentExerciseIndex !== undefined && currentExerciseIndex > 0) {
      setExpandedRows(null);
      navigate(`/grammar/emeneau-exercises/${exercises[currentExerciseIndex - 1].id}`);
    }
  };

  const handleNext = () => {
    if (exercises && currentExerciseIndex !== undefined && currentExerciseIndex < exercises.length - 1) {
      setExpandedRows(null);
      navigate(`/grammar/emeneau-exercises/${exercises[currentExerciseIndex + 1].id}`);
    }
  };

  const rowExpansionTemplate = (data: EamenauTaskDto) => {
    return <SolutionPanel taskId={data.id} />;
  };

  const getFilterRulesLink = () => {
    if (uniqueSandhiRules && uniqueSandhiRules.length > 0) {
      const queryParams = uniqueSandhiRules.map(rule => `rule=${rule.ruleNumber}`).join('&');
      return `/grammar/emeneau-rules?${queryParams}`;
    }
    return '#'; // Fallback link
  };

  if (isLoadingExercises || isLoadingExercise || isLoadingUniqueSandhiRules) {
    return <ProgressSpinner />;
  }

  if (error) {
    return <div>{t('quiz.fetchError', { message: error.message })}</div>;
  }

  if (!exercise) {
    return <div>{t('quiz.noQuizzesFound')}</div>;
  }

  return (
    <div className="p-4">
      <div className="flex justify-content-between align-items-center mb-5">
        <Button icon="pi pi-chevron-left" onClick={handlePrev} disabled={currentExerciseIndex === 0} />
        <h1 className="text-center">
          {t('eamenau.exerciseNumber')} {exercise.exerciseNumber}
          {exercise.exerciseLetter && ` ${exercise.exerciseLetter}`}
        </h1>
        <Button icon="pi pi-chevron-right" iconPos="right" onClick={handleNext} disabled={currentExerciseIndex === (exercises?.length ?? 0) - 1} />
      </div>
      <p className="mb-4">{exercise.instructionText}</p>

      {uniqueSandhiRules && uniqueSandhiRules.length > 0 && (
        <div className="flex justify-content-end mb-4">
          <Link to={getFilterRulesLink()} className="p-link flex align-items-center"> {/* Changed span to Link */}
            {t('eamenau.rules')}:{' '}
            {uniqueSandhiRules.map((rule, index) => (
              <React.Fragment key={rule.ruleNumber}>
                <span
                  data-pr-tooltip={rule.shortDescription}
                  data-pr-position="top"
                  className="rule-number-link"
                >
                  {rule.ruleNumber}
                </span>
                {index < uniqueSandhiRules.length - 1 && ', '}
                <Tooltip target={`.rule-number-link[data-pr-tooltip="${rule.shortDescription}"]`} />
              </React.Fragment>
            ))}
          </Link>
        </div>
      )}

      <DataTable
        value={exercise.tasks}
        expandedRows={expandedRows}
        onRowToggle={(e) => setExpandedRows(e.data)}
        rowExpansionTemplate={rowExpansionTemplate}
        dataKey="id"
        className="p-datatable-sm no-header no-border"
        showGridlines={false}
        sortField="taskNumber"
        sortOrder={1}
      >
        <Column expander style={{ width: '3em' }} />
        <Column field="taskNumber" style={{ width: '5rem' }} />
        <Column field="taskText" />
      </DataTable>
    </div>
  );
};

export default EmeneauExerciseDetailPage;
