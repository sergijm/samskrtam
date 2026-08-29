import React from 'react';
import { useTranslation } from 'react-i18next';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import type { CaseEndingDto } from '../../types/content-dtos';

interface CaseEndingsTableProps {
  rows: CaseEndingDto[];
}

const CaseEndingsTable: React.FC<CaseEndingsTableProps> = ({ rows }) => {
  const { t, i18n } = useTranslation();
  const locale = i18n.language;

  const translate = (section: string, value?: string): string => {
    if (!value) return '';
    return t(`${section}.${value}`, value);
  };

  const stemTypeBody = (row: CaseEndingDto) => translate('stemType', row.stemType);
  const genderBody = (row: CaseEndingDto) => translate('gender', row.gender);
  const numberBody = (row: CaseEndingDto) => translate('number', row.number);
  const caseBody = (row: CaseEndingDto) => translate('case', row.grammaticalCase);
  const endingBody = (row: CaseEndingDto) => (
    <span style={{ fontFamily: 'monospace' }}>{row.caseEnding}</span>
  );

  return (
    <div className="overflow-x-auto">
      <DataTable
        value={rows}
        stripedRows
        size="small"
        scrollable
        paginator
        rows={20}
        rowsPerPageOptions={[10, 20, 50, 100]}
        paginatorTemplate="FirstPageLink PrevPageLink PageLinks NextPageLink LastPageLink RowsPerPageDropdown CurrentPageReport"
        currentPageReportTemplate={
          locale === 'ru' ? 'Всего {totalRecords} записей' : 'Total {totalRecords} records'
        }
        className="case-endings-table w-full"
        tableStyle={{ width: '100%' }}
        emptyMessage={locale === 'ru' ? 'Нет данных' : 'No data'}
      >
        <Column
          field="stemType"
          header={t('caseEndings.stemType')}
          headerClassName="case-endings-header"
          body={stemTypeBody}
          sortable
        />
        <Column
          field="gender"
          header={t('caseEndings.gender')}
          headerClassName="case-endings-header"
          body={genderBody}
          sortable
        />
        <Column
          field="number"
          header={t('caseEndings.number')}
          headerClassName="case-endings-header"
          body={numberBody}
          sortable
        />
        <Column
          field="grammaticalCase"
          header={t('caseEndings.grammaticalCase')}
          headerClassName="case-endings-header"
          body={caseBody}
          sortable
        />
        <Column
          field="caseEnding"
          header={t('caseEndings.caseEnding')}
          headerClassName="case-endings-header"
          body={endingBody}
          sortable
        />
      </DataTable>
    </div>
  );
};

export default CaseEndingsTable;
