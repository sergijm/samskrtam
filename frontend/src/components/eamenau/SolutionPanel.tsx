import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { SolutionDto, SandhiRuleInfo } from '../../types';
import { contentApi } from '../../api/contentApi';
import { useNavigate } from 'react-router-dom';
import { Tooltip } from 'primereact/tooltip';
import { Button } from 'primereact/button';
import { InputTextarea } from 'primereact/inputtextarea';
import { InputText } from 'primereact/inputtext';

interface SolutionPanelProps {
  taskId: number;
}

const SolutionPanel: React.FC<SolutionPanelProps> = ({ taskId }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  const [isEditing, setIsEditing] = useState<number | null>(null);
  const [editedStepByStep, setEditedStepByStep] = useState('');
  const [editedRules, setEditedRules] = useState('');

  const { data: solutions, isLoading, error } = useQuery<SolutionDto[], Error>({
    queryKey: ['solution', taskId],
    queryFn: () => contentApi.getSolutionsForTask(taskId).then(res => res.data),
    enabled: !!taskId,
  });

  const updateSolutionMutation = useMutation({
    mutationFn: (data: { solutionId: number; stepByStep: string; ruleNumbers: string }) => 
      contentApi.updateSolution(data.solutionId, data.stepByStep, data.ruleNumbers),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['solution', taskId] });
      setIsEditing(null);
    },
  });

  const handleEdit = (solution: SolutionDto) => {
    setIsEditing(solution.id);
    setEditedStepByStep(solution.stepByStep || '');
    setEditedRules(solution.sandhiRules.map(r => r.ruleNumber).join(', '));
  };

  const handleSave = (solutionId: number) => {
    updateSolutionMutation.mutate({ solutionId, stepByStep: editedStepByStep, ruleNumbers: editedRules });
  };

  const handleCancel = () => {
    setIsEditing(null);
  };

  const handleRulesClick = (rules: SandhiRuleInfo[]) => {
    if (rules && rules.length > 0) {
      const queryParams = rules.map(rule => `rule=${rule.ruleNumber}`).join('&');
      navigate(`/grammar/emeneau-rules?${queryParams}`);
    }
  };

  if (isLoading) {
    return <ProgressSpinner style={{ width: '50px', height: '50px' }} />;
  }

  if (error) {
    return <div>{t('quiz.fetchError', { message: error.message })}</div>;
  }

  if (!solutions || solutions.length === 0) {
    return <div>{t('eamenau.noSolutionFound')}</div>;
  }

  return (
    <div className="p-3">
      {solutions.map((solution, index) => (
        <div key={solution.id} className={index > 0 ? "mt-4" : ""}>
          <div className="flex justify-content-between align-items-start">
            <p><strong>{solution.solutionText}</strong></p>
            <div>
              {isEditing === solution.id ? (
                <>
                  <Button
                    label={t('common.save', 'Сохранить')}
                    className="p-button-link"
                    onClick={() => handleSave(solution.id)}
                    loading={updateSolutionMutation.isPending}
                  />
                  <Button
                    label={t('common.cancel', 'Отмена')}
                    className="p-button-link p-button-danger"
                    onClick={handleCancel}
                  />
                </>
              ) : (
                <Button
                  label={t('common.edit', 'Редактировать')}
                  className="p-button-link"
                  onClick={() => handleEdit(solution)}
                />
              )}
            </div>
          </div>

          {isEditing === solution.id ? (
            <div className="flex flex-column gap-2 mt-2">
              <InputTextarea
                value={editedStepByStep}
                onChange={(e) => setEditedStepByStep(e.target.value)}
                rows={5}
                autoResize
              />
              <InputText
                value={editedRules}
                onChange={(e) => setEditedRules(e.target.value)}
                placeholder={t('eamenau.ruleNumbersPlaceholder', 'Номера правил через запятую')}
              />
            </div>
          ) : (
            <>
              {solution.stepByStep && <p style={{ whiteSpace: 'pre-wrap' }}>{solution.stepByStep}</p>}
              {solution.sandhiRules && solution.sandhiRules.length > 0 && (
                <p className="cursor-pointer" onClick={() => handleRulesClick(solution.sandhiRules)}>
                  {t('eamenau.rules')}:{' '}
                  {solution.sandhiRules.map((rule: SandhiRuleInfo, ruleIndex: number) => (
                    <React.Fragment key={rule.ruleNumber}>
                      <span
                        className="p-link rule-number-link"
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
            </>
          )}
        </div>
      ))}
    </div>
  );
};

export default SolutionPanel;
