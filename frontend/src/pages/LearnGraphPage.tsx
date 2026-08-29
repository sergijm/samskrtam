import React, { useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { ProgressBar } from 'primereact/progressbar';
import { Card } from 'primereact/card';
import { useLearnGraph } from '../hooks/useLearnGraph';
import { useLocaleStore } from '../store/localeStore';
import ProgressSummaryPanel from '../components/dashboard/ProgressSummaryPanel';
import type { LearnGraphTopic, LearnLayerDto, TopicStatus, TopicTypeGroup } from '../types/learnGraph';

const TYPE_ICONS: Record<TopicTypeGroup, string> = {
  vocabulary: 'pi-book',
  declension: 'pi-tag',
  sandhi: 'pi-link',
  conjugation: 'pi-sort-alt',
  syntax: 'pi-sitemap',
  other: 'pi-circle',
};

const TYPE_FILTERS: Array<TopicTypeGroup | 'all'> = [
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

const TYPE_FILTER_STORAGE_KEY = 'learnGraph.typeFilter';
const EXPANDED_STORAGE_KEY = 'learnGraph.expanded';

function loadTypeFilter(): TopicTypeGroup | 'all' {
  try {
    const raw = localStorage.getItem(TYPE_FILTER_STORAGE_KEY);
    if (raw && (raw === 'all' || TYPE_FILTERS.includes(raw as TopicTypeGroup))) {
      return raw as TopicTypeGroup | 'all';
    }
  } catch {
    // ignore corrupted value
  }
  return 'all';
}

function saveTypeFilter(value: TopicTypeGroup | 'all') {
  try {
    localStorage.setItem(TYPE_FILTER_STORAGE_KEY, value);
  } catch {
    // ignore
  }
}

function loadExpanded(): Record<string, boolean> {
  const defaults: Record<string, boolean> = { L0: true, always: true };
  try {
    const raw = localStorage.getItem(EXPANDED_STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) as Record<string, boolean>;
      if (parsed && typeof parsed === 'object') {
        return { ...defaults, ...parsed };
      }
    }
  } catch {
    // ignore corrupted value, fall back to defaults
  }
  return defaults;
}

function saveExpanded(value: Record<string, boolean>) {
  try {
    localStorage.setItem(EXPANDED_STORAGE_KEY, JSON.stringify(value));
  } catch {
    // ignore
  }
}

/**
 * LearnGraphPage — карта обучения (dashboard). Данные из curriculum-service
 * GET /api/v2/curriculum/learn-graph: реальные темы, слои по LearningLevel,
 * прогресс пользователя пока генерируется на бэкенде.
 */
const LearnGraphPage: React.FC = () => {
  const { t } = useTranslation();
  const { locale } = useLocaleStore();
  const navigate = useNavigate();

  const { data, isLoading, isError, refetch } = useLearnGraph();

  const [expanded, setExpanded] = useState<Record<string, boolean>>(loadExpanded);
  const [typeFilter, setTypeFilter] = useState<TopicTypeGroup | 'all'>(loadTypeFilter);
  const [highlightCode, setHighlightCode] = useState<string | null>(null);

  const layers: LearnLayerDto[] = useMemo(() => data?.layers ?? [], [data]);

  const topicLayer = useMemo(() => {
    const index: Record<string, string> = {};
    for (const layer of layers) {
      for (const topic of layer.topics) {
        index[topic.code] = layer.id;
      }
    }
    return index;
  }, [layers]);

  const allTopics = useMemo(() => layers.flatMap((l) => l.topics), [layers]);

  const topicTitle = (topic: LearnGraphTopic) => (locale === 'en' ? topic.titleEn : topic.titleRu);
  const topicDesc = (topic: LearnGraphTopic) => (locale === 'en' ? topic.titleEn : topic.titleRu);

  const updateExpanded = (updater: (prev: Record<string, boolean>) => Record<string, boolean>) => {
    setExpanded((prev) => {
      const next = updater(prev);
      saveExpanded(next);
      return next;
    });
  };

  const toggle = (id: string) => updateExpanded((prev) => ({ ...prev, [id]: !prev[id] }));

  const expandLayer = (layerId?: string) => {
    if (!layerId) return;
    updateExpanded((prev) => ({ ...prev, [layerId]: true }));
  };

  const visibleTopics = (layer: LearnLayerDto) =>
    layer.topics.filter((topic) => typeFilter === 'all' || topic.typeGroup === typeFilter);

  const openTopic = (topic: LearnGraphTopic) => navigate(topic.route ?? '/grammar');

  const statusText = (status?: TopicStatus) =>
    status && status !== 'available' ? t(`learnGraph.statuses.${status}`) : null;

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
  const continueProgress = continueTopic?.status === 'mastered' ? 100 : (continueTopic?.progressPercent ?? 0);

  const renderTopic = (topic: LearnGraphTopic) => {
    const status = statusText(topic.status);
    const isInProgress = topic.status === 'in_progress';
    return (
      <div key={topic.code} className="col-12 md:col-6 xl:col-4">
        <div
          className={[
            'learn-graph-topic',
            highlightCode === topic.code ? ' learn-graph-topic--highlighted' : '',
            topic.status === 'recommended' ? ' learn-graph-topic--recommended' : '',
          ].join('')}
        >
          <div className="flex align-items-start justify-content-between gap-2">
            <div className="flex align-items-center gap-2">
              <i
                className={`learn-graph-topic-icon pi ${TYPE_ICONS[topic.typeGroup]}`}
                title={t(`learnGraph.typeFilters.${topic.typeGroup}`)}
              />
              <span className="font-medium">{topicTitle(topic)}</span>
            </div>
            {status && (
              <span className={`learn-graph-status learn-graph-status--${topic.status}`}>
                {STATUS_ICONS[topic.status!] && (
                  <i className={`pi ${STATUS_ICONS[topic.status!]} learn-graph-status-icon`} />
                )}
                {status}
              </span>
            )}
          </div>

          <div className="text-sm text-500 mt-1">{topicDesc(topic)}</div>

          {isInProgress && topic.progressPercent !== undefined && (
            <ProgressBar value={topic.progressPercent} className="learn-graph-topic-progress mt-2" />
          )}

          {topic.prerequisites && topic.prerequisites.length > 0 && (
            <div className="learn-graph-dep mt-2">
              <i className="pi pi-arrow-left learn-graph-dep-icon" />
              <span className="text-xs text-500">{t('learnGraph.dependsOn')}:</span>
              {topic.prerequisites.map((code) => {
                const prereqTopic = allTopics.find((tp) => tp.code === code);
                return (
                  <button
                    key={code}
                    type="button"
                    className="learn-graph-prereq"
                    onMouseEnter={() => setHighlightCode(code)}
                    onMouseLeave={() => setHighlightCode(null)}
                    onClick={() => expandLayer(topicLayer[code])}
                  >
                    {prereqTopic ? topicTitle(prereqTopic) : code}
                  </button>
                );
              })}
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

  const header = (
    <header className="mb-4">
      <div className="flex align-items-center gap-2 mb-1">
        <i className="pi pi-sitemap text-2xl text-primary" />
        <h1 className="m-0 text-2xl">{t('learnGraph.title')}</h1>
      </div>
      <p className="m-0 text-500">{t('learnGraph.subtitle')}</p>
    </header>
  );

  if (isLoading) {
    return (
      <div className="learn-graph-page p-3 md:p-4">
        {header}
        <Card className="mb-4 fade-in">
          <div className="grid">
            {Array.from({ length: 6 }).map((_, i) => (
              <div key={i} className="col-12 md:col-6 xl:col-4">
                <div className="learn-graph-topic">
                  <div className="skeleton w-full" style={{ height: '1.4rem' }} />
                  <div className="skeleton w-full mt-2" style={{ height: '1rem' }} />
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="learn-graph-page p-3 md:p-4">
        {header}
        <Card>
          <div className="text-center p-3 text-500">{t('learnGraph.subtitle')}
            <div className="mt-3">
              <button type="button" className="learn-graph-cta" onClick={() => refetch()}>
                <i className="pi pi-refresh mr-2" />
                {t('common.continue')}
              </button>
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="learn-graph-page p-3 md:p-4">
      {header}

      <div className="mb-4">
        <ProgressSummaryPanel scope="learn-graph" />
      </div>

      <Card className="learn-graph-summary mb-4 fade-in">
        <div className="flex flex-column gap-2">
          <div className="flex justify-content-between align-items-center">
            <span className="font-medium">{t('learnGraph.progressTitle')}</span>
            <span className="learn-graph-overall">{overallPercent}%</span>
          </div>
          <ProgressBar value={overallPercent} style={{ height: '0.75rem' }} />
          <div className="flex flex-wrap gap-3 text-sm text-500">
            <span>
              <b>{masteredCount}</b> {t('learnGraph.progressMastered')}
            </span>
            <span>
              <b>{inProgressCount}</b> {t('learnGraph.progressInProgress')}
            </span>
            <span>
              <b>{recommendedCount}</b> {t('learnGraph.progressAvailable')}
            </span>
          </div>
        </div>
      </Card>

      {continueTopic && (
        <Card className="learn-graph-continue mb-4 fade-in">
          <div className="flex flex-column md:flex-row align-items-center justify-content-between gap-3">
            <div className="flex flex-column gap-2 flex-1 w-full">
              <span className="text-sm text-500 font-medium">{t('learnGraph.continueTitle')}</span>
              <div className="flex align-items-center flex-wrap gap-2">
                <span className="font-bold text-lg">{topicTitle(continueTopic)}</span>
                <span className="text-sm text-500">{topicDesc(continueTopic)}</span>
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

      <div className="learn-graph-filter flex flex-wrap align-items-center gap-2 mb-3">
        <span className="text-sm font-medium text-color-secondary mr-1">{t('learnGraph.filterTitle')}</span>
        {TYPE_FILTERS.map((group) => (
          <button
            key={group}
            type="button"
            className={`learn-graph-filter-btn${typeFilter === group ? ' active' : ''}`}
            onClick={() => {
              setTypeFilter(group);
              saveTypeFilter(group);
            }}
          >
            {t(`learnGraph.typeFilters.${group}`)}
          </button>
        ))}
      </div>

      <div className="learn-graph">
        {layers.map((layer) => {
          const isOpen = !!expanded[layer.id];
          const topics = visibleTopics(layer);
          return (
            <div
              key={layer.id}
              className={`learn-graph-layer${layer.alwaysAvailable ? ' learn-graph-layer--always' : ''}`}
            >
              <button
                type="button"
                className="learn-graph-layer-header"
                onClick={() => toggle(layer.id)}
                aria-expanded={isOpen}
              >
                <div className="learn-graph-layer-number">
                  {layer.alwaysAvailable ? <i className="pi pi-infinity" /> : layer.id.replace('L', '')}
                </div>
                <div className="flex flex-column flex-1 text-left">
                  <span className="font-bold">{t(`learnGraph.layers.${layer.id.toLowerCase()}.title`)}</span>
                  <span className="text-sm text-500">
                    {t(`learnGraph.layers.${layer.id.toLowerCase()}.description`)}
                  </span>
                </div>
                <span className="learn-graph-topic-count">{topics.length}</span>
                <i className={`pi ${isOpen ? 'pi-chevron-up' : 'pi-chevron-down'} text-color-secondary`} />
              </button>

              {isOpen && (
                <div className="learn-graph-layer-body">
                  <div className="grid">{topics.map((topic) => renderTopic(topic))}</div>
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