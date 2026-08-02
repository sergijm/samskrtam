import { useSearchParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { DataTable } from 'primereact/datatable';
import { Column } from 'primereact/column';
import { Skeleton } from 'primereact/skeleton';
import { Tooltip } from 'primereact/tooltip';
import { useVersesBatch, useAnalyzeVerses } from '../../hooks/useSangraha';
import { verseStatusIcon } from '../../utils/verseStatus';
import { IconButton, PageButton } from '../../components/common/buttons';
import type { VerseBatchItemDto } from '../../types/sangraha';

/**
 * Массовый просмотр/анализ стихов по произвольному списку id
 * (sangraha-service/batch-verse-review.md, роут /sangraha/verses, ADMIN-only).
 */
const VersesBatchPage = () => {
  const { t, i18n } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const ids = searchParams.getAll('id');
  const { data, isLoading, isError } = useVersesBatch(ids);
  const analyze = useAnalyzeVerses();

  const titleFor = (item: VerseBatchItemDto) =>
    i18n.language === 'ru' ? item.workTitleRu : item.workTitleEn;

  const chapterTitleFor = (item: VerseBatchItemDto) =>
    i18n.language === 'ru' ? item.chapterTitleRu : item.chapterTitleEn;

  const verses = data?.verses ?? [];

  const statusBody = (row: VerseBatchItemDto) => (
    <>
      <i
        className={verseStatusIcon[row.status]?.icon ?? 'pi pi-question-circle'}
        style={{ color: verseStatusIcon[row.status]?.color ?? 'var(--text-color-secondary)' }}
        data-pr-tooltip={t(`sangraha.status.${row.status}`)}
        data-pr-position="top"
      />
      <span className="ml-2">{t(`sangraha.status.${row.status}`)}</span>
    </>
  );

  return (
    <div className="p-4">
      <Tooltip />

      <div className="flex align-items-center mb-3">
        <IconButton
          iconName="pi-arrow-left"
          className="p-button-rounded mr-2"
          onClick={() => navigate('/sangraha')}
        />
        <div style={{ flex: 1 }}>
          <h2 className="m-0">{t('sangraha.batchReviewTitle')}</h2>
        </div>
        {/* Кнопка активна всегда — повторный анализ ANALYZED осмыслен (batch-verse-review.md). */}
        <PageButton
          variant="cta-primary"
          iconName="pi-sync"
          labelKey="sangraha.action.analyzeAll"
          loading={analyze.isPending}
          disabled={ids.length === 0 || analyze.isPending}
          onClick={() => analyze.mutate(ids)}
        />
      </div>

      {isLoading ? (
        <div>
          <Skeleton width="100%" height="2.5rem" className="mb-1" />
          <Skeleton width="100%" height="2.5rem" className="mb-1" />
          <Skeleton width="100%" height="2.5rem" className="mb-1" />
          <Skeleton width="100%" height="2.5rem" />
        </div>
      ) : isError || !data ? (
        <div className="p-4 text-center text-color-secondary">{t('common.error')}</div>
      ) : verses.length === 0 ? (
        <div className="p-4 text-center text-color-secondary">{t('sangraha.noVerses')}</div>
      ) : (
        <DataTable
          value={verses}
          responsiveLayout="scroll"
          selectionMode="single"
          className="cursor-pointer"
          onRowClick={(e) => navigate(`/sangraha/${e.data.workSlug}/verses/${e.data.id}`)}
          emptyMessage={t('sangraha.noVerses')}
        >
          <Column
            field="workTitle"
            header={t('sangraha.work')}
            body={(row: VerseBatchItemDto) => titleFor(row)}
            style={{ width: '25%' }}
          />
          <Column
            field="chapterTitle"
            header={t('sangraha.chapter')}
            body={(row: VerseBatchItemDto) => chapterTitleFor(row)}
            style={{ width: '20%' }}
          />
          <Column
            field="verseOrderIndex"
            header={t('sangraha.fields.verseOrderIndex')}
            style={{ width: '6rem' }}
          />
          <Column field="textIastPreview" header={t('sangraha.fields.versePreview')} />
          <Column header={t('sangraha.fields.status')} body={statusBody} style={{ width: '12rem' }} />
        </DataTable>
      )}
    </div>
  );
};

export default VersesBatchPage;
