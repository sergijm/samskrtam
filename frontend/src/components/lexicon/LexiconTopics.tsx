import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { ProgressBar } from 'primereact/progressbar';
import { Toast } from 'primereact/toast';
import { LexicalTopic } from '../../types/lexicon';
import { LexiconSectionHeader } from './LexiconSectionHeader';
import { useLexiconToast } from './useLexiconToast';
import { useLexiconLocale } from '../../hooks/useLexiconLocale';

interface LexiconTopicsProps {
  topics: LexicalTopic[];
}

const INITIAL_VISIBLE = 9;
const EXPANDED_STORAGE_KEY = 'lexicon_topics_expanded';

/** «Темы» — семантические группы слов. Показываем 9, остальные по «Все темы →». */
const LexiconTopics: React.FC<LexiconTopicsProps> = ({ topics }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { toast } = useLexiconToast();
  const locale = useLexiconLocale();
  const [showAll, setShowAll] = useState(
    () => localStorage.getItem(EXPANDED_STORAGE_KEY) === '1',
  );

  const toggleAll = () => {
    setShowAll((prev) => {
      const next = !prev;
      localStorage.setItem(EXPANDED_STORAGE_KEY, next ? '1' : '0');
      return next;
    });
  };

  const visible = showAll ? topics : topics.slice(0, INITIAL_VISIBLE);

  const percent = (topic: LexicalTopic) =>
    topic.wordCount ? Math.round((topic.masteredCount / topic.wordCount) * 100) : 0;

  return (
    <>
      <Toast ref={toast} />
      <section className="mb-5">
        <LexiconSectionHeader
          titleKey="lexicon.topicsTitle"
          icon="pi-th-large"
        />

        <div className="grid">
          {visible.map((topic) => (
            <div key={topic.id} className="col-12 sm:col-6 lg:col-4 xl:col-3">
              <div
                className="lexicon-card lexicon-topic-card h-full cursor-pointer"
                onClick={() => navigate(`/lessons/vocabulary/${topic.id}`)}
              >
                <div className="flex align-items-center justify-content-between gap-2">
                  <span className="font-semibold">{locale({ ru: topic.nameRu, en: topic.nameEn })}</span>
                  <span className="text-sm text-500">
                    {topic.wordCount} {t('lexicon.words')}
                  </span>
                </div>
                <div className="text-sm text-500 mt-1 mb-2">
                  {topic.masteredCount} {t('lexicon.mastered').toLowerCase()}
                </div>
                <ProgressBar value={percent(topic)} style={{ height: '0.4rem' }} />
              </div>
            </div>
          ))}
        </div>

        <div className="flex justify-content-center mt-3">
          <button
            type="button"
            className="lexicon-link-btn"
            onClick={toggleAll}
          >
            {showAll ? t('lexicon.showLessCta') : t('lexicon.allTopicsCta')}
            <i className={`pi ${showAll ? 'pi-chevron-up' : 'pi-chevron-down'}`} />
          </button>
        </div>
      </section>
    </>
  );
};

export default React.memo(LexiconTopics);
