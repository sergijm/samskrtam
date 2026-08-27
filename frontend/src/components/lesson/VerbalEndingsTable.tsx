import React from 'react';
import { useTranslation } from 'react-i18next';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import type { VerbalEndingDto } from '../../types/content-dtos';

interface VerbalEndingsTableProps {
  rows: VerbalEndingDto[];
}

const VerbalEndingsTable: React.FC<VerbalEndingsTableProps> = ({ rows }) => {
  const { t, i18n } = useTranslation();
  const locale = i18n.language;

  const translate = (section: string, value?: string): string => {
    if (!value) return '';
    return t(`${section}.${value}`, value);
  };

  const endingBody = (row: VerbalEndingDto) => (
    <span style={{ fontFamily: 'monospace' }}>{row.ending}</span>
  );
  const suffixBody = (row: VerbalEndingDto) =>
    row.lemmaSuffix ? <span style={{ fontFamily: 'monospace' }}>{row.lemmaSuffix}</span> : '—';
  const augmentBody = (row: VerbalEndingDto) =>
    row.hasAugment ? '+' : '—';
  const tenseMoodBody = (row: VerbalEndingDto) => translate('tenseMood', row.tenseMood);
  const personNumberBody = (row: VerbalEndingDto) => translate('personNumber', row.personNumber);
  const padaBody = (row: VerbalEndingDto) => translate('pada', row.pada);
  const notesBody = (row: VerbalEndingDto) => row.notes || '';

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
        className="verbal-endings-table w-full"
        tableStyle={{ width: '100%' }}
        emptyMessage={locale === 'ru' ? 'Нет данных' : 'No data'}
      >
        <Column
          field="ending"
          header={t('verbalEndings.ending')}
          headerClassName="verbal-endings-header"
          body={endingBody}
          sortable
        />
        <Column
          field="lemmaSuffix"
          header={t('verbalEndings.lemmaSuffix')}
          headerClassName="verbal-endings-header"
          body={suffixBody}
          sortable
        />
        <Column
          field="hasAugment"
          header={t('verbalEndings.hasAugment')}
          headerClassName="verbal-endings-header"
          body={augmentBody}
          sortable
        />
        <Column
          field="tenseMood"
          header={t('verbalEndings.tenseMood')}
          headerClassName="verbal-endings-header"
          body={tenseMoodBody}
          sortable
        />
        <Column
          field="personNumber"
          header={t('verbalEndings.personNumber')}
          headerClassName="verbal-endings-header"
          body={personNumberBody}
          sortable
        />
        <Column
          field="pada"
          header={t('verbalEndings.pada')}
          headerClassName="verbal-endings-header"
          body={padaBody}
          sortable
        />
        <Column
          field="notes"
          header={t('verbalEndings.notes')}
          headerClassName="verbal-endings-header"
          body={notesBody}
        />
      </DataTable>
    </div>
  );
};

export default VerbalEndingsTable;