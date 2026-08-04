import React from 'react';
import { useTranslation } from 'react-i18next';
import { ProgressBar } from 'primereact/progressbar';
import { Toast } from 'primereact/toast';
import { LexiconProgressSummary } from '../../types/lexicon';
import { useLexiconToast } from './useLexiconToast';

interface LexiconHeroProps {
  summary: LexiconProgressSummary;
}

/** Верхняя часть: заголовок, подзаголовок, CTA и общий прогресс. */
const LexiconHero: React.FC<LexiconHeroProps> = ({ summary }) => {
  const { t } = useTranslation();
  const { toast, showComingSoon } = useLexiconToast();

  const percent = summary.totalWords
    ? Math.round((summary.masteredCount / summary.totalWords) * 100)
    : 0;
  const hasStarted = summary.masteredCount > 0;

  return (
    <>
      <Toast ref={toast} />
      <div className="lexicon-hero mb-5">
        <div className="flex flex-column md:flex-row md:align-items-end md:justify-content-between gap-3">
          <div>
            <h1 className="m-0 text-3xl">{t('lexicon.title')}</h1>
            <p className="m-0 text-500 mt-1">{t('lexicon.subtitle')}</p>
          </div>
          <button type="button" className="lexicon-hero-cta" onClick={() => showComingSoon()}>
            {hasStarted ? t('lexicon.continueCta') : t('lexicon.startCta')}
            <i className="pi pi-arrow-right" />
          </button>
        </div>

        <div className="lexicon-hero-stats mt-4">
          <div className="flex justify-content-between align-items-center mb-1">
            <span className="font-semibold">
              {summary.masteredCount} / {summary.totalWords} {t('lexicon.words')}
            </span>
            <span className="font-semibold text-primary">
              {percent}% {t('lexicon.mastered')}
            </span>
          </div>
          <ProgressBar value={percent} style={{ height: '0.75rem' }} />
        </div>
      </div>
    </>
  );
};

export default React.memo(LexiconHero);
