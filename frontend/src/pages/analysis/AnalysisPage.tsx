import { useTranslation } from 'react-i18next';
import { useCallback, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import {
  useStandaloneVerses,
  useCreateStandaloneAnalysis,
  useDeleteStandaloneVerse,
  useVerseDetail,
} from '../../hooks/useSangraha';
import { sangrahaApi } from '../../api/sangraha';
import { VerseStatus, StandaloneVerseItemDto } from '../../types/sangraha';
import { verseStatusIcon } from '../../utils/verseStatus';
import VerseWordsList from '../../components/sangraha/VerseWordsList';
import SandhiSplitsList from '../../components/sangraha/SandhiSplitsList';
import { IconButton, CtaButton } from '../../components/common/buttons';
import { Tag } from 'primereact/tag';
import { Toast } from 'primereact/toast';
import { Tooltip } from 'primereact/tooltip';
import { Skeleton } from 'primereact/skeleton';
import { InputTextarea } from 'primereact/inputtextarea';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';

/**
 * Страница /analysis — разбор произвольного предложения (standalone-стих,
 * verse.chapter_id = null, не привязан к произведению/главе).
 * Функционал детального просмотра аналогичен VersePage: ввод текста,
 * LLM-анализ, перевод, сандхи, пословный разбор и кнопка «Изучить».
 * Стихи персональные — каждый пользователь видит и удаляет только свои.
 */
const AnalysisPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const toast = useRef<Toast>(null);
  const queryClient = useQueryClient();

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [newText, setNewText] = useState('');

  const { data: items, isLoading, isError } = useStandaloneVerses();
  const create = useCreateStandaloneAnalysis();
  const remove = useDeleteStandaloneVerse();

  // Детальный просмотр выбранного standalone-стиха
  const { data: verse, isLoading: verseLoading } = useVerseDetail(selectedId || '');

  const [editText, setEditText] = useState('');
  const [analyzePending, setAnalyzePending] = useState(false);
  const [analyzeMorphologyPending, setAnalyzeMorphologyPending] = useState(false);
  const [isEditing, setIsEditing] = useState(false);

  // При выборе нового стиха сбрасываем поле редактирования текста
  const selectVerse = useCallback(
    (id: string) => {
      setSelectedId(id);
      setEditText('');
    },
    [],
  );

  const formatDate = (value?: string) =>
    value ? new Date(value).toLocaleString(i18n.language === 'ru' ? 'ru-RU' : 'en-GB') : '-';

  // ── Создание нового standalone-стиха + анализ ──
  const handleCreate = useCallback(async () => {
    if (!newText.trim()) {
      toast.current?.show({ severity: 'warn', summary: t('common.error') });
      return;
    }
    try {
      const res = await create.mutateAsync(newText.trim());
      setNewText('');
      selectVerse(res.data.id);
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [newText, create, selectVerse, t]);

  // ── Повторный анализ (DRAFT/FAILED/повторное редактирование) ──
  const handleAnalyze = useCallback(async () => {
    if (!selectedId) return;
    setAnalyzePending(true);
    try {
      await sangrahaApi.analyzeVerse(selectedId, { text: editText });
      queryClient.invalidateQueries({ queryKey: ['sangraha', 'verse', selectedId] });
      queryClient.invalidateQueries({ queryKey: ['sangraha', 'analysis', 'list'] });
      toast.current?.show({ severity: 'success', summary: t('sangraha.action.analyze') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    } finally {
      setAnalyzePending(false);
      setIsEditing(false);
    }
  }, [selectedId, editText, queryClient, t]);

  // ── «Анализировать морфологию» — шаг 2: пословный разбор слов (ADMIN) ──
  const handleAnalyzeMorphology = useCallback(async () => {
    if (!selectedId) return;
    setAnalyzeMorphologyPending(true);
    try {
      await sangrahaApi.analyzeVerseInternalSandhi(selectedId);
      queryClient.invalidateQueries({ queryKey: ['sangraha', 'verse', selectedId] });
      toast.current?.show({ severity: 'success', summary: t('sangraha.action.analyzeMorphology') });
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    } finally {
      setAnalyzeMorphologyPending(false);
    }
  }, [selectedId, queryClient, t]);

  // ── «Изучить» — экспорт пачки лемм стиха и переход на урок ──
  const handleStudy = useCallback(async () => {
    if (!selectedId) return;
    try {
      const res = await sangrahaApi.studyVerse(selectedId);
      const { verseTopicCode: code } = res.data;
      navigate(`/lessons/vocabulary/${code}`);
    } catch {
      toast.current?.show({ severity: 'error', summary: t('common.error') });
    }
  }, [selectedId, navigate, t]);

  const handleDelete = useCallback(
    async (item: StandaloneVerseItemDto) => {
      if (!window.confirm(t('sangraha.analysis.deleteConfirm'))) return;
      try {
        await remove.mutateAsync(item.id);
        if (selectedId === item.id) {
          setSelectedId(null);
        }
      } catch {
        toast.current?.show({ severity: 'error', summary: t('common.error') });
      }
    },
    [remove, selectedId, t],
  );

  // ── Статусы детального просмотра ──
  const status = verse?.status;
  const isAnalyzing = status === 'ANALYZING';
  const isDraftOrFailed = status === 'DRAFT' || status === 'FAILED';
  const isAnalyzed = status === 'ANALYZED';

  /** Severity для PrimeReact Tag (в отличие от verseStatusSeverity — `warning` вместо `warn`). */
  const tagSeverityFor = (s: VerseStatus | undefined): 'success' | 'info' | 'warning' | 'danger' =>
    s === 'ANALYZED' ? 'success' : s === 'FAILED' ? 'danger' : s === 'ANALYZING' ? 'info' : 'warning';

  const studyIcon = useMemo(() => 'pi-book', []);

  const rows = items ?? [];
  const selectedCreatedAt = rows.find((i) => i.id === selectedId)?.createdAt;

  return (
    <div className="p-4">
      <Toast ref={toast} />
      <Tooltip />

      {/* ── Детальный просмотр выбранного разбора ── */}
      {selectedId ? (
        <>
          <div className="flex align-items-center mb-3">
            <IconButton
              iconName="pi-arrow-left"
              className="p-button-rounded mr-2"
              onClick={() => setSelectedId(null)}
            />
            <h2 className="m-0">
              {t('sangraha.analysis.entry')}
              {selectedCreatedAt ? ` — ${formatDate(selectedCreatedAt)}` : ''}
            </h2>
            {verse && (
              <Tag
                value={t(`sangraha.status.${verse.status}`)}
                severity={tagSeverityFor(verse.status)}
                className="ml-2"
              />
            )}
            {isAnalyzed && (
              <div className="flex align-items-center gap-2 ml-auto">
                <CtaButton
                  labelKey="sangraha.action.analyze"
                  iconName="pi-robot"
                  className="p-button-text"
                  onClick={handleAnalyze}
                  loading={analyzePending}
                />
                <CtaButton
                  labelKey="common.edit"
                  iconName="pi-pencil"
                  className="p-button-text"
                  onClick={() => {
                    setEditText(verse?.rawText ?? verse?.textDevanagari ?? verse?.textIast ?? '');
                    setIsEditing(true);
                  }}
                />
              </div>
            )}
          </div>

          {verseLoading ? (
            <div className="p-4">
              <Skeleton width="100%" height="30px" className="mb-3" />
              <Skeleton width="100%" height="200px" className="mb-3" />
              <Skeleton width="100%" height="300px" />
            </div>
          ) : !verse ? (
            <div className="p-4 text-center text-color-secondary">{t('common.error')}</div>
          ) : (
            <>
              {isAnalyzing && (
                <div className="mb-4">
                  <Skeleton width="100%" height="50px" />
                  <p className="mt-2 text-color-secondary">{t('sangraha.status.ANALYZING')}</p>
                </div>
              )}

              {/* DRAFT/FAILED or editing: input + Analyze button */}
              {(isDraftOrFailed || isEditing) && !isAnalyzing && (
                <div className="mb-4">
                  <div className="mb-3">
                    <label className="block mb-1 font-semibold">{t('sangraha.fields.text')}</label>
                    <InputTextarea
                      value={editText || verse.rawText || ''}
                      onChange={(e) => setEditText(e.target.value)}
                      className="w-full"
                      rows={4}
                      placeholder={t('sangraha.placeholder.text')}
                    />
                  </div>
                  <div className="flex align-items-center gap-2">
                    <CtaButton
                      labelKey="sangraha.action.analyze"
                      iconName="pi-robot"
                      className="p-button-success"
                      onClick={handleAnalyze}
                      loading={analyzePending}
                    />
                    {isEditing && (
                      <CtaButton
                        labelKey="common.cancel"
                        iconName="pi-times"
                        className="p-button-text"
                        onClick={() => setIsEditing(false)}
                      />
                    )}
                  </div>
                </div>
              )}

              {/* ANALYZED: read-only просмотр */}
              {isAnalyzed && (
                <>
                  <div className="mb-4">
                    <div className="mb-3">
                      <label className="block mb-1 font-semibold">{t('sangraha.fields.textDevanagari')}</label>
                      <div className="p-3 border-1 border-round surface-border surface-ground">
                        <p className="m-0 text-lg">{verse.textDevanagari || '-'}</p>
                      </div>
                    </div>
                    <div className="mb-3">
                      <label className="block mb-1 font-semibold">{t('sangraha.fields.textIast')}</label>
                      <div className="p-3 border-1 border-round surface-border surface-ground">
                        <p className="m-0 text-lg">{verse.textIast || '-'}</p>
                      </div>
                    </div>
                  </div>

                  {verse.analysis && (
                    <div className="mb-4">
                      <div className="mb-3">
                        <label className="block mb-1 font-semibold">{t('sangraha.fields.translation')}</label>
                        <div className="p-3 border-1 border-round surface-border surface-ground">
                          <p className="m-0">
                            {(i18n.language === 'ru'
                              ? verse.analysis.translationRu
                              : verse.analysis.translationEn) || '-'}
                          </p>
                        </div>
                      </div>
                      <SandhiSplitsList sandhiSplits={verse.analysis.sandhiSplits} />
                    </div>
                  )}

                  {verse.words && verse.words.length > 0 && (
<VerseWordsList
                        words={verse.words}
                        headerActions={
                        <>
                          <CtaButton
                            labelKey="sangraha.action.analyzeMorphology"
                            iconName="pi-sitemap"
                            className="p-button-text"
                            onClick={handleAnalyzeMorphology}
                            loading={analyzeMorphologyPending}
                          />
                          <CtaButton
                            labelKey="sangraha.action.study"
                            iconName={studyIcon}
                            className="p-button-text"
                            onClick={handleStudy}
                          />
                        </>
                      }
                    />
                  )}
                </>
              )}
            </>
          )}
        </>
      ) : (
        /* ── Список разборов + создание нового ── */
        <>
          <div className="flex align-items-center mb-3">
            <h2 className="m-0">{t('sangraha.analysis.title')}</h2>
          </div>

          {/* Создание нового разбора */}
          <div className="mb-4 p-3 border-1 border-round surface-border surface-ground">
            <label className="block mb-1 font-semibold">{t('sangraha.analysis.newEntry')}</label>
            <InputTextarea
              value={newText}
              onChange={(e) => setNewText(e.target.value)}
              className="w-full mb-2"
              rows={3}
              placeholder={t('sangraha.placeholder.text')}
            />
            <div className="flex align-items-center justify-content-between">
              <CtaButton
                labelKey="sangraha.action.analyze"
                iconName="pi-robot"
                className="p-button-success"
                onClick={handleCreate}
                loading={create.isPending}
              />
              <span className="text-sm text-color-secondary">{t('sangraha.analysis.hint')}</span>
            </div>
          </div>

          {/* Список разборов пользователя */}
          <h3 className="mb-2">{t('sangraha.analysis.myEntries')}</h3>
          {isLoading ? (
            <div>
              <Skeleton width="100%" height="2.5rem" className="mb-1" />
              <Skeleton width="100%" height="2.5rem" className="mb-1" />
              <Skeleton width="100%" height="2.5rem" className="mb-1" />
              <Skeleton width="100%" height="2.5rem" />
            </div>
          ) : isError ? (
            <div className="p-4 text-center text-color-secondary">{t('common.error')}</div>
          ) : rows.length === 0 ? (
            <div className="p-4 text-center text-color-secondary">{t('sangraha.analysis.noEntries')}</div>
          ) : (
            <DataTable
              value={rows}
              responsiveLayout="scroll"
              selectionMode="single"
              className="cursor-pointer"
              onRowClick={(e) => selectVerse(e.data.id)}
              emptyMessage={t('sangraha.analysis.noEntries')}
            >
              <Column
                field="preview"
                header={t('sangraha.fields.text')}
                body={(row: StandaloneVerseItemDto) => (
                  <span className="font-italic">{row.preview || '-'}</span>
                )}
              />
              <Column
                header={t('sangraha.fields.status')}
                body={(row: StandaloneVerseItemDto) => (
                  <>
                    <i
                      className={verseStatusIcon[row.status]?.icon ?? 'pi pi-question-circle'}
                      style={{ color: verseStatusIcon[row.status]?.color ?? 'var(--text-color-secondary)' }}
                      data-pr-tooltip={t(`sangraha.status.${row.status}`)}
                      data-pr-position="top"
                    />
                    <span className="ml-2">{t(`sangraha.status.${row.status}`)}</span>
                  </>
                )}
                style={{ width: '12rem' }}
              />
              <Column
                field="createdAt"
                header={t('sangraha.analysis.createdAt')}
                body={(row: StandaloneVerseItemDto) => formatDate(row.createdAt)}
                style={{ width: '14rem' }}
              />
              <Column
                header=""
                body={(row: StandaloneVerseItemDto) => (
                  <IconButton
                    iconName="pi-trash"
                    className="p-button-rounded p-button-text p-button-danger"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleDelete(row);
                    }}
                  />
                )}
                style={{ width: '4rem' }}
              />
            </DataTable>
          )}
        </>
      )}
    </div>
  );
};

export default AnalysisPage;
