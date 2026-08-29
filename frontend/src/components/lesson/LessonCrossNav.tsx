import React from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { useDeclensionLessons } from '../../hooks/useQuiz';

export const DECLENSION_CROSS_LINK_SLUGS: string[] = [
  'a-stem',
  'i-u-stems',
  'r-stems',
  'ii-uu-stems',
  'irregular-stems-declension',
  'case-endings',
];

interface LessonCrossNavProps {
  currentSlug: string;
}

export const LessonCrossNav = ({ currentSlug }: LessonCrossNavProps) => {
  const { i18n } = useTranslation();
  const navigate = useNavigate();
  const isRu = i18n.language === 'ru';
  const { data: lessons = [] } = useDeclensionLessons();

  const links = DECLENSION_CROSS_LINK_SLUGS
    .filter((slug) => slug !== currentSlug)
    .map((slug) => {
      const lesson = lessons.find((l) => l.slug === slug);
      const title = lesson
        ? isRu
          ? lesson.titleRu
          : lesson.titleEn
        : slug;
      return { slug, title };
    });

  if (links.length === 0) return null;

  return (
    <div className="flex flex-wrap align-items-center gap-2 text-sm text-500">
      {links.map((link, idx) => (
        <React.Fragment key={link.slug}>
          {idx > 0 && <span className="text-400">·</span>}
          <a
            className="cursor-pointer text-500 hover:text-primary hover:underline"
            onClick={() => navigate(`/lessons/grammar/${link.slug}`)}
          >
            {link.title}
          </a>
        </React.Fragment>
      ))}
    </div>
  );
};

export default LessonCrossNav;
