import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { MiniProgressBar } from '../common/MiniProgressBar';
import type { GrammarQuestionProgress } from '../../types/lesson';

interface GrammarDetailsTableProps {
  forms: GrammarQuestionProgress[];
  quizSlug: string;
  sortField: string;
  sortOrder: number;
  onSort: (field: string) => void;
}

export const GrammarDetailsTable: React.FC<GrammarDetailsTableProps> = ({
  forms,
  quizSlug,
  sortField,
  sortOrder,
  onSort,
}) => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();

  const buildFilterUrl = (row: GrammarQuestionProgress) =>
    `/quiz/grammar/${quizSlug}?filterScope=CASE_NUMBER_GENDER&filterCaseType=${row.caseType}&filterNumberType=${row.numberType}&filterGender=${row.gender}`;

  return (
    <DataTable value={forms} paginator rows={20} responsiveLayout="scroll">
      <Column
        header={i18n.language === 'ru' ? 'Падеж' : 'Case'}
        body={(rowData: GrammarQuestionProgress) => (
          <div>{i18n.language === 'ru' ? rowData.caseRu : rowData.caseEn}</div>
        )}
        style={{ width: '15%' }}
        sortable
        sortField="caseType"
        onSort={() => onSort('caseType')}
      />
      <Column
        header={i18n.language === 'ru' ? 'Число' : 'Number'}
        body={(rowData: GrammarQuestionProgress) =>
          i18n.language === 'ru' ? rowData.numberRu : rowData.numberEn
        }
        style={{ width: '12%' }}
        sortable
        sortField="numberType"
        onSort={() => onSort('numberType')}
      />
      <Column
        header={i18n.language === 'ru' ? 'Род' : 'Gender'}
        body={(rowData: GrammarQuestionProgress) =>
          i18n.language === 'ru' ? rowData.genderRu : rowData.genderEn
        }
        style={{ width: '12%' }}
        sortable
        sortField="gender"
        onSort={() => onSort('gender')}
      />
      <Column
        header={i18n.language === 'ru' ? 'Изучено' : 'Learned'}
        body={(rowData: GrammarQuestionProgress) => (
          <MiniProgressBar
            value={rowData.score}
            status={rowData.status}
            onClick={() => navigate(buildFilterUrl(rowData))}
          />
        )}
        style={{ width: '18%' }}
        sortable
        sortField="score"
        onSort={() => onSort('score')}
      />
    </DataTable>
  );
};

