import React from 'react';
import { useTranslation } from 'react-i18next';
import { DataTable, DataTableSelectionMultipleChangeEvent } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { ProgressBar } from 'primereact/progressbar';
import type { NumberAggregation } from '../../utils/grammarAggregation';

interface NumberAggregationTableProps {
  aggregations: NumberAggregation[];
  selected: NumberAggregation[];
  onSelectionChange: (selected: NumberAggregation[]) => void;
}

export const NumberAggregationTable: React.FC<NumberAggregationTableProps> = ({
  aggregations,
  selected,
  onSelectionChange,
}) => {
  const { t, i18n } = useTranslation();

  const handleSelectionChange = (e: DataTableSelectionMultipleChangeEvent<NumberAggregation[]>) => {
    onSelectionChange(e.value as NumberAggregation[]);
  };

  return (
    <DataTable
      value={aggregations}
      paginator
      rows={20}
      responsiveLayout="scroll"
      selectionMode="multiple"
      selection={selected}
      onSelectionChange={handleSelectionChange}
      dataKey="numberType"
    >
      <Column selectionMode="multiple" headerStyle={{ width: '3rem' }} />
      <Column
        field="numberType"
        header={t('grammar.lesson.table.number')}
        body={(rowData: NumberAggregation) => (
          <div>{i18n.language === 'ru' ? rowData.numberRu : rowData.numberEn}</div>
        )}
        style={{ width: '30%' }}
        sortable
      />
      <Column
        field="aggregatedProgress"
        header={t('grammar.lesson.table.learned')}
        body={(rowData: NumberAggregation) => (
          <div className="flex align-items-center gap-2">
            <ProgressBar
              value={rowData.aggregatedProgress}
              style={{ height: '8px', width: '80px' }}
              showValue={false}
            />
            <span className="text-primary">
              {rowData.learnedCombinations}/{rowData.totalCombinations}
            </span>
          </div>
        )}
        style={{ width: '25%' }}
        sortable
      />
    </DataTable>
  );
};
