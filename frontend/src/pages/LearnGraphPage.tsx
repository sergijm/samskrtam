import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressBar } from 'primereact/progressbar';
import { Card } from 'primereact/card';
import {
  learnGraphLayers,
  topicLayerIndex,
  type LearnGraphLayer,
  type LearnGraphTopic,
  type TopicStatus,
  type TypeGroup,
} from '../config/learnGraph';

const TYPE_ICONS: Record<TypeGroup, string> = {
  vocabulary: 'pi-book',
  declension: 'pi-tag',
  sandhi: 'pi-link',
  conjugation: 'pi-sort-alt',
  syntax: 'pi-sitemap',
  other: 'pi-circle',
};

const TYPE_FILTERS: Array<TypeGroup | 'all'> = [
  'all',
  'vocabulary',
  'declension',
  'sandhi',
  'conjugation',
  'syntax',
  'other',
];

const STATUS_ICONS: Partial<Record<TopicStatus, string>> = {
  mastered: 'pi-check',
  in_progress: 'pi-spinner',
  recommended: 'pi-arrow-right',
  review: 'pi-refresh',
};

/**
 * LearnGraphPage — карта обучения (docs/services/curriculum.md §4).
 *
 * Слои 0–6 как сворачиваемые кластеры в топологическом порядке. Карточка темы
 * показывает состояние (освоено/в процессе/дальше/повторить — пока DEMO-данные),
 * короткое описание, тип, входящие зависимости и кнопку «Изучить →».
 * Фильтр по типу сверху, «Ваш путь» и CTA «Продолжить» над картой.
 */
