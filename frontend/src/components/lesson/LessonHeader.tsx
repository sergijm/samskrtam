import React from 'react';
import { ProgressBar } from 'primereact/progressbar';
import { Tag } from 'primereact/tag';
import { Button } from 'primereact/button';

interface LessonHeaderProps {
  title: string;
  titleEn: string;
  difficulty: string;
  progress: number;
  total: number;
  learned: number;
}

export const LessonHeader = ({ 
  title, 
  titleEn, 
  difficulty, 
  progress, 
  total, 
  learned 
}: LessonHeaderProps) => {
  return (
    <div className="card">
      <div className="flex flex-column md:flex-row md:justify-content-between md:align-items-center mb-3">
        <div>
          <h2 className="m-0">{title}</h2>
          <p className="text-sm text-color-secondary m-0">{titleEn}</p>
        </div>
        <Tag value={difficulty} className="mt-2 md:mt-0" />
      </div>
      
      <div className="mb-3">
        <div className="flex justify-content-between mb-1">
          <span>Прогресс</span>
          <span>{learned} из {total}</span>
        </div>
        <ProgressBar value={progress} showValue={false} />
      </div>
      
      <div className="text-sm text-color-secondary">
        Изучено: {progress}% ({learned} {learned === 1 ? 'слово' : 'слов'})
      </div>
    </div>
  );
};