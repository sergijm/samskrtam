import React from 'react';
import { useTranslation } from 'react-i18next';
import { ProgressBar } from 'primereact/progressbar';
import { Toast } from 'primereact/toast';
import { LexicalSource } from '../../types/lexicon';
import { LexiconSectionHeader } from './LexiconSectionHeader';
import { useLexiconToast } from './useLexiconToast';

interface LexiconSourcesProps {
  sources: LexicalSource[];
}

/** «Лексика по текстам» — уникальные слова в произведениях. */
const LexiconSources: React.FC<LexiconSourcesProps> = ({ sources }) => {
  const { t } = useTranslation();
  const { toast, showComingSoon } = useLexiconToast();

  const percent = (source: LexicalSource) =>
    source.wordCount ? Math.round((source.masteredCount / source.wordCount) * 100) : 0;

  return (
    <>
      <Toast ref={toast} />
      <section className="mb-5">
        <LexiconSectionHeader
          titleKey="lexicon.sourcesTitle"
          subtitleKey="lexicon.sourcesSubtitle"
          icon="pi-book"
        />

        <div className="grid">
          {sources.map((source) => (
            <div key={source.id} className="col-12 md:col-6 xl:col-4">
              <div
                className="lexicon-card lexicon-source-card h-full flex flex-column cursor-pointer"
                onClick={() => showComingSoon()}
              >
                <div className="flex align-items-start justify-content-between gap-2">
                  <div>
                    {source.devanagari && (
                      <div className="lexicon-source-devanagari">{source.devanagari}</div>
                    )}
                    <div className="font-semibold text-lg">{source.title}</div>
                  </div>
                  <i className="pi pi-arrow-right text-primary" />
                </div>

                <div className="text-sm text-500 mt-2">
                  {source.wordCount} {t('lexicon.uniqueWords')} · {percent(source)}% {t('lexicon.mastered').toLowerCase()}
                </div>
                <ProgressBar value={percent(source)} style={{ height: '0.4rem', marginTop: '0.5rem' }} />

                <button
                  type="button"
                  className="lexicon-link-btn align-self-start mt-auto pt-3"
                  onClick={(e) => {
                    e.stopPropagation();
                    showComingSoon();
                  }}
                >
                  {t('lexicon.study')}
                  <i className="pi pi-arrow-right" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  );
};

export default React.memo(LexiconSources);
