import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Toast } from 'primereact/toast';
import { Dialog } from 'primereact/dialog';
import { Button } from 'primereact/button';
import { UserCollection } from '../../types/lexicon';
import { LexiconSectionHeader } from './LexiconSectionHeader';
import { useLexiconToast } from './useLexiconToast';

interface LexiconCollectionsProps {
  collections: UserCollection[];
}

/** «Мои списки» — собственные наборы слов + mock-диалог создания. */
const LexiconCollections: React.FC<LexiconCollectionsProps> = ({ collections }) => {
  const { t } = useTranslation();
  const { toast, showComingSoon } = useLexiconToast();
  const [dialogVisible, setDialogVisible] = useState(false);

  return (
    <>
      <Toast ref={toast} />
      <Dialog
        visible={dialogVisible}
        onHide={() => setDialogVisible(false)}
        header={t('lexicon.createListDialogTitle')}
        footer={
          <Button label={t('lexicon.createListDialogOk')} onClick={() => setDialogVisible(false)} />
        }
        className="lexicon-create-dialog"
      >
        <p className="m-0 text-500">{t('lexicon.createListDialogDesc')}</p>
      </Dialog>

      <section className="mb-5">
        <LexiconSectionHeader
          titleKey="lexicon.collectionsTitle"
          subtitleKey="lexicon.collectionsSubtitle"
          icon="pi-star"
          action={
            <Button
              label={t('lexicon.createListCta')}
              icon="pi pi-plus"
              size="small"
              outlined
              onClick={() => setDialogVisible(true)}
            />
          }
        />

        <div className="grid">
          {collections.map((collection) => (
            <div key={collection.id} className="col-12 sm:col-6 lg:col-4">
              <div
                className="lexicon-card lexicon-collection-card h-full flex align-items-center justify-content-between gap-2 cursor-pointer"
                onClick={() => showComingSoon()}
              >
                <div className="flex align-items-center gap-3">
                  <i className="pi pi-star-fill text-primary" />
                  <div className="flex flex-column">
                    <span className="font-semibold">{collection.name}</span>
                    <span className="text-sm text-500">{collection.wordCount} {t('lexicon.words')}</span>
                  </div>
                </div>
                <i className="pi pi-arrow-right text-color-secondary" />
              </div>
            </div>
          ))}
        </div>

        <div className="mt-3">
          <button type="button" className="lexicon-link-btn" onClick={() => showComingSoon()}>
            {t('lexicon.allListsCta')}
            <i className="pi pi-arrow-right" />
          </button>
        </div>
      </section>
    </>
  );
};

export default React.memo(LexiconCollections);