const LearnGraphPage: React.FC = () => {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [expanded, setExpanded] = useState<Record<string, boolean>>({ l0: true, always: true });
  const [typeFilter, setTypeFilter] = useState<TypeGroup | 'all'>('all');
  const [highlightId, setHighlightId] = useState<string | null>(null);

  const topicLayer = useMemo(() => topicLayerIndex(learnGraphLayers), []);
  const allTopics = useMemo(() => learnGraphLayers.flatMap((l) => l.topics), []);

  const topicTitle = (id: string) => t(`learnGraph.topics.${id}`);
  const topicDesc = (id: string) => t(`learnGraph.topicDescs.${id}`);

  const toggle = (id: string) => setExpanded((prev) => ({ ...prev, [id]: !prev[id] }));

  const expandLayer = (layerId?: string) => {
    if (!layerId) return;
    setExpanded((prev) => ({ ...prev, [layerId]: true }));
  };

  const visibleTopics = (layer: LearnGraphLayer) =>
    layer.topics.filter((topic) => typeFilter === 'all' || topic.typeGroup === typeFilter);

  const openTopic = (topic: LearnGraphTopic) => navigate(topic.route ?? '/grammar');

  const statusText = (status?: TopicStatus) =>
    status && status !== 'available' ? t(`learnGraph.statuses.${status}`) : null;

  // ─── «Ваш путь» — агрегированный прогресс (DEMO-статусы) ───
  const masteredCount = allTopics.filter((tp) => tp.status === 'mastered').length;
  const inProgressCount = allTopics.filter((tp) => tp.status === 'in_progress').length;
  const recommendedCount = allTopics.filter((tp) => tp.status === 'recommended').length;
  const overallPercent = Math.round(
    allTopics.reduce((sum, tp) => {
      if (tp.status === 'mastered') return sum + 100;
      if (tp.status === 'in_progress') return sum + (tp.progressPercent ?? 50);
      if (tp.status === 'review') return sum + 50;
      return sum;
    }, 0) / Math.max(allTopics.length, 1),
  );

  const continueTopic = useMemo(
    () => allTopics.find((tp) => tp.status === 'in_progress') ?? allTopics.find((tp) => tp.status === 'recommended'),
    [allTopics],
  );
  const continueProgress =
    continueTopic?.status === 'mastered' ? 100 : (continueTopic?.progressPercent ?? 0);

  const renderTopic = (topic: LearnGraphTopic) => {
    const status = statusText(topic.status);
    const isInProgress = topic.status === 'in_progress';
    return (
      <div key={topic.id} className="col-12 md:col-6 xl:col-4">
        <div
          className={[
            'learn-graph-topic',
            highlightId === topic.id ? ' learn-graph-topic--highlighted' : '',
            topic.status === 'recommended' ? ' learn-graph-topic--recommended' : '',
          ].join('')}
        >
          <div className="flex align-items-start justify-content-between gap-2">
            <div className="flex align-items-center gap-2">
              <i
                className={`learn-graph-topic-icon pi ${TYPE_ICONS[topic.typeGroup]}`}
                title={t(`learnGraph.typeFilters.${topic.typeGroup}`)}
              />
              <span className="font-medium">{topicTitle(topic.id)}</span>
            </div>
            {status && (
              <span className={`learn-graph-status learn-graph-status--${topic.status}`}>
                {STATUS_ICONS[topic.status!] && <i className={`pi ${STATUS_ICONS[topic.status!]} learn-graph-status-icon`} />}
                {status}
              </span>
            )}
          </div>

          <div className="text-sm text-500 mt-1">{topicDesc(topic.id)}</div>

          {isInProgress && topic.progressPercent !== undefined && (
            <ProgressBar value={topic.progressPercent} className="learn-graph-topic-progress mt-2" />
          )}

          {topic.prerequisites && topic.prerequisites.length > 0 && (
            <div className="learn-graph-dep mt-2">
              <i className="pi pi-arrow-left learn-graph-dep-icon" />
              <span className="text-xs text-500">{t('learnGraph.dependsOn')}:</span>
              {topic.prerequisites.map((p) => (
                <button
                  key={p}
                  type="button"
                  className="learn-graph-prereq"
                  onMouseEnter={() => setHighlightId(p)}
                  onMouseLeave={() => setHighlightId(null)}
                  onClick={() => expandLayer(topicLayer[p])}
                >
                  {topicTitle(p)}
                </button>
              ))}
            </div>
          )}

          <div className="learn-graph-topic-actions">
            <button type="button" className="learn-graph-learn-btn" onClick={() => openTopic(topic)}>
              {isInProgress ? t('learnGraph.continueBtn') : t('learnGraph.studyBtn')}
              <i className="pi pi-arrow-right" />
            </button>
          </div>
        </div>
      </div>
    );
  };

  return (
    <div className="learn-graph-page p-3 md:p-4">
      <header className="mb-4">
        <div className="flex align-items-center gap-2 mb-1">
          <i className="pi pi-sitemap text-2xl text-primary" />
          <h1 className="m-0 text-2xl">{t('learnGraph.title')}</h1>
        </div>
        <p className="m-0 text-500">{t('learnGraph.subtitle')}</p>
      </header>

      {/* Ваш путь — §4: где я нахожусь */}
      <Card className="learn-graph-summary mb-4 fade-in">
        <div className="flex flex-column gap-2">
          <div className="flex justify-content-between align-items-center">
            <span className="font-medium">{t('learnGraph.progressTitle')}</span>
            <span className="learn-graph-overall">{overallPercent}%</span>
          </div>
          <ProgressBar value={overallPercent} style={{ height: '0.75rem' }} />
          <div className="flex flex-wrap gap-3 text-sm text-500">
            <span><b>{masteredCount}</b> {t('learnGraph.progressMastered')}</span>
            <span><b>{inProgressCount}</b> {t('learnGraph.progressInProgress')}</span>
            <span><b>{recommendedCount}</b> {t('learnGraph.progressAvailable')}</span>
          </div>
        </div>
      </Card>

      {/* CTA «Продолжить обучение» — §4: что делать сейчас */}
      {continueTopic && (
        <Card className="learn-graph-continue mb-4 fade-in">
          <div className="flex flex-column md:flex-row align-items-center justify-content-between gap-3">
            <div className="flex flex-column gap-2 flex-1 w-full">
              <span className="text-sm text-500 font-medium">{t('learnGraph.continueTitle')}</span>
              <div className="flex align-items-center flex-wrap gap-2">
                <span className="font-bold text-lg">{topicTitle(continueTopic.id)}</span>
                <span className="text-sm text-500">{topicDesc(continueTopic.id)}</span>
              </div>
              <div className="w-full" style={{ maxWidth: '400px' }}>
                <ProgressBar value={continueProgress} style={{ height: '0.75rem' }} />
              </div>
            </div>
            <button type="button" className="learn-graph-cta" onClick={() => openTopic(continueTopic)}>
              {continueTopic.status === 'in_progress' ? t('learnGraph.continueBtn') : t('learnGraph.studyBtn')}
              <i className="pi pi-arrow-right" />
            </button>
          </div>
        </Card>
      )}

      {/* Фильтр по типу */}
      <div className="learn-graph-filter flex flex-wrap align-items-center gap-2 mb-3">
        <span className="text-sm font-medium text-color-secondary mr-1">{t('learnGraph.filterTitle')}</span>
        {TYPE_FILTERS.map((group) => (
          <button
            key={group}
            type="button"
            className={`learn-graph-filter-btn${typeFilter === group ? ' active' : ''}`}
            onClick={() => setTypeFilter(group)}
          >
            {t(`learnGraph.typeFilters.${group}`)}
          </button>
        ))}
      </div>

      {/* Слои */}
      <div className="learn-graph">
        {learnGraphLayers.map((layer) => {
          const isOpen = !!expanded[layer.id];
          const topics = visibleTopics(layer);
          return (
            <div key={layer.id} className={`learn-graph-layer${layer.alwaysAvailable ? ' learn-graph-layer--always' : ''}`}>
              <button
                type="button"
                className="learn-graph-layer-header"
                onClick={() => toggle(layer.id)}
                aria-expanded={isOpen}
              >
                <div className="learn-graph-layer-number">
                  {layer.alwaysAvailable ? <i className="pi pi-infinity" /> : layer.id.replace('l', '')}
                </div>
                <div className="flex flex-column flex-1 text-left">
                  <span className="font-bold">{t(layer.titleKey)}</span>
                  <span className="text-sm text-500">{t(layer.descriptionKey)}</span>
                </div>
                <span className="learn-graph-topic-count">{topics.length}</span>
                <i className={`pi ${isOpen ? 'pi-chevron-up' : 'pi-chevron-down'} text-color-secondary`} />
              </button>

              {isOpen && (
                <div className="learn-graph-layer-body">
                  <div className="grid">
                    {topics.map((topic) => renderTopic(topic))}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default LearnGraphPage;
