import React from 'react';

interface LessonHeaderProps {
  title: string;
  titleEn?: string;
  subtitle?: React.ReactNode;
}

export const LessonHeader = ({ 
  title, 
  titleEn, 
  subtitle, 
}: LessonHeaderProps) => {
  return (
    <div>
      <h2 className="m-0">{title}</h2>
      {subtitle !== undefined ? (
        <div className="text-sm text-color-secondary m-0">{subtitle}</div>
      ) : (
        <p className="text-sm text-color-secondary m-0">{titleEn}</p>
      )}
    </div>
  );
};