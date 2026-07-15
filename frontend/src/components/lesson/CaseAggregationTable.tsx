import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { ProgressBar } from 'primereact/progressbar';
import { WordStatusIcon } from './WordStatusIcon';
import type { CaseAggregation } from '../../utils/grammarAggregation';

interface CaseAggregationTableProps {
  aggregations: CaseAggregation[];
  quizSlug: string;
}

export const CaseAggregationTable: React.FC<CaseAggregationTableProps> = ({ aggregations, quizSlug }) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();

  return (
    <DataTable value={aggregations} paginator rows={20} responsiveLayout="scroll">
      <Column
        header={i18n.language === 'ru' ? 'Статус' : 'Status'}
        body={(rowData: CaseAggregation) => <WordStatusIcon status={rowData.status} />}
        style={{ width: '10%' }}
        sortable
        sortField="status"
      />
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
          <div className="flex align-items-center gap-2">
            <ProgressBar
              value={rowData.aggregatedProgress}
              style={{ height: '8px', width: '80px' }}
              showValue={false}
            />
            <span
              className="cursor-pointer underline text-primary"
              onClick={() =>
                navigate(
                  `/quiz/grammar/${quizSlug}?filterScope=CASE_ONLY&filterCaseType=${rowData.caseType}`,
                )
              }
            >
              {rowData.aggregatedProgress}%
            </span>
          </div>
        )}
        style={{ width: '25%' }}
        sortable
        sortField="aggregatedProgress"
      />
    </DataTable>
  );
};
