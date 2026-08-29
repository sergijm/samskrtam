import React from 'react';
import { useTranslation } from 'react-i18next';
import { MiniProgressBar } from '../common/MiniProgressBar';
import type {
  CaseAggregation,
  CaseNumberAggregation,
  NumberAggregation,
} from '../../types/lesson';

interface GrammarProgressGridProps {
  aggregations: CaseNumberAggregation[];
  caseNames: CaseAggregation[];
  numberNames: NumberAggregation[];
  quizSlug: string;
}

const GrammarProgressGrid: React.FC<GrammarProgressGridProps> = ({
  aggregations,
  caseNames,
  numberNames,
  quizSlug,
}) => {
  const { i18n } = useTranslation();

  if (caseNames.length === 0 && numberNames.length === 0) {
    return (
      <div className="text-center p-4 text-color-secondary">
        {i18n.language === 'ru'
          ? 'Данные о прогрессе отсутствуют'
          : 'No progress data available'}
      </div>
    );
  }

  const cellByKey = new Map<string, CaseNumberAggregation>();
  for (const agg of aggregations) {
    cellByKey.set(`${agg.caseType}:${agg.numberType}`, agg);
  }

  const handleCaseClick = (caseType: string) => {
    window.open(`/quiz/grammar/${quizSlug}?filterScope=CASE_ONLY&filterCaseType=${caseType}`, '_blank');
  };

  const handleNumberClick = (numberType: string) => {
    window.open(`/quiz/grammar/${quizSlug}?filterScope=NUMBER_ONLY&filterNumberTypes=${numberType}`, '_blank');
  };

  return (
    <div className="overflow-x-auto">
      <table className="w-full border-collapse text-sm">
        <thead>
          <tr>
            <th
              className="text-left p-2 border-bottom-1 border-200 font-semibold"
              style={{ width: '25%' }}
            />
            {numberNames.map(num => (
              <th
                key={num.numberType}
                className="text-center p-2 border-bottom-1 border-200 font-semibold cursor-pointer hover:surface-100 transition-colors"
                onClick={() => handleNumberClick(num.numberType)}
                title={i18n.language === 'ru' ? 'Начать квиз по этому числу' : 'Start quiz for this number'}
              >
                {i18n.language === 'ru' ? num.numberRu : num.numberEn}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {caseNames.map(caseItem => (
            <tr key={caseItem.caseType}>
              <td
                className="px-2 py-1 cursor-pointer hover:surface-100 transition-colors"
                onClick={() => handleCaseClick(caseItem.caseType)}
                title={i18n.language === 'ru' ? 'Начать квиз по этому падежу' : 'Start quiz for this case'}
              >
                {i18n.language === 'ru' ? caseItem.caseRu : caseItem.caseEn}
              </td>
              {numberNames.map(num => {
                const agg = cellByKey.get(`${caseItem.caseType}:${num.numberType}`);
                if (!agg) {
                  return (
                    <td
                      key={num.numberType}
                      className="text-center px-2 py-1 text-color-secondary"
                    >
                      —
                    </td>
                  );
                }
                return (
                  <td key={num.numberType} className="text-center px-2 py-1">
                    <MiniProgressBar
                      value={agg.aggregatedProgress}
                      status={agg.status}
                      width="110px"
                      className="justify-content-center"
                    />
                  </td>
                );
              })}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default GrammarProgressGrid;
