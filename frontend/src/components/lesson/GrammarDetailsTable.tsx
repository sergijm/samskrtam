import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { ProgressBar } from 'primereact/progressbar';
import { statusToProgressColor } from '../../utils/statusColor';
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
        header={i18n.language === 'ru' ? 'Окончание' : 'Ending'}
        body={(rowData: GrammarQuestionProgress) => (
          <span className="font-bold">{rowData.caseEnding ?? '-'}</span>
        )}
        style={{ width: '15%' }}
      />
      <Column
        header={i18n.language === 'ru' ? 'Изучено' : 'Learned'}
        body={(rowData: GrammarQuestionProgress) => (
          <div className="flex align-items-center gap-2">
            <ProgressBar
              value={rowData.score}
              color={statusToProgressColor(rowData.status)}
              style={{ height: '5px', width: '80px' }}
              showValue={false}
            />
            <span
              className="cursor-pointer underline text-primary"
              onClick={() => navigate(buildFilterUrl(rowData))}
            >
              {rowData.score}%
            </span>
          </div>
        )}
        style={{ width: '18%' }}
        sortable
        sortField="score"
        onSort={() => onSort('score')}
      />
    </DataTable>
  );
};

