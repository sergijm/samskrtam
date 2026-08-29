import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { LexiconPos } from '../../types/lexicon';
import { LexiconSectionHeader } from './LexiconSectionHeader';
import { useLexiconLocale } from '../../hooks/useLexiconLocale';

interface LexiconPosProps {
  pos: LexiconPos[];
}

/** «Части речи» — компактные чипы, отдельное измерение от «Тем». */
const LexiconPos: React.FC<LexiconPosProps> = ({ pos }) => {
  const navigate = useNavigate();
  const locale = useLexiconLocale();

  const openLesson = (posCode: string) => navigate(`/lessons/vocabulary/lex-pos-${posCode}`);

  return (
    <section className="mb-5">
      <LexiconSectionHeader
        titleKey="lexicon.posTitle"
        icon="pi-align-justify"
      />

      <div className="flex flex-wrap gap-2">
        {pos.map((part) => (
          <button
            key={part.id}
            type="button"
            className="lexicon-chip cursor-pointer"
            onClick={() => openLesson(part.id)}
          >
            <span>{locale({ ru: part.nameRu, en: part.nameEn })}</span>
            <span className="lexicon-chip-count">{part.wordCount}</span>
          </button>
        ))}
      </div>
    </section>
  );
};

export default React.memo(LexiconPos);
