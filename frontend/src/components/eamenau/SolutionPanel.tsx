import React from 'react';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { SolutionDto, SandhiRuleInfo } from '../../types';
import { contentApi } from '../../api/contentApi';
import { useNavigate } from 'react-router-dom';
import { Tooltip } from 'primereact/tooltip';

interface SolutionPanelProps {
  taskId: number;
}

const SolutionPanel: React.FC<SolutionPanelProps> = ({ taskId }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const { data: solutions, isLoading, error } = useQuery<SolutionDto[], Error>({
    queryKey: ['solution', taskId],
    queryFn: () => contentApi.getSolutionsForTask(taskId).then(res => res.data),
    enabled: !!taskId,
  });

  if (isLoading) {
    return <ProgressSpinner style={{ width: '50px', height: '50px' }} />;
  }

  if (error) {
    return <div>{t('quiz.fetchError', { message: error.message })}</div>;
  }

  if (!solutions || solutions.length === 0) {
    return <div>{t('eamenau.noSolutionFound')}</div>;
  }

  const handleRulesClick = (rules: SandhiRuleInfo[]) => {
    if (rules && rules.length > 0) {
      const queryParams = rules.map(rule => `rule=${rule.ruleNumber}`).join('&');
      navigate(`/grammar/emeneau-rules?${queryParams}`);
    }
  };

  return (
    <div className="p-3">
      {solutions.map((solution, index) => (
        <div key={solution.id} className={index > 0 ? "mt-4" : ""}>
          <p><strong>{solution.solutionText}</strong></p>
          {solution.stepByStep && <p>{solution.stepByStep}</p>}
          {solution.sandhiRules && solution.sandhiRules.length > 0 && (
            <p className="cursor-pointer" onClick={() => handleRulesClick(solution.sandhiRules)}>
              {t('eamenau.rules')}:{' '}
              {solution.sandhiRules.map((rule: SandhiRuleInfo, ruleIndex: number) => (
                <React.Fragment key={rule.ruleNumber}>
                  <span
                    className="p-link rule-number-link" // Added rule-number-link class
                    data-pr-tooltip={rule.shortDescription}
                    data-pr-position="top"
                  >
                    {rule.ruleNumber}
                  </span>
                  {ruleIndex < solution.sandhiRules.length - 1 && ', '}
                  <Tooltip target={`.rule-number-link[data-pr-tooltip="${rule.shortDescription}"]`} />
                </React.Fragment>
              ))}
            </p>
          )}
        </div>
      ))}
    </div>
  );
};

export default SolutionPanel;
