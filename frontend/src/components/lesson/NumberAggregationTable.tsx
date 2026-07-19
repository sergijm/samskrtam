import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { ProgressBar } from 'primereact/progressbar';
import { Button } from 'primereact/button';
import { statusToProgressColor } from '../../utils/statusColor';
import type { NumberAggregation } from '../../utils/grammarAggregation';

interface NumberAggregationTableProps {
  aggregations: NumberAggregation[];
  quizSlug: string;
}

export const NumberAggregationTable: React.FC<NumberAggregationTableProps> = ({ aggregations, quizSlug }) => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();

  const handleStartNumberQuiz = (numberType: string) => {
    navigate(
      `/quiz/grammar/${quizSlug}?filterScope=NUMBER_ONLY&filterNumberTypes=${numberType}`,
    );
  };

  return (
    <DataTable value={aggregations} paginator rows={20} responsiveLayout="scroll">
      <Column
        header={i18n.language === 'ru' ? 'Число' : 'Number'}
        body={(rowData: NumberAggregation) => (
          <div>{i18n.language === 'ru' ? rowData.numberRu : rowData.numberEn}</div>
        )}
        style={{ width: '30%' }}
        sortable
        sortField="numberType"
      />
      <Column
        header={i18n.language === 'ru' ? 'Изучено' : 'Learned'}
        body={(rowData: NumberAggregation) => (
          <div className="flex align-items-center gap-2">
            <ProgressBar
              value={rowData.aggregatedProgress}
              color={statusToProgressColor(rowData.status)}
              style={{ height: '5px', width: '80px' }}
              showValue={false}
            />
            <span
              className="cursor-pointer underline text-primary"
              onClick={() => handleStartNumberQuiz(rowData.numberType)}
            >
              {rowData.aggregatedProgress}%
            </span>
          </div>
        )}
        style={{ width: '30%' }}
        sortable
        sortField="aggregatedProgress"
      />
      <Column
        body={(rowData: NumberAggregation) => (
          <Button
            icon="pi pi-play"
            className="p-button-rounded p-button-text p-button-sm"
            style={{ width: '1.6rem', height: '1.6rem', padding: 0 }}
            onClick={() => handleStartNumberQuiz(rowData.numberType)}
            tooltip={i18n.language === 'ru' ? 'Начать' : 'Start'}
          />
        )}
        style={{ width: '10%' }}
      />
    </DataTable>
  );
};
