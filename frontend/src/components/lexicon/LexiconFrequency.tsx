import React from 'react';
import { useTranslation } from 'react-i18next';
import { ProgressBar } from 'primereact/progressbar';
import { Toast } from 'primereact/toast';
import { FrequencyBand } from '../../types/lexicon';
import { LexiconSectionHeader } from './LexiconSectionHeader';
import { useLexiconToast } from './useLexiconToast';

interface LexiconFrequencyProps {
  bands: FrequencyBand[];
}

/** «Частотность» — компактные диапазоны от самого употребительного к редкому. */
const LexiconFrequency: React.FC<LexiconFrequencyProps> = ({ bands }) => {
  const { t } = useTranslation();
  const { toast, showComingSoon } = useLexiconToast();

  const overview = bands.slice(0, 3).reduce(
    (acc, band) => ({
      wordCount: acc.wordCount + band.wordCount,
      masteredCount: acc.masteredCount + band.masteredCount,
    }),
    { wordCount: 0, masteredCount: 0 },
  );
  const overviewPercent = overview.wordCount
    ? Math.round((overview.masteredCount / overview.wordCount) * 100)
    : 0;

  const bandLabel = (band: FrequencyBand) => `${band.from}–${band.to}`;

  const percent = (band: FrequencyBand) =>
    band.wordCount ? Math.round((band.masteredCount / band.wordCount) * 100) : 0;

  return (
    <>
      <Toast ref={toast} />
      <section className="mb-5">
        <LexiconSectionHeader
          titleKey="lexicon.frequencyTitle"
          subtitleKey="lexicon.frequencySubtitle"
          icon="pi-chart-line"
        />

        {/* Обзорный компактный блок */}
        <div className="lexicon-card lexicon-band-overview mb-3" onClick={() => showComingSoon()}>
          <div className="flex align-items-center justify-content-between gap-3">
            <div className="flex flex-column gap-1">
              <span className="font-bold">{t('lexicon.frequencyOverview')}</span>
              <span className="text-sm text-500">
                {overview.masteredCount} {t('lexicon.mastered').toLowerCase()} ·{' '}
                {overview.wordCount - overview.masteredCount} {t('lexicon.frequencyOverviewInProgress')}
              </span>
              <ProgressBar value={overviewPercent} style={{ height: '0.5rem', maxWidth: '320px' }} />
            </div>
            <i className="pi pi-arrow-right text-primary" />
          </div>
        </div>

        {/* Диапазоны */}
        <div className="grid">
          {bands.map((band) => (
            <div key={band.id} className="col-12 md:col-6 xl:col-4">
              <div
                className="lexicon-card lexicon-band-card h-full cursor-pointer"
                onClick={() => showComingSoon()}
              >
                <div className="flex align-items-center justify-content-between mb-1">
                  <span className="font-semibold">{bandLabel(band)}</span>
                  <span className="text-sm text-500">
                    {band.masteredCount} / {band.wordCount}
                  </span>
                </div>
                <ProgressBar value={percent(band)} style={{ height: '0.4rem' }} />
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  );
};

export default React.memo(LexiconFrequency);
