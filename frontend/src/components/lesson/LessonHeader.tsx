import React from 'react';

interface LessonHeaderProps {
  title: string;
  titleEn: string;
}

export const LessonHeader = ({ 
  title, 
  titleEn, 
}: LessonHeaderProps) => {
  return (
    <div>
      <h2 className="m-0">{title}</h2>
      <p className="text-sm text-color-secondary m-0">{titleEn}</p>
    </div>
  );
};