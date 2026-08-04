import React from 'react';
import { useTranslation } from 'react-i18next';
import { Toast } from 'primereact/toast';
import { QuickStartPreset } from '../../types/lexicon';
import { useLexiconToast } from './useLexiconToast';

interface LexiconQuickStartProps {
  presets: QuickStartPreset[];
}

/** «Быстрый старт» — готовые тренировки, чтобы начать сразу. */
const LexiconQuickStart: React.FC<LexiconQuickStartProps> = ({ presets }) => {
  const { t } = useTranslation();
  const { toast, showComingSoon } = useLexiconToast();

  return (
    <>
      <Toast ref={toast} />
      <div className="mb-4">
        <div className="flex align-items-center gap-2 mb-3">
          <i className="pi pi-bolt text-lg text-primary" />
          <h2 className="m-0 text-xl">{t('lexicon.quickStartTitle')}</h2>
          <span className="text-sm text-500 ml-2">{t('lexicon.quickStartSubtitle')}</span>
        </div>
        <div className="grid">
          {presets.map((preset) => (
            <div key={preset.id} className="col-12 sm:col-6 lg:col-4">
              <div className="lexicon-card lexicon-quick-card h-full flex flex-column gap-2">
                <div className="flex align-items-center gap-2">
                  <i className="pi pi-play-circle text-2xl text-primary" />
                  <span className="font-bold text-lg">{t(preset.titleKey)}</span>
                </div>
                <span className="text-sm text-500">{t(preset.metaKey)}</span>
                <button
                  type="button"
                  className="lexicon-link-btn align-self-start mt-auto"
                  onClick={() => showComingSoon()}
                >
                  {t('lexicon.start')}
                  <i className="pi pi-arrow-right" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </>
  );
};

export default React.memo(LexiconQuickStart);
