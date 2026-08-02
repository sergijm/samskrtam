import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { MiniProgressBar } from '../common/MiniProgressBar';
import type {
  CaseAggregation,
  CaseNumberAggregation,
  NumberAggregation,
} from '../../utils/grammarAggregation';

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
  const navigate = useNavigate();

  const cellByKey = new Map<string, CaseNumberAggregation>();
  for (const agg of aggregations) {
    cellByKey.set(`${agg.caseType}:${agg.numberType}`, agg);
  }

  const handleCaseClick = (caseType: string) => {
    navigate(`/quiz/grammar/${quizSlug}?filterScope=CASE_ONLY&filterCaseType=${caseType}`);
  };

  const handleNumberClick = (numberType: string) => {
    navigate(`/quiz/grammar/${quizSlug}?filterScope=NUMBER_ONLY&filterNumberTypes=${numberType}`);
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
                className="p-2 border-bottom-1 border-100 cursor-pointer hover:surface-100 transition-colors"
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
                      className="text-center p-2 border-bottom-1 border-100 text-color-secondary"
                    >
                      —
                    </td>
                  );
                }
                return (
                  <td key={num.numberType} className="text-center p-2 border-bottom-1 border-100">
                    <MiniProgressBar value={agg.aggregatedProgress} status={agg.status} />
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
