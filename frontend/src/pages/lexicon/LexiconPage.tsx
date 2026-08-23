import React from 'react';
import { useTranslation } from 'react-i18next';
import { ProgressSpinner } from 'primereact/progressspinner';
import { Message } from 'primereact/message';
import { useLexiconDashboard } from '../../hooks/useLexicon';
import LexiconHero from '../../components/lexicon/LexiconHero';
import LexiconToday from '../../components/lexicon/LexiconToday';
import LexiconFrequency from '../../components/lexicon/LexiconFrequency';
import LexiconTopics from '../../components/lexicon/LexiconTopics';
import LexiconPos from '../../components/lexicon/LexiconPos';
import LexiconCollections from '../../components/lexicon/LexiconCollections';
import ProgressSummaryPanel from '../../components/dashboard/ProgressSummaryPanel';

/**
 * LexiconPage — стартовая страница «Лексика» (learning dashboard).
 *
 * Данные приходят из LexiconService (сейчас mock, позже — API).
 * Иерархия: Hero → Сегодня → Частотность → Темы → Части речи →
 * Произведения → Мои списки.
 */
const LexiconPage: React.FC = () => {
  const { t } = useTranslation();
  const { data, isLoading, isError } = useLexiconDashboard();

  if (isLoading) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <ProgressSpinner />
      </div>
    );
  }

  if (isError || !data) {
    return (
      <div className="flex justify-content-center align-items-center min-h-screen">
        <Message severity="error" text={t('lexicon.underConstruction')} />
      </div>
    );
  }

  return (
    <div className="lexicon-page p-3 md:p-4">
      <LexiconHero summary={data.summary} />
      <div className="mb-4">
        <ProgressSummaryPanel scope="lexicon" />
      </div>
      <LexiconToday today={data.today} />

      <LexiconFrequency bands={data.frequencyBands} />
      <LexiconTopics topics={data.topics} />
      <LexiconPos pos={data.pos} />
      <LexiconCollections collections={data.collections} />
    </div>
  );
};

export default LexiconPage;
