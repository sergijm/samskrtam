import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { ProgressBar } from 'primereact/progressbar';
import { Button } from 'primereact/button';

interface TagSetProgressItem {
  setId: string;
  labelRu: string;
  labelEn: string;
  aggregatedProgress: number;
  totalCombinations: number;
  learnedCombinations: number;
  status: string;
}

interface Props {
  tagSets: TagSetProgressItem[];
  quizSlug: string;
}

const GrammarProgressTagSets: React.FC<Props> = ({ tagSets, quizSlug }) => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();

  const color = (status: string) => {
    switch (status) {
      case 'MASTERED': return 'var(--green-500)';
      case 'LEARNING': return 'var(--blue-500)';
      default: return 'var(--gray-400)';
    }
  };

  if (tagSets.length === 0) return null;

  return (
    <div className="mt-4">
      <h4 className="mb-3">
        {i18n.language === 'ru' ? 'Дополнительные разрезы' : 'Additional slices'}
      </h4>
      <div className="grid">
        {tagSets.map(ts => (
          <div key={ts.setId} className="col-12 md:col-6 lg:col-4">
            <div className="card p-3 flex align-items-center gap-3">
              <div className="flex-1">
                <div className="text-sm font-medium mb-2">
                  {i18n.language === 'ru' ? ts.labelRu : ts.labelEn}
                </div>
                <ProgressBar
                  value={ts.aggregatedProgress}
                  color={color(ts.status)}
                  style={{ height: '6px' }}
                  showValue={false}
                />
                <div className="text-xs text-color-secondary mt-1">
                  {ts.learnedCombinations}/{ts.totalCombinations}
                </div>
              </div>
              <Button
                icon="pi pi-angle-double-right"
                severity="secondary"
                text
                rounded
                tooltip={i18n.language === 'ru' ? 'Квиз' : 'Quiz'}
                tooltipOptions={{ position: 'top' }}
                onClick={() => navigate(`/quiz/grammar/${quizSlug}?progressTagSetId=${ts.setId}`)}
              />
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default GrammarProgressTagSets;