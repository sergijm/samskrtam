import { useParams, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useChapterVerses, useAnalyzeAllVerses } from '../../hooks/useSangraha';
import { Skeleton } from 'primereact/skeleton';
import { IconButton, PageButton } from '../../components/common/buttons';
import { Tooltip } from 'primereact/tooltip';

const statusSeverity: Record<string, 'success' | 'info' | 'warn' | 'danger'> = {
  ANALYZED: 'success',
  ANALYZING: 'info',
  DRAFT: 'warn',
  FAILED: 'danger',
};

const statusIcon: Record<string, { icon: string; color: string }> = {
  ANALYZED:  { icon: 'pi pi-check-circle',   color: 'var(--green-500)' },
  ANALYZING: { icon: 'pi pi-spin pi-spinner', color: 'var(--blue-500)' },
  DRAFT:     { icon: 'pi pi-pencil',          color: 'var(--yellow-500)' },
  FAILED:    { icon: 'pi pi-exclamation-circle', color: 'var(--red-500)' },
};

const ChapterPage = () => {
  const { t, i18n } = useTranslation();
  const { workSlug, chapterId } = useParams<{ workSlug: string; chapterId: string }>();
  const navigate = useNavigate();
  const { data: chapter, isLoading, isError } = useChapterVerses(chapterId || '');
  const analyzeAll = useAnalyzeAllVerses();

  const hasAnalyzableVerses = chapter?.verses?.some(
    (v) => v.status === 'DRAFT' || v.status === 'FAILED'
  );

  if (isLoading) {
    return (
      <div className="p-4">
        <Skeleton width="60%" height="2rem" className="mb-2" />
        <Skeleton width="100%" height="1.5rem" className="mb-1" />
        <Skeleton width="100%" height="1.5rem" className="mb-1" />
        <Skeleton width="100%" height="1.5rem" />
      </div>
    );
  }

  if (isError || !chapter) {
    return (
      <div className="p-4 text-center">
        <i className="pi pi-exclamation-triangle text-4xl text-red-500 mb-3" />
        <h3>{t('common.error')}</h3>
        <p>{t('sangraha.chapterNotFound')}</p>
        <IconButton iconName="pi-arrow-left" className="p-button-rounded" onClick={() => navigate(`/sangraha/${workSlug}`)} />
      </div>
    );
  }

  const translationFor = (translationRu?: string | null, translationEn?: string | null) =>
    i18n.language === 'ru' ? translationRu : translationEn;

  return (
    <div className="p-4">
      <Tooltip />

      <div className="flex align-items-center mb-3">
        <IconButton
          iconName="pi-arrow-left"
          className="p-button-rounded mr-2"
          onClick={() => navigate(`/sangraha/${workSlug}`)}
        />
        <div style={{ flex: 1 }}>
          <h2 className="m-0">
            {chapter.titleIast || chapter.titleEn}
            {chapter.titleDevanagari ? ` (${chapter.titleDevanagari})` : ''}
          </h2>
          <p className="text-color-secondary text-sm m-0">
            {i18n.language === 'ru' ? chapter.titleRu : chapter.titleEn}
          </p>
        </div>
        <PageButton
          variant="cta-primary"
          iconName="pi-sync"
          labelKey="sangraha.action.analyzeAll"
          loading={analyzeAll.isPending}
          disabled={!hasAnalyzableVerses || analyzeAll.isPending}
          onClick={() => analyzeAll.mutate(chapterId!)}
        />
      </div>

      {chapter.verses && chapter.verses.length > 0 ? (
        <div className="work-tree">
          {chapter.verses.map((v) => (
            <div
              key={v.id}
              className="work-tree-row cursor-pointer hover:surface-hover"
              onClick={() => navigate(`/sangraha/${workSlug}/verses/${v.id}`)}
            >
              <div className="work-tree-row-left">
                <span className="font-bold text-sm" style={{ minWidth: '2rem' }}>{v.orderIndex}</span>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', flex: 1 }}>
                  <span className="font-medium">
                      {v.textIast || `${t('sangraha.verse')} ${v.orderIndex}`}
                    </span>
                  {translationFor(v.translationRu, v.translationEn) && (
                    <span className="text-xs text-color-secondary font-italic">
                      {translationFor(v.translationRu, v.translationEn)}
                    </span>
                  )}
                </div>
            </div>
              <div className="work-tree-row-right">
                <i
                  className={statusIcon[v.status]?.icon ?? 'pi pi-question-circle'}
                  style={{ color: statusIcon[v.status]?.color ?? 'var(--text-color-secondary)' }}
                  data-pr-tooltip={t(`sangraha.status.${v.status}`)}
                  data-pr-position="top"
                />
                <i className="pi pi-chevron-right text-color-secondary ml-2" />
        </div>
            </div>
          ))}
    </div>
      ) : (
        <div className="text-center text-color-secondary p-4">
          {t('sangraha.noVerses')}
        </div>
      )}
    </div>
  );
};

export default ChapterPage;

