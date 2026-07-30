import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Button } from 'primereact/button';
import { MiniProgressBar } from '../common/MiniProgressBar';
import type { CaseAggregation } from '../../utils/grammarAggregation';

interface CaseAggregationTableProps {
  aggregations: CaseAggregation[];
  quizSlug: string;
}

export const CaseAggregationTable: React.FC<CaseAggregationTableProps> = ({ aggregations, quizSlug }) => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();

  const handleStartCaseQuiz = (caseType: string) => {
                navigate(
      `/quiz/grammar/${quizSlug}?filterScope=CASE_ONLY&filterCaseType=${caseType}`,
  );
};

  return (
    <DataTable value={aggregations} paginator rows={20} responsiveLayout="scroll">
      <Column
        header={i18n.language === 'ru' ? 'Падеж' : 'Case'}
        body={(rowData: CaseAggregation) => (
          <div>{i18n.language === 'ru' ? rowData.caseRu : rowData.caseEn}</div>
        )}
        style={{ width: '30%' }}
        sortable
        sortField="caseType"
      />
      <Column
        header={i18n.language === 'ru' ? 'Изучено' : 'Learned'}
        body={(rowData: CaseAggregation) => (
          <MiniProgressBar
              value={rowData.aggregatedProgress}
            status={rowData.status}
              onClick={() => handleStartCaseQuiz(rowData.caseType)}
          />
        )}
        style={{ width: '30%' }}
        sortable
        sortField="aggregatedProgress"
      />
      <Column
        body={(rowData: CaseAggregation) => (
          <Button
            icon="pi pi-play"
            className="p-button-rounded p-button-text p-button-sm"
            style={{ width: '1.6rem', height: '1.6rem', padding: 0 }}
            onClick={() => handleStartCaseQuiz(rowData.caseType)}
            tooltip={i18n.language === 'ru' ? 'Начать' : 'Start'}
          />
        )}
        style={{ width: '10%' }}
      />
    </DataTable>
  );
};

