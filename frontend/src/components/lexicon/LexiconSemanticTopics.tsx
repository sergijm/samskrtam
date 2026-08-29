import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';
import { ProgressBar } from 'primereact/progressbar';
import { SemanticTopic } from '../../types/lexicon';
import { LexiconSectionHeader } from './LexiconSectionHeader';
import { useLexiconLocale } from '../../hooks/useLexiconLocale';

interface LexiconSemanticTopicsProps {
  topics: SemanticTopic[];
}

const INITIAL_VISIBLE = 9;
const EXPANDED_STORAGE_KEY = 'lexicon_semantic_topics_expanded';

/**
 * «Семантические темы» — отдельная группа плиток, где леммы привязаны к топику
 * через цепочку lemma_semantic_class → semantic_class_topic (в отличие от
 * «Тем», где привязка идёт напрямую через lexeme_lexical_topic).
 */
const LexiconSemanticTopics: React.FC<LexiconSemanticTopicsProps> = ({ topics }) => {
  const { t } = useTranslation();
  const navigate = useNavigate();
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

  const percent = (topic: SemanticTopic) =>
    topic.wordCount ? Math.round((topic.masteredCount / topic.wordCount) * 100) : 0;

  if (topics.length === 0) {
    return null;
  }

  return (
    <section className="mb-5">
      <LexiconSectionHeader
        titleKey="lexicon.semanticTopicsTitle"
        subtitleKey="lexicon.semanticTopicsSubtitle"
        icon="pi-sitemap"
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
          {showAll ? t('lexicon.showLessCta') : t('lexicon.allSemanticTopicsCta')}
          <i className={`pi ${showAll ? 'pi-chevron-up' : 'pi-chevron-down'}`} />
        </button>
      </div>
    </section>
  );
};

export default React.memo(LexiconSemanticTopics);
