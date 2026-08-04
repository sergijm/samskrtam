import React from 'react';
import { useTranslation } from 'react-i18next';
import { Toast } from 'primereact/toast';
import { PartOfSpeech } from '../../types/lexicon';
import { LexiconSectionHeader } from './LexiconSectionHeader';
import { useLexiconToast } from './useLexiconToast';

interface LexiconPosProps {
  pos: PartOfSpeech[];
}

/** «Части речи» — компактные чипы, отдельное измерение от «Тем». */
const LexiconPos: React.FC<LexiconPosProps> = ({ pos }) => {
  const { t } = useTranslation();
  const { toast, showComingSoon } = useLexiconToast();

  return (
    <>
      <Toast ref={toast} />
      <section className="mb-5">
        <LexiconSectionHeader
          titleKey="lexicon.posTitle"
          subtitleKey="lexicon.posSubtitle"
          icon="pi-align-justify"
        />

        <div className="flex flex-wrap gap-2">
          {pos.map((part) => (
            <button
              key={part.id}
              type="button"
              className="lexicon-chip cursor-pointer"
              onClick={() => showComingSoon()}
            >
              <span>{t(part.nameKey)}</span>
              <span className="lexicon-chip-count">{part.wordCount}</span>
            </button>
          ))}
        </div>

        <div className="mt-3">
          <button type="button" className="lexicon-link-btn" onClick={() => showComingSoon()}>
            {t('lexicon.allPosCta')}
            <i className="pi pi-arrow-right" />
          </button>
        </div>
      </section>
    </>
  );
};

export default React.memo(LexiconPos);
