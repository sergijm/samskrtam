import React from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from 'primereact/button';
import { Badge } from 'primereact/badge';
import { Tooltip } from 'primereact/tooltip';
import { useNavigate } from 'react-router-dom';
import type { LessonStatusSummary as LessonStatusSummaryType } from '../../types/lesson';

interface LessonStatsBadgesProps {
  statusSummary: LessonStatusSummaryType | null | undefined;
  /** Путь квиза для навигации (e.g., '/quiz/vocabulary/:slug' or '/quiz/grammar/:type') */
  quizPath: string;
}

export const LessonStatsBadges = ({ statusSummary, quizPath }: LessonStatsBadgesProps) => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const isRu = i18n.language === 'ru';

  if (!statusSummary) {
    return null;
  }

  const { total, newCount, learning, mastered, reviewDue } = statusSummary;

  const handleBadgeClick = (statusFilter: string) => {
    const url = `${quizPath}?statusFilter=${statusFilter}`;
    navigate(url);
  };

  const masteredDisabled = reviewDue <= 0;
  const newDisabled = newCount <= 0;
  const learningDisabled = learning <= 0;

  return (
      <div className="flex flex-wrap gap-3 align-items-center">
        {/* Изучено / REVIEW */}
        <Tooltip target=".stats-badge-mastered" />
        <Button
          className={`stats-badge-mastered p-button-text p-2 ${masteredDisabled ? 'p-disabled' : ''}`}
          data-pr-tooltip={isRu
            ? `Повторить изученное (${reviewDue} ${reviewDue === 1 ? 'элемент' : reviewDue > 1 && reviewDue < 5 ? 'элемента' : 'элементов'})`
            : `Review mastered (${reviewDue} ${reviewDue === 1 ? 'item' : 'items'})`
          }
          disabled={masteredDisabled}
          onClick={() => handleBadgeClick('REVIEW')}
        >
          <Badge value={`${mastered}/${total}`} severity={masteredDisabled ? 'secondary' : 'success'} size="large" />
          <span className="ml-2 text-sm">{isRu ? 'Изучено' : 'Mastered'}</span>
        </Button>

        {/* Новые / NEW */}
        <Tooltip target=".stats-badge-new" />
        <Button
          className={`stats-badge-new p-button-text p-2 ${newDisabled ? 'p-disabled' : ''}`}
          data-pr-tooltip={isRu
            ? `Новые слова (${newCount} ${newCount === 1 ? 'элемент' : newCount > 1 && newCount < 5 ? 'элемента' : 'элементов'})`
            : `New words (${newCount})`
          }
          disabled={newDisabled}
          onClick={() => handleBadgeClick('NEW')}
        >
          <Badge value={newCount} severity={newDisabled ? 'secondary' : 'warning'} size="large" />
          <span className="ml-2 text-sm">{isRu ? 'Новые' : 'New'}</span>
        </Button>

        {/* В процессе / LEARNING */}
        <Tooltip target=".stats-badge-learning" />
        <Button
          className={`stats-badge-learning p-button-text p-2 ${learningDisabled ? 'p-disabled' : ''}`}
          data-pr-tooltip={isRu
            ? `В процессе изучения (${learning})`
            : `In progress (${learning})`
          }
          disabled={learningDisabled}
          onClick={() => handleBadgeClick('LEARNING')}
        >
          <Badge value={learning} severity={learningDisabled ? 'secondary' : 'info'} size="large" />
          <span className="ml-2 text-sm">{isRu ? 'В процессе' : 'Learning'}</span>
        </Button>
      </div>
    );
};

